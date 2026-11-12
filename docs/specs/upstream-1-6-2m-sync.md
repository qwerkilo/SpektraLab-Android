# Upstream 1.6.2m sync

Status: `ready-for-agent`. Decision record: ADR-0010. Vocabulary:
`GLOSSARY.md`. Tracker: local tickets under
`.scratch/upstream-1.6.2m-sync/issues/`.

## Goal

Bring the upstream **1.6.2m (385)** native core and user-visible fixes into the
Material 3 Console without adopting the upstream UI. The fork keeps the Prism
Table shell, the M3 components, the brand shelf and its hardening rules.

## Non-goals

- Replacing the M3 Console with upstream `MainActivity`.
- A stable-channel release; stable stays at 156.
- Porting the feedback WeChat QR or upstream-only chrome.
- Engine work beyond what upstream already ships in `1.6.2m`.

## Workstreams

### A — native and JNI

Take the upstream `spektralab_jni.cpp`, `CMakeLists.txt` and the
`engine/SpektraLabEngine.java` contract changes, keeping the fork's package
names, manifest hardening and JNI entry points. The new native pieces include
the geometry band window, export-result race fix and the rebate/export metadata
changes.

Acceptance: a signed 16 KB APK builds with the donor APK; a JPEG import renders;
a full-size export writes a file.

### B — rebate legibility

Port the 1.6.2m rebate rule: keep the marks, thicken the stroke when the canvas
long edge is under 3500 px, and only drop the type below 900 px. The test uses
`max(width, height)`, so a portrait 645 frame receives the wordmark.

Acceptance: a 3330x4456 portrait frame shows the rebate wordmark in the preview;
a 5973x3222 landscape keeps the same full-size export.

### C — per-frame Original

Add `Frame.original` and use it when reopening a chip: a Frame left on 原图 comes
back on 原图, not developed; a legacy frame without the flag opens on its print.
The session document stores the selected page, not a global Original state.

Acceptance: switch a Frame to 原图, open another Frame, return; the first is
still 原图. A pre-flag session opens developed.

### D — render ownership

Add `renderEpoch` and `clearBusyPlate`: bump the epoch when the canvas is
replaced; a render that returns for an ended epoch must not paint and must
release the RENDERING state. The frame strip and import path share the counter.

Acceptance: switch photos during a render; the new frame never shows RENDERING
after it finishes, and no stale bitmap paints.

### E — geometry snapshot

Capture quarter turns, fine rotation, crop and flips at commit time; the render
thread sends `geoSnap*`, not the live fields. A failed render restores the
committed rotation. `lastTapMs` is cleared on UP.

Acceptance: commit -8.64°, let the render run; the engine receives -8.64°, not 0;
undo and frame switching restore the same values.

### F — panel gate

A panel that acts on a render is gated on a live session/preview: the optics
slider, print row, scale cards, rebate and date pages grey out with a reason
until a render exists. `opticsPanel` is assigned; the lock deep-disables rather
than only dims.

Acceptance: on a fresh Frame, the optics panel is locked; after a film renders it
unlocks; returning to 原图 locks it again.

### G — composition and chip fixes

Port the 14 fixes that touch behaviour: `cropToRatio` re-cuts from the whole
frame; gate cards map 1:1 to `EDGE_IDS`; a rejected gate says why; the geometry
band is windowed; 16:9 gate deadlock; batch index crash; Ilford EDR; export
`g_result` race; curve axis labels; histogram backdrop and live bins.

Acceptance: crop 1:1 on a wide frame cuts the full frame; every gate card opens
the gate of the same index; a rejected 16:9 pick shows a reason; batch export
does not crash on the last frame.

### H — export self-check

Port `exportMatchesCanvas`: compare the written file with the canvas using a
48-column grayscale NCC. A mismatch logs `export-corrupt`, is not announced as
success, and triggers one automatic re-export. The old X/Y contrast test is
removed.

Acceptance: a normal export reports success; a corrupted file (test seam) is
not announced, and exactly one retry runs.

## Test seams

- Native/JNI: `align16k` build + `check-apk.py`; device import/render/export.
- Pure JVM: `FrameOriginal` (flag round-trip), `RenderEpoch` (stale callback),
  `GeometrySnapshot` (capture/restore), `ExportMatch` (NCC thresholds).
- Robolectric: per-frame Original chip state, panel lock visibility, export
  sheet rows.
- Device: portrait rebate, frame switch during render, crop 1:1, batch export.

## Acceptance

- Preview channel is `385 / 1.6.2m-m3`; `update.json` stays on 156.
- All four gates green: `checkstyleGovernance`, `checkJvm`, `lintDebug`,
  `testDebugUnitTest`; `align16k` + `check-apk.py`.
- The M3 Console, brand shelf, scan inspect, session restore and transactional
  export keep working on the device.

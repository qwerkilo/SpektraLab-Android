# Upstream 1.6.0e UI port

Status: `in progress` (phase 1). Tracker: local tickets under
`.scratch/upstream-1.6-ui/issues/`. Decision record: ADR-0007. Vocabulary:
`GLOSSARY.md`.

## Goal

Expose the 1.6.0e UI capabilities inside the Material 3 Console without
replacing the Console's Frame/Look/undo model. The native contract is already
merged (ADR-0007); this spec covers the app layer.

## Non-goals

- Replacing the M3 Console with upstream's `MainActivity`.
- New engine work or a stable-channel release.
- Presets or batch migration for existing Frames beyond what the user asks.

## Phases

### Phase 1 — live preview and canvas

While a slider is moving, render one small frame (`LIVE_LONG_EDGE` 800) through
`renderBegin("live", true)` so the engine can reprint from the cached negative.
Requests coalesce, at most one frame per 90 ms. A full render, an import or a
Frame change invalidates every live frame in flight. Live frames never record
undo, persist, or change the busy state.

Seam: `app.spektralab.core.LivePreviewGate` (pure JVM) plus
`LivePreviewGateCheck`. Activity wiring may be covered by Robolectric later.

### Phase 2 — batch import and export

Multi-select import registers each URI as a Frame without decoding it; opening
a chip uses the normal import path. Batch export reopens each Frame and runs
the existing `RenderSnapshot`/export path sequentially, with Back stopping
after the current file. No new engine entry points.

### Phase 3 — presets and film cards

Save/apply a compact Look JSON, share/import it through the clipboard, and show
a film card's cover. Presets must be optional and must not change the Look of a
Frame until Apply.

Presets are implemented. Film covers are `03b` and blocked: the repository has
no cover art and the catalogue has no cover field, so the card keeps its text
form until art exists.

### Phase 4 — colour, metering, histogram

Seven metering modes are implemented: the choice lives in the Frame's Look and
is written as `auto_exposure` / `auto_exposure_method`. The histogram overlay is
implemented too: the spectrum readout cell toggles it and `core/Histogram` bins
the displayed bitmap without a render. Hue correction is implemented as the
view sheet's `胶片原色` toggle; `core/HueCorrection` pins the seven untrusted
stocks and writes `hue_correction` into the delta. The preview gamut is
implemented: the view sheet chooses sRGB or Display P3, preview bitmaps are
tagged on API 29+, and export forces sRGB through `snapshotFor(..., true)`.

## Test seams

- `checkJvm`: `LivePreviewGateCheck`, `LookCheck`, `RenderSnapshotCheck`, and
  new pure preset/metering checks.
- `testDebugUnitTest`: panel rows and theme/frame state, as they exist today.
- Device: one slider drag shows frames without a full render each move; release
  lands the 2048 px preview; a full export is not overwritten by a live frame.

## Acceptance

- No render on a theme rebuild, tab change, Scan toggle or Compare gesture.
- One full render after a slider release; live frames are dropped once the full
  render starts.
- Phase 2–4 keep the M3 layout and the per-Frame Look/undo behavior.

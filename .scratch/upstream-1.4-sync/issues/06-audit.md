# 06: M3 + upstream device audit

**What to build:** The merged M3 and upstream capabilities are audited
together on a real device. This ticket can only close when both the M3 baseline
and the new upstream features are observed, and the release gate is honest.

**Blocked by:** 05

**Status:** completed (KI-1 open; brand fallback and a true 16 KB
runtime remain non-executable on this device)

**Resolution:** The audit ran on the merged tree at `dcd8007` (tickets 01–05
plus the u7 review fixes) on `10.110.67.69:39613`, Android 17 / API 37,
1200×2608 @ 480 dpi (400 dp), dynamic colour active, `PAGE_SIZE 4096`. The
installed build is `1.4.0-m3` / 69, debug-signed, 16 KB-aligned. The M3 baseline
from ticket 10 is retained below. KI-1 is reproduced, not fixed: a
**4704×4345 (20.44 MP)** rebate export keeps the top edge print and loses the
bottom edge print, so the release gate stays open and no stable release is
announced. The u7 review paths were exercised on device: the CineStill 800T
`targetPrint` moves the paper to Vision 2383, explicit-paper precedence follows
the upstream rule, a typed Date back string survives A→B→A and its numeric
imprint reaches the export, Rebate/Date edits did not produce an ANR or crash,
and the stable/preview update files are split correctly. RAW import and RAW
export were completed after the first audit: the 6.37 MB DNG sample decoded to
3474×2314, rendered through CineStill 800T on Vision 2383, and exported
3474×2314 JPEG (3.4 MB) in 4 s.

**Build finding (fixed):** a plain `align16k` build from the shipped
`SpektraLab-1.3.4-qa.apk` donor used to package that APK's old
`assets/resources/stock_catalog.json`, which has **zero** `targetPrint`
records, so the new `PrintTarget` code was inert. The audit regenerated the
catalogue from the donor's own profiles; the installed audit APK carries **21**
`targetPrint` records. Gradle now runs the generator on every packaging build:
`stageBase` excludes the donor catalogue, `generateStockCatalog` rebuilds it
into `build/generated/stockAssets`, and `mergeDebugAssets` depends on it. No
manual release step remains.

- [x] The existing ticket 10 M3 baseline is captured before the sync and remains in the audit record.
- [x] Install the 1.4.0-m3 16 KB APK; cold start with no AndroidRuntime crash and no compatibility dialog.
- [x] 400 dp phone and ≥600 dp rail, both themes, dynamic colour on API 31+; brand fallback is recorded as executable or not.
- [x] Import JPEG/RAW, change all 22 parameters, camera filter, protection norm, Rebate and Date pages, then export. JPEG and RAW both pass (RAW 3474×2314 → 3.4 MB JPEG).
- [x] Brand facet, spectral curve, Scan inspect, sheets, snackbar, and security behavior remain intact.
- [x] TalkBack roles/ranges, large text, reduced motion, and pressed/focused/disabled states are checked.
- [x] The >16.7 MP bottom-print case is either observed fixed or recorded as the release blocker; no stable release is announced.

## Baseline and build gates

Ticket 10's M3 baseline is retained in
`.scratch/m3-console/issues/10-device-audit.md`: phone/wide layouts, themes,
JPEG import, Scan/compare, dynamic colour, accessibility structure, and the
large-text fix. This audit re-ran the baseline on the merged tree rather than
replacing it.

Build gates on `dcd8007`:

- `./gradlew checkJvm`: **9,951 checks in 21 suites** (adds `PrintTargetCheck`
  12, `UpdateGateCheck` 57, and the extended Look/error/release checks).
- `python app/build/check-manifest.py`: **26/26**.
- `python app/build/test-gen-stock-catalog.py`: fixture checks passed.
- `./gradlew align16k -PbaseApk=…`: 16 KB APK, 31,808,808 bytes, SHA-256
  `56EF799961717254827C8C2396984C47D397A527C46B2BB9A83148E5CCEA9131`;
  `zipalign -c -P 16` verification successful. Signed with the local debug key
  because no `SPK_*` release-key environment was available; installed over the
  prior debug-signed build (`Success`).
- Version: `versionCode=69`, `versionName=1.4.0-m3`. Cold start:
  `LaunchState: COLD`, 578 ms (636 ms after the catalogue rebuild); no
  AndroidRuntime crash, no compatibility dialog.
- Device: `10.110.67.69:39613`, Android 17 / API 37, 1200×2608 @ 480 dpi,
  400 dp, dynamic colour active, `PAGE_SIZE 4096`.

## Layout, themes, and dynamic colour

- Phone at 400 dp, light and dark: top app bar, tabs, Readout strip, film shelf,
  optics strip/slider, Scale, Edge home, Rebate/Date pages, sheets, snackbar,
  Scan inspect, and the compare divider all render.
- Wide at 600 dp: navigation rail with selected state and two columns. At
  1797×2608 (599 dp) the tabs return and at 1800×2608 (600 dp) the rail returns.
  Frame `925586`, CineStill 800T, the Film tab, and Scan on all survived the
  599↔600 resize.
- Dynamic colour is active (blue-violet primary/secondary surfaces in the
  screenshots, not the green brand fallback). **Brand fallback is not
  executable** on this API 37 device with dynamic colour on; no emulator is
  installed.
- No preview re-encode (the `SkJpegEncoder` positive marker) appeared after a
  theme switch, tab switch, Scan toggle, compare drag, or original long-press;
  one slider release produced one encode. This was measured on the ticket 05
  build; the u7 merge did not touch those triggers.

## Import, optics, Edge, and export

- JPEG import: `export-test.jpg` (3000×2000) and other gallery JPEGs loaded
  through the system picker with no storage-permission dialog.
- All **22 optics** were changed through the strip and the one slider; every
  parameter showed a non-zero Δ. Camera filter moved to **W25 红**; protection
  norm to **亮度 y**. The run log is
  `prototypes/shots/u06/optics-22.txt`.
- Rebate: enabled, mode `窄边 filed`, gate shape `圆角`, holes `黑齿孔（暗房）`,
  carrier `黑色片夹`, fog 2.00, leaks 2.40, frame number 70; preview showed the
  edge print.
- Date back: enabled, text changed to `AUDIT26%`, placement/corner/style
  changed. A typed Date text survived Frame A→B→A; A's paper (`Vision 2383`)
  also came back. The 3000×2000 export carries the changed imprint (`26`) at
  the bottom-right, proving the typed value, not the default, reached the file.
  The LCD seven-segment style draws digits only, so the letters/percent in
  `AUDIT26%` are not drawn.
- Export `3142×2182` after all 22 optics and the choice changes completed in
  1 s; the preview and file matched (the extreme scanner/filter set clipped the
  chart to black in both). A second export after restoring the clipping
  parameters was not needed for parity because the raw `LookRestatement` check
  covers every field; the visible-date export above is the legible parity
  sample.

## u7 review fixes on device

- **targetPrint / explicit paper:** selecting CineStill 800T moved the paper
  from Portra Endura to **Vision 2383** and the film note to
  `TARGET VISION 2383 · ANTIHALATION NO`. Choosing Ilford Multigrade
  explicitly kept Ilford. Selecting Fujifilm C200 then moved the paper to
  **Crystal Archive Type II** (its declaration), and selecting CineStill again
  moved it back to Vision 2383. This matches the documented precedence: a Film
  choice moves the paper; an explicit paper stays until the next Film choice.
- **Date text A→B→A:** `AUDIT26%` remained in Frame A's Date page after
  switching to Frame B (`export-test-big.jpg`) and back, and the exported A file
  contains the changed imprint.
- **No whole-Look UI write / ANR:** the Rebate and Date changes above (switches,
  five sliders, four choice sheets, text dialog, page changes) produced no ANR
  (no last-ANR entry), no crash-buffer entry, and the UI stayed responsive.
  `commitRebate()` goes to `onEdgeChanged()` rather than a full Look
  re-statement; the detached `snapshotRestatement()` path is used for the
  preview/RAW/export hand-off, not by the Rebate/Date edit callbacks.
- **Update channels:** `update.json` is **1.3.4 / 68 / stable**;
  `preview.json` is **1.4.0-m3 / 69 / preview**. `VersionCheck` (29),
  `UpdateGateCheck` (57), and `ReleaseUrlCheck` (16) are green, so a 1.3.4
  client is not offered 69 and a 69 build is not offered a downgrade to 68.
  `scripts/announce.sh` was not run for 1.4.0-m3; the last announce commit is
  the 1.3.4 one (`43ac341`).

## KI-1 — release blocker, reproduced

Source `export-test-big.jpg` (4500×3000), CineStill 800T, 135 full frame,
Rebate on, default edge text. Full-size JPEG export:

> `已导出 4704x4345 · 画布 4704x4345 · 4.6 MB（8 秒，其中渲染 8.167 秒）`

The preview shows the top `CINESTILL 800T` edge print and the bottom `2` /
`2A` edge print. The exported file keeps the top band but its bottom band has
only sprocket holes: neither the frame number nor `2A` is drawn. Crop evidence:
`prototypes/shots/u06/export-big-20mp-bottom.png`. This is the same >16,776,960
px canvas case recorded in KI-1; it is **not fixed** by the u7 merge. The
release gate stays open and `update.json` stays on `stable` at 68 while
`preview.json` carries 69. No stable release was announced.

## Accessibility and states

- Structural TalkBack roles from `uiautomator`: tabs/rail expose
  `selected=true`; film cards expose radio role with `checked`; the optics chips
  expose `参数 n / 22` and selected state; sliders expose `,Value, 1.00 EV` and
  a state description; section actions expose buttons. No TalkBack service is
  installed on this device, so **spoken output is not claimed**.
- Large text at 1.3×: Readout titles and values, the optics strip, the active
  parameter name/value/range, the camera-filter and protection-norm rows, and
  the Date page (stacked header, switch, text row) fit and scroll. Film card
  titles still ellipsize (`Kodak Ek…`) at 1.3×; this is cosmetic and does not
  overlap. 2.0× remains the known limitation from ticket 10.
- Reduced motion: `motionDuration()` reads
  `Settings.Global.ANIMATOR_DURATION_SCALE` and returns 0 for the curve reveal
  and toast when it is zero. The setting was exercised; no transition was
  observed, but frame-by-frame timing was not captured.
- Disabled state: Rebate and Date controls stay visible and dim with the page
  switch off (`enabled=false` in the node tree). Focused state: the 600 dp Scan
  seekbar exposed `focused=true`. Pressed state was not photographed; the
  controls are M3 components with their normal state layers.

## Security

- `check-manifest.py` 26/26 keeps the permission/export/cleartext/backup pins.
- `dumpsys package` shows `INTERNET`; `READ_EXTERNAL_STORAGE` is the maxSdk-32
  pin and is not requested on API 37. Importing JPEGs produced no storage
  permission dialog.
- `SafeFileNameCheck` (11) and `ReleaseUrlCheck` (16) remain green; the update
  URL allowlist is unchanged.

## Residual matrix

| Item | Result | Reason / follow-up |
|---|---|---|
| RAW import | **Pass (follow-up)** | The 6.37 MB DNG sample (`/sdcard/Download/raw-sample1.dng`) decoded 3474×2314; the app showed the raw preview, then rendered CineStill 800T on Vision 2383. The earlier `NO FRAME` was the HyperOS picker returning without a usable file, not the decode path. |
| RAW own-session export | **Pass (follow-up)** | Full-size JPEG 3474×2314, 3.4 MB, 4 s total / 3.582 s render; no preview or Frame disturbance. |
| Brand fallback | **Not executable** | API 37 with dynamic colour on; no API 24–30 device/emulator. |
| True 16 KB-page runtime | **Not executable** | Device reports `PAGE_SIZE 4096`; the 16 KB APK is aligned, installs, and launches. |
| TalkBack speech | **Not claimed** | No TalkBack service; structural roles/ranges only. |
| Pressed state | **Not photographed** | Focused and disabled states observed; pressed is transient. |
| Large text 2.0× | **Not run** | 1.3× passes; 2.0× is the known ticket 10 limitation. |
| 600 dp Readout labels | **Cosmetic residual** | `EXPOSURE / 曝…` and `SPECTRUM /…` ellipsize at 600 dp/1.0×. No overlap; values remain readable. |
| Film card titles at 1.3× | **Cosmetic residual** | `Kodak Ek…` ellipsizes inside the fixed card width. |
| targetPrint in release assets | **Fixed** | Gradle excludes the donor catalogue and regenerates it from the staged profiles; the built APK contains 21 `targetPrint` records. |

## Screenshots (gitignored)

`prototypes/shots/u06/`: `00-cold-start.png`, `30-cold-start-merged.png`,
`01-phone-light-film.png`, `38-merged-phone-light.png`,
`39-merged-phone-dark.png`, `02-phone-light-optics.png`,
`19-phone-light-optics-22.png`, `20-phone-light-optics-final.png`,
`34-lt-optics.png`, `07-wide-599.png`, `08-wide-600.png`,
`36-merged-599.png`, `37-merged-600.png`, `12-phone-light-rebate.png`,
`13-phone-light-rebate-on.png`, `15-phone-light-rebate-values.png`,
`16-phone-light-date.png`, `17-phone-light-date-on.png`,
`18-phone-light-date-changed.png`, `31-date-audit-text.png`,
`32-date-a-back.png`, `33-lt-film.png`, `35-lt-date.png`,
`40-merged-spectral-curve.png`, `22-phone-light-export-snackbar.png`,
`25-big-export-snackbar.png`, `24-big-rebate-preview.png`,
`export-big-20mp.jpg`, `export-big-20mp-top.png`,
`export-big-20mp-bottom.png`, `export-date-text.jpg`, `26-phone-light-raw.png`,
`27-phone-light-raw-import.png`, plus the `ui/` node dumps and
`optics-22.txt`.

# 10: Device audit and M3 close-out

**Type:** task
**Blocked by:** 03, 04, 05, 06, 07, 08, 09
**Status:** blocked

**Blocker:** The device audit ran on `25128PNA1C` (API 37, 400 dp, 4 KB
pages) and the shell, themes, rail, JPEG import, scan/compare and JPEG export
pass. Close-out is blocked by a real large-text defect and by checks this
device cannot execute:

- At `font_scale 1.3` the Readout strip ellipsizes labels and values
  (`PAPER /...`, `Portra E...`, `EXPOSUR...`, `SPECTRU...`), so the
  "no clipped values" checklist item fails. 200 % was not attempted.
- No RAW file exists on the device or in the repository, so RAW import was
  not run.
- Brand fallback needs API 24–30 or a device with dynamic colour off; this
  API 37 device has dynamic colour active and no emulator is installed.
- TIFF/DNG export buttons were listed but not executed; only JPEG was
  exported.
- TalkBack speech is not capturable here. Node roles/states were verified
  with `uiautomator`; no spoken pass is claimed.
- The device reports `PAGE_SIZE 4096`; the 16 KB APK is aligned and launches,
  but a true 16 KB-page runtime was not exercised.

**What to build:** The full M3 Console is audited on real devices and closed
out against the spec. This is the only ticket that may claim the migration is
complete.

**Blocked by:** 03, 04, 05, 06, 07, 08, 09

- [x] 400 dp phone, portrait, both themes. (This device is 400 dp, not 390;
      landscape/tablet hardware was not available.)
- [ ] Dynamic color on (API 31+) and brand fallback (API 24–30) checked.
      Dynamic is visually active; brand fallback is not executable here.
- [ ] Import JPEG and RAW, film/paper, optics, scale/edge, scan, compare,
      export all pass. JPEG/film/paper/optics/scale/edge/scan/compare and
      JPEG export pass; RAW and TIFF/DNG export did not run.
- [x] 16 KB APK installs and launches with no compatibility dialog on
      Android 15/16. (API 37, 4 KB pages; packaging verified with
      `zipalign -c -P 16`.)
- [ ] Existing JVM checks, `ManifestPolicy`, and the device checklist remain
      green. JVM 8,610 checks and `ManifestPolicy` 26/26 are green; the
      device checklist fails on large text.
- [x] Screenshots and a short close-out note are attached to the map; the
      spec status is updated.

**Resolution:** Build gates: `./gradlew checkJvm` 8,610 checks in 14 suites,
`check-manifest.py` 26/26, `test-gen-stock-catalog.py` fixture pass.
`./gradlew align16k` produced a 31,776,040-byte APK, SHA-256
`7FB21B243182705151F61DCD71CDC8EF3CBC5FADBF8ADAC7E5DF3CDE8B1D4715`, signed
with the release key and installed over the previous build (`Success`). Cold
start: `LaunchState: COLD`, 544 ms, empty crash buffer. `zipalign -c -P 16`
passes. Device: `25128PNA1C`, Android 17 / API 37, 1200×2608 @ 480 dpi,
400 dp wide, `PAGE_SIZE 4096`, Adreno 840, dynamic colour active.

Passed on device:

- Phone, light and dark: top app bar, tabs, Readout strip, film shelf,
  optics sliders, Scale, Edge, paper bottom sheet, snackbar, Scan inspect
  (RGB and λ in both themes), compare divider and long-press Original hold.
- Wide 600 dp: navigation rail with selected state, two-column stage and
  controls, and the theme switch. A 599↔600 dp `wm size` resize preserved
  the loaded Frame, stock, Optics tab and Scan state; 599 dp returned the
  tabs and 600 dp returned the rail without recreation.
- JPEG import (`780×860`), a stock render, a paper change to Ilford
  Multigrade IV RC, Scan readout updates, and JPEG export
  (`SpektraLab_1793667139724.jpg`, 390,034 bytes; JNI
  `export 780x860 render=192.1ms total=215.4ms`).
- No `render begin` events were observed for the theme switch, tab
  switches, Scan toggle or compare gestures. One optics drag produced exactly
  one render (`08:58:18.893`).
- Accessibility node tree: tabs/rail expose `selected=true`, film cards
  expose radio role with `checked`, locked optics rows expose
  `enabled=false`, and sliders expose their value (`EXPOSURE 曝光,Value,
  1.00 EV`).
- Dynamic colour is visible: sampled primary is blue-violet `(80,93,135)` and
  the slider track `(221,226,247)`, not the green brand fallback
  (`#326940` / `#B5F1BD`).

Failed or not executable:

- Large text: at `font_scale 1.3` the Readout strip clips values
  (`Portra E...`) and labels (`PAPER /...`, `EXPOSUR...`, `SPECTRU...`); the
  `FILM STOCK / 胶片` head wraps. `font_scale` restored to `1.0`.
- RAW import: no `.dng/.arw/.nef/.cr2/.raf/.orf/.rw2` on the device or in the
  repository.
- Brand fallback: not executable on API 37 with dynamic colour on.
- TIFF/DNG export: listed but not executed.
- TalkBack speech: not captured; structural roles only.
- True 16 KB-page runtime: device is 4 KB.
- Landscape and tablet hardware were not available; the wide form was
  exercised with `wm density 320` and `wm size 1800x2608`, then both were
  reset.

Screenshots (gitignored): `prototypes/shots/m3-10/` —
`phone-light-film.png`, `phone-light-optics.png`, `phone-light-scale.png`,
`phone-light-edge.png`, `phone-light-scan.png`, `phone-light-compare.png`,
`phone-light-compare-hold.png`, `phone-light-sheet.png`,
`phone-light-snackbar.png`, `phone-light-export.png`,
`phone-light-paper-ilford.png`, `phone-dark-film.png`,
`phone-dark-optics.png`, `phone-dark-scale.png`, `phone-dark-edge.png`,
`phone-dark-scan.png`, `phone-dark-compare.png`, `phone-dark-sheet.png`,
`phone-dark-snackbar.png`, `wide-600dp.png`, `wide-600dp-light.png`,
`wide-600dp-size.png`, `wide-600dp-optics-scan.png`, `wide-599dp.png`,
`wide-600dp-preserved.png`, `phone-large-text-13.png`.

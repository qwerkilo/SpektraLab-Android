# Manual device checks

The JVM checks cannot exercise Android permissions, the picker, or the native
renderer. Run this list on a device after `adb install -r`; record the device,
Android version, and Theme mode.

## Permissions and external entry

- [ ] Install fresh (`adb install` after `pm clear app.spektralab`) and open the app. No storage permission dialog appears; the picker still opens.
- [ ] On Android 13+, import a JPEG and a RAW from the system picker. No `READ_EXTERNAL_STORAGE` grant is requested.
- [ ] From a file manager, "open with" offers no SpektraLab image entry (the dead `VIEW` filter is gone); use the in-app picker.
- [ ] `adb shell dumpsys package app.spektralab` shows only `READ_EXTERNAL_STORAGE` (maxSdk 32) and `INTERNET`.
- [ ] `adb shell cmd netpolicy` is not needed; the app only reaches HTTPS endpoints and the loopback debug sink.

## Normal and boundary

- [ ] Phone at 360 dp and 390 dp: no horizontal page scroll; brand row, chips, curve, note, and paper row all visible.
- [ ] Tablet/foldable at 600 dp and 960×600: two columns; collapse/expand the panel; resize across 599↔600 with Frame, Look, tab, and Scan state intact.
- [ ] Slider ends: exposure 0.25 / 4, grain 0 / 2, halation 0 / 4, glare 0 / 12; Δ reads correctly and reset returns the default.
- [ ] Import a 1-pixel-wide image, a square image, and a 3:2 image; Scale offers only frame-shaped gates.
- [ ] Select an 81-sample stock: the curve reveals once, and a Look change or theme switch leaves it at the full plot without replaying.
- [ ] Enable the system's reduced-motion setting, then select a stock: the full curve appears at once with no plotter sweep.
- [ ] Light/dark toggle with a frame loaded: no white flash, no render, values survive.

## Material 3

- [ ] Android 12+ with dynamic colour on: the shell, readout strip, panel links, paper row, and compare divider follow the wallpaper scheme; the photographic stage and spectral R/G/B do not change.
- [ ] Android 11 or older (or dynamic colour off): the brand fallback roles show the same layout with no missing or unreadable control.
- [ ] TalkBack: tabs/rail announce selected state, film cards announce radio role and checked state, optics sliders announce their real range and value, and section actions announce buttons.
- [ ] Large text (system font size at 200 %): readout values, optics labels, section heads, and sheet rows stay readable; no clipped or overlapping controls.
- [ ] Pressed, focused, and disabled: tap and keyboard-focus a section link, film card, gate card, scan chip, and paper row; each shows an M3 state layer. Locked optics rows read disabled and expose that state.
- [ ] Install the `align16k` APK on an Android 15/16 device with 16 KB pages; it launches with no page-size warning and opens a RAW.

## Exception and data

- [ ] Import a truncated RAW; the user sees the E-11xx message and the app stays usable.
- [ ] Import an image with no spectral profile; the curve says `SPECTRUM UNAVAILABLE`, not a fake line.
- [ ] Select a stock with gaps; the curve breaks at gaps and the sample count matches the shipped data.
- [ ] Corrupt `update.json` on a scratch build; manual update check reports E-2002 and does not open a browser.
- [ ] Reversal stock (Provia 100F): preview and export both say direct scan; paper is not used.

## Upstream 1.4.0 export and release gate

- [ ] Change the Film stock, Print stock, camera filter, protection norm, all 22 optics, a Rebate field, and a Date back field; export a JPEG and confirm each changed value is in the file (compare with the preview, and check `export-params` in the debug sink).
- [ ] Choose CineStill 800T: the paper moves to Vision 2383 and the film note says `TARGET VISION 2383`. Choose another paper explicitly, then pick a different Film: the new Film's declaration moves the paper. If the paper does not move, the base APK's `stock_catalog.json` predates the generator — regenerate it before building.
- [ ] Set a Date back text, switch Frame A→B→A, and export A: the typed text is still in A's Date page and the exported file carries its imprint (the LCD seven-segment style draws digits only).
- [ ] Export a RAW, then switch Frame A→B→A: the preview keeps its picture and the exported file matches the Look it was started with.
- [ ] Export a TIFF after changing a Date back text with a quote and a line break; the engine must not see a malformed delta.
- [ ] Set a Gate/Scale whose exported canvas is over 16,776,960 px with the Rebate on; record whether the bottom edge print is present. While KI-1 is open, record it as the release blocker, not as a pass.
- [ ] Confirm `preview.json` reports 1.4.0-m3 / 69 on the `preview` channel, `update.json` still reports the last stable build, and a stable client is not offered the preview build.

## Regression

- [ ] Open Date Back with the switch on: 日期位置, 日期角位 and 日期样式 rows all exist; change each, then toggle the theme and confirm all three values survive.
- [ ] After any panel-builder refactor, open Film, Optics, Scale, Edge, Date Back and Compose and count the visible rows against the pre-refactor screen.
- [ ] Import a JPEG, enable Scan, switch Frame A→B→A; Scan fraction and visibility return.
- [ ] Drag Scan while the film panel is open: the curve's mark follows the sampled wavelength, not the drag's horizontal fraction; grayscale clears the mark; turn Scan off and the mark disappears.
- [ ] Filter to KODAK, switch Frame, and return; the filter stays KODAK.
- [ ] Tap 重置当前页 while filtered and scrolled; the filter returns to 全部 and the shelf starts at the left.
- [ ] Select a stock from another brand; it stays pinned first and remains selected.
- [ ] Export JPEG, TIFF, and DNG after a theme toggle; files complete and match the preview.

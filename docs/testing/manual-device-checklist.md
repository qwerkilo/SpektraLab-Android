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

## Exception and data

- [ ] Import a truncated RAW; the user sees the E-11xx message and the app stays usable.
- [ ] Import an image with no spectral profile; the curve says `SPECTRUM UNAVAILABLE`, not a fake line.
- [ ] Select a stock with gaps; the curve breaks at gaps and the sample count matches the shipped data.
- [ ] Corrupt `update.json` on a scratch build; manual update check reports E-2002 and does not open a browser.
- [ ] Reversal stock (Provia 100F): preview and export both say direct scan; paper is not used.

## Regression

- [ ] Import a JPEG, enable Scan, switch Frame A→B→A; Scan fraction and visibility return.
- [ ] Drag Scan while the film panel is open: the curve's mark follows the same 0–1 fraction; turn Scan off and the mark disappears.
- [ ] Filter to KODAK, switch Frame, and return; the filter stays KODAK.
- [ ] Tap 重置当前页 while filtered and scrolled; the filter returns to 全部 and the shelf starts at the left.
- [ ] Select a stock from another brand; it stays pinned first and remains selected.
- [ ] Export JPEG, TIFF, and DNG after a theme toggle; files complete and match the preview.

# 04: M3 Date back secondary page

**What to build:** The Date back card opens its own M3 secondary page with the
upstream date controls. The default text comes from the Frame's EXIF date, and
Back returns to Edge without losing the Look.

**Blocked by:** 01 (Core parameter, Look, JNI, and catalogue sync)

**Status:** in progress — code and pure checks pass; device smoke covered
opening the page, the EXIF default, the switch, and style/corner changes. The
full edit/disable/re-enable/export pass is deferred to ticket 06 because the
QA device kept restarting the app's task during the session.

- [x] Date back has an on/off switch; controls stay visible and are disabled/dimmed while off.
- [x] Text, left/top insets, size, exposure, corner, style, and capture-data line are present and write the full Look.
- [x] Default text follows the Frame's EXIF capture date, not the current clock.
- [x] The preview imprint and the export use the same text and layout values.
- [x] Back restores the Edge page; switching Frame restores that Frame's Date back state.
- [ ] Device check: edit, disable, re-enable, export; both themes and large text.
  Partial: Date page opened on `10.110.67.69:37839`; the default text showed
  `'26 10 31` from the imported file's EXIF; the switch enabled the controls;
  style (点阵 dots) and corner (左上) changed without a crash. The device
  restarted the app task (`SwipeUpClean`) during the capture-data edit, and the
  package switched between `app.spektralab` and its `.u02` clone, so the
  remaining pass is recorded for ticket 06.

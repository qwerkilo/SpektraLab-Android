# 04: M3 Date back secondary page

**What to build:** The Date back card opens its own M3 secondary page with the
upstream date controls. The default text comes from the Frame's EXIF date, and
Back returns to Edge without losing the Look.

**Blocked by:** 01 (Core parameter, Look, JNI, and catalogue sync)

**Status:** pending

- [ ] Date back has an on/off switch; controls stay visible and are disabled/dimmed while off.
- [ ] Text, left/top insets, size, exposure, corner, style, and capture-data line are present and write the full Look.
- [ ] Default text follows the Frame's EXIF capture date, not the current clock.
- [ ] The preview imprint and the export use the same text and layout values.
- [ ] Back restores the Edge page; switching Frame restores that Frame's Date back state.
- [ ] Device check: edit, disable, re-enable, export; both themes and large text.

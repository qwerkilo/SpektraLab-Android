# 06: Scan inspect

**What to build:** Add Scan inspect over the preview: a draggable scan line samples the current preview pixels and reports R, G, B and a wavelength estimate, with the same position marked on the spectral curve. It works by drag, tap, and arrow keys, and is read-only.

**Blocked by:** 02 (Console shell), 03 (Film panel)

**Status:** ready-for-agent

- [ ] Scan inspect is disabled when no preview exists; it never shows placeholder numbers.
- [ ] Dragging, tapping, and the left/right arrow keys move the scan line; the line exposes a 0–100 value to accessibility services.
- [ ] The readout reports real R, G, B from the preview at the line and a wavelength estimate within 380–780 nm.
- [ ] The spectral curve marks the same fraction as the scan line.
- [ ] The scan sample clamps at both edges and returns the expected colour for a uniform row and a single coloured pixel.
- [ ] Scan inspect never mutates a Look, never starts a render, and never changes export.
- [ ] The overlay works in both Theme modes and does not obscure the Original compare.

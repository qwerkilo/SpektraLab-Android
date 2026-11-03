# 09: Scan and compare in M3

**Type:** task
**Blocked by:** 02
**Status:** completed

**What to build:** Scan inspect and the compare divider keep their capabilities
but use M3 readout chips, state layers, and accessibility labels. Scan remains
read-only and never starts a render.

**Blocked by:** 02

- [ ] Scan toggle and readout use M3 components; RGB and λ text keep their measured semantics.
- [ ] Scan line drag, tap, arrow keys, Shift+arrows, Home/End, and the 0–100 RangeInfo still work.
- [ ] Grayscale and the line of purples show λ — rather than a fabricated number.
- [ ] Compare divider and long-press hold keep their 48 dp hit bands; Scan off restores the original compare gestures.
- [ ] Per-Frame Scan fraction and visibility survive Frame switches, theme switches, and a 599↔600 dp resize.
- [ ] Device check: Original vs rendered preview, both themes; no render from Scan or compare.

**Resolution:** M3 scan chip and readout card landed; measured RGB/lambda semantics and compare gestures preserved; ScanSampleCheck 973. Merged into feat/m3-spec. Device was offline.

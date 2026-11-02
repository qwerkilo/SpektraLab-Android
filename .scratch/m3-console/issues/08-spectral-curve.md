# 08: Spectral curve inside M3

**Type:** task
**Blocked by:** 04
**Status:** pending

**What to build:** The spectral curve is rebuilt as an M3-aware custom view in
a Material surface. Measured wavelengths, channel colours, gaps, and the Scan
mark do not change; the surrounding labels, legend, and reveal do.

**Blocked by:** 04

- [ ] Curve labels, legend, and wavelength endpoints use M3 type roles; R/G/B remain the measured colours.
- [ ] Gaps stay gaps; a single point stays a dot; no run is bridged or dropped.
- [ ] The plotter reveal runs once per new curve and settles at the full plot; it does not replay on a render-state refresh.
- [ ] Reduced motion shows the full curve immediately.
- [ ] Scan inspect's mark follows the same fraction axis and disappears when Scan is off.
- [ ] Device check with a real 81-sample stock, a gapped stock, and an unavailable spectrum; both themes.

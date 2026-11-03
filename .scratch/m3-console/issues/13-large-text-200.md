# 13: 2.0× large-text polish

**What to build:** The shell stays legible at Android's maximum 2.0× font scale:
the wordmark, theme action, empty-stage import control, and Readout labels do
not collide or truncate essential names.

**Blocked by:** None (follows ticket 11)

**Status:** completed

**Resolution:** At `font_scale >= 1.6` the top app bar drops `/ CONSOLE` and the
English `LIGHT/DARK` half of the theme action, so `SpektraLab`, `亮/暗`, and
`导出` all fit on one row. The empty-stage Scan pill is hidden when there is no
preview; the import pill is biased left so it clears `PRINT EXPOSURE`; the
Readout titles wrap to two lines instead of ellipsizing. At 1.3× the tagline is
still shown and the wider film cards keep `CineStill 800T` readable.

Verified on `10.110.67.69:39613` at 1.3× and 2.0×, light theme; font scale
restored to 1.0×. `checkJvm` remains green.

- [x] `SpektraLab`, theme, and Export fit on the 2.0× top app bar.
- [x] Empty-stage import control does not overlap `PRINT EXPOSURE`.
- [x] `EXPOSURE / 曝光` and `SPECTRUM / 光谱` wrap instead of ellipsizing at 2.0×.
- [x] 1.3× keeps `/ CONSOLE` and fits `CineStill 800T`.

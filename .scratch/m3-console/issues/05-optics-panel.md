# 05: Optics panel in Material 3

**Type:** task
**Blocked by:** 02
**Status:** pending

**What to build:** The Optics workspace uses Material sliders with a visible
parameter name, live value, unit, display-only Δ, and reset. The four
parameters keep their current defaults and ranges, and each slider exposes its
real range to accessibility services.

**Blocked by:** 02

- [ ] EXPOSURE, GRAIN, HALATION, and GLARE each show name, value, unit, Δ, and reset; all four fit without clipping at 390 dp.
- [ ] Sliders use M3 behaviour and 48 dp touch height; TalkBack announces the parameter, current value, unit, and range.
- [ ] Slider release sends one parameter update and one render; reset is a no-op when already at the default.
- [ ] Δ is recomputed from the value and the panel default; no new Look field is introduced.
- [ ] Locked Optics still disables the rows and explains why.
- [ ] Device check: move and reset each row, both themes; one render per gesture in logcat.

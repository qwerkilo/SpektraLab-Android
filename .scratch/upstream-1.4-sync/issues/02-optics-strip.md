# 02: M3 optics parameter strip and slider

**What to build:** The Optics workspace can reach all 22 engine parameters
through a compact M3 parameter strip and one large slider, plus the camera
filter and protection norm choices. The Film, Paper, Spectrum, and Readout
strip remain intact.

**Blocked by:** 01 (Core parameter, Look, JNI, and catalogue sync)

**Status:** pending

- [ ] The strip lists all 22 parameters; selecting one binds the slider and shows its name, value, unit, and display-only delta.
- [ ] The slider uses the parameter's real range, step, and unit, and exposes that range to accessibility services.
- [ ] Reset returns only the active parameter to its default and is a no-op when already there.
- [ ] Camera filter offers the eight Wratten choices; protection norm offers power, luminance, and peak.
- [ ] Every choice updates one Look and starts one render; the strip scrolls instead of clipping at 390 dp and under large text.
- [ ] Device check: change each parameter and both choices, both themes; TalkBack announces role, value, and range.

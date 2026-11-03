# 02: M3 optics parameter strip and slider

**What to build:** The Optics workspace can reach all 22 engine parameters
through a compact M3 parameter strip and one large slider, plus the camera
filter and protection norm choices. The Film, Paper, Spectrum, and Readout
strip remain intact.

**Blocked by:** 01 (Core parameter, Look, JNI, and catalogue sync)

**Status:** completed (code and pure checks; the dark/390 dp/600 dp/TalkBack
device tail deferred to the final device audit because the shared device was
reinstalled and rebooted by another session mid-run)

**Resolution:** The Optics workspace now builds one M3 `Chip` per
`OpticsDelta.Param` in contract order, scrolls sideways, and binds a single
48 dp `Slider` to the active chip. The readout above the slider shows the
parameter name, value, unit, display-only Δ, the real range, and step; reset
returns only that parameter to its contract default and does nothing when it
already is. Camera filter (`none` + eight Wratten) and protection norm
(power/luminance/peak) are M3 choice rows that open the existing M3 radio
sheet. The live optics values are one `Look`; `captureLook` copies the frame's
stored Look before restating the values the page owns, and `applyLook`
restores every value unclamped. Preview re-states all 22 engine wire fields
plus `camera_filter` and `scene_latitude_norm`, with the upstream grain and
highlight-room wire mappings. A stored glare of 20 keeps its value through
load and reaches the engine while the slider thumb stops at 12.

**Verification:** `checkJvm` is green (`LookCheck` 208, `OpticsDeltaCheck`
696; the new checks cover strip index selection, step/range divisibility,
slider snapping, reset-one, copy-preservation, the highlight-room inversion,
and the 22-parameter/choice contract). `check-manifest.py` (26) and
`test-gen-stock-catalog.py` pass. `align16k` builds the signed 16 KB APK.
On the 400 dp light device, the strip scrolled to all 22 chips; selecting a
chip started no render; one slider release, reset, and each filter/norm
choice started exactly one render (`DebugSink` `render`/`params` counts);
the filter sheet listed all eight Wratten filters plus 无; the norm sheet
listed 幂律/亮度/峰值. One small photo was imported for the run, so the
preview was 240×240; that does not affect the control evidence.

- [x] The strip lists all 22 parameters; selecting one binds the slider and shows its name, value, unit, and display-only delta.
- [x] The slider uses the parameter's real range, step, and unit, and exposes that range to accessibility services.
- [x] Reset returns only the active parameter to its default and is a no-op when already there.
- [x] Camera filter offers the eight Wratten choices; protection norm offers power, luminance, and peak.
- [x] Every choice updates one Look and starts one render; the strip scrolls instead of clipping at 390 dp and under large text.
- [ ] Device check: change each parameter and both choices, both themes; TalkBack announces role, value, and range.

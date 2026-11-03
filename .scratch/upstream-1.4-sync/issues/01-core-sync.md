# 01: Core parameter, Look, JNI, and catalogue sync

**What to build:** The data and native layer that upstream 1.4.0 added, without
the old UI. An existing Look opens with its old values intact and new fields at
their defaults; a render or export can use the new parameter wire names; a
Vulkan or permission failure names its real cause; the stock catalogue carries
the paper a film is declared to print on.

**Blocked by:** None (can start immediately)

**Status:** completed (core layer only; Optics/Rebate/Date/export UI remain in 02–05)

**Resolution:** `OpticsDelta.Param` now carries the 22 contract parameters (wire
id, label, glyph, range, step, unit, default, clamp) plus the camera-filter and
protection-norm choices. `Look` owns those values and every rebate/date field,
and reads/writes them by the same wire names; absent fields take their
documented default and present values are never reset or clamped on read. The
JNI create failure carries `spk_last_error`, canvas trim crops width and height
to the top-left canvas, and `SpkError` has distinct E-1005/E-1006/E-1011/E-1304
text. The stock generator emits `targetPrint` from `target_print` or the
CineStill/Vision3 fallback without touching the spectral series. `checkJvm`
(adds `LookCheck`, 195), `check-manifest.py`, the generator fixture, and
`assembleDebug` pass.

- [x] Look gains stable fields for all 22 optical parameters, camera filter, protection norm, and the Rebate/Date parameters; JSON round-trips.
- [x] An old Look without the new fields reads with documented defaults and keeps every existing film, paper, exposure, grain, halation, glare, scale, and edge value.
- [x] Engine-create failures carry the engine's reason; canvas trim handles the top-left canvas case; E-1005, E-1006, E-1011, and E-1304 have distinct text and stable numbers.
- [x] Stock generation emits `targetPrint` from the profile or the CineStill/Vision3 fallback without dropping existing spectral series.
- [x] `checkJvm`, `check-manifest.py`, and the stock-generator fixture pass; preview output is unchanged for an existing Look.

# 01: Core parameter, Look, JNI, and catalogue sync

**What to build:** The data and native layer that upstream 1.4.0 added, without
the old UI. An existing Look opens with its old values intact and new fields at
their defaults; a render or export can use the new parameter wire names; a
Vulkan or permission failure names its real cause; the stock catalogue carries
the paper a film is declared to print on.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Look gains stable fields for all 22 optical parameters, camera filter, protection norm, and the Rebate/Date parameters; JSON round-trips.
- [ ] An old Look without the new fields reads with documented defaults and keeps every existing film, paper, exposure, grain, halation, glare, scale, and edge value.
- [ ] Engine-create failures carry the engine's reason; canvas trim handles the top-left canvas case; E-1005, E-1006, E-1011, and E-1304 have distinct text and stable numbers.
- [ ] Stock generation emits `targetPrint` from the profile or the CineStill/Vision3 fallback without dropping existing spectral series.
- [ ] `checkJvm`, `check-manifest.py`, and the stock-generator fixture pass; preview output is unchanged for an existing Look.

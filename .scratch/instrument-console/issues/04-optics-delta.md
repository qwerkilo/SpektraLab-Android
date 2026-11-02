# 04: Optics panel with delta

**What to build:** Rework the optics panel into four calibrated rows: exposure, grain, halation, and glare. Each row shows its value, its unit, a reset, and a display-only Δ against that panel's default; changes update the Look and the Readout strip.

**Blocked by:** 02 (Console shell)

**Status:** ready-for-agent

- [ ] Exposure, grain, halation, and glare each have a labelled row, a live value, and the correct unit.
- [ ] Each row shows a signed Δ with two decimals against the panel default; the default shows 0.00.
- [ ] Changing a value updates the Look, the Readout strip, and the Δ; resetting a row returns it to the default.
- [ ] Δ is display-only and never becomes a second state or an engine parameter.
- [ ] The rows respect the engine's existing ranges and do not introduce new limits.
- [ ] The panel works in both Theme modes at 390 dp, with touch targets that are comfortable to drag.

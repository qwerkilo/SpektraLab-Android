# 04: Extract OpticsPanel

**Type:** task
**Blocked by:** 03
**Status:** completed

**Resolution:** `ui.panel.OpticsPanel` builds the whole optics workspace: head,
lock note, View/Meter/Scanner entries, 22-parameter strip, active readout
(name, reset, value, unit, Δ, range), the 48 dp slider and both choice rows
(camera filter, protection norm). `OpticsPanel.Host` carries chip selection,
reset, page entries, slider value/start/stop and the two pickers; the Activity
still owns the Look and the one-render-per-release policy.

**Verification:** device 800T frame unlocks the panel; H 高光保护 switches in
one tap with range 0–3 EV; a track tap moves the value to 1.50 EV and Δ +1.50;
the norm row opens the 幂律/亮度/峰值 sheet; 22 chips are guarded by
`PanelRowsTest`. Four gates green.

**Remaining:** move the explicit commit policy (`onOpticsSliderValue` /
`commitOptics`) behind the panel only if a later slice needs it; no UI rows are
left in the Activity.

**TDD seams**

- `OpticsDeltaCheck` and `LookCheck` stay the rule owners.
- Robolectric: 22 chips, slider, entries, lock state.
- Device: chip select, slider drag/release, View undo, meter page, scanner page.

**Acceptance**

- [ ] One render per release; zero on chip selection.
- [ ] Lock state matches the pre-slice behaviour.
- [ ] Four gates green.

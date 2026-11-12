# 04: Extract OpticsPanel

**Type:** task
**Blocked by:** 03
**Status:** in progress

**Resolution so far:** `ui.panel.OpticsPanel` builds the head, the 22-parameter
strip, the active-parameter readout and the lock surface; the shared filter-chip
style is `OpticsPanel.styleChip`. The Activity still builds the slider and the
two choice rows, owns the Look, and supplies `Host` for chip selection, reset
and the View/Meter/Scanner pages.

**Verification:** device 800T frame unlocks the panel; H 高光保护 is one
selection with range 0–3 EV; a track tap moves the value to 1.50 EV and the
readout/Δ update; 22 chips are guarded by `PanelRowsTest`.

**Remaining:** move the slider and choice rows into the panel, then move the
one-render-per-release policy behind it.

**TDD seams**

- `OpticsDeltaCheck` and `LookCheck` stay the rule owners.
- Robolectric: 22 chips, slider, entries, lock state.
- Device: chip select, slider drag/release, View undo, meter page, scanner page.

**Acceptance**

- [ ] One render per release; zero on chip selection.
- [ ] Lock state matches the pre-slice behaviour.
- [ ] Four gates green.

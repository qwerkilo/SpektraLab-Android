# 04: Extract OpticsPanel

**Type:** task
**Blocked by:** 03
**Status:** ready

**What to build:** Move the optics workspace into `ui.panel.OpticsPanel`.

- Own the parameter strip, one slider, View/Meter/Scanner entries and the panel
  lock.
- Host: `look`, edit optics, open a secondary page.
- Every release renders once; no hardcoded ranges (use `OpticsDelta.Param`).

**TDD seams**

- `OpticsDeltaCheck` and `LookCheck` stay the rule owners.
- Robolectric: 22 chips, slider, entries, lock state.
- Device: chip select, slider drag/release, View undo, meter page, scanner page.

**Acceptance**

- [ ] One render per release; zero on chip selection.
- [ ] Lock state matches the pre-slice behaviour.
- [ ] Four gates green.

# 05: Extract ScaleEdgePanel

**Type:** task
**Blocked by:** 04
**Status:** ready

**What to build:** Move scale, gate, rebate, date and compose into
`ui.panel.ScaleEdgePanel`.

- Own format cards, gate rows, rebate page, date page and composition sheet.
- Host: `look`, apply gate, open rebate/date/compose.
- Draft/apply semantics and `GateScale` normalization stay unchanged.

**TDD seams**

- `GateScaleCheck`, `DateBack`, `RebateControls` checks stay the owners.
- Robolectric: format cards at 1.0x/2.0x, date rows, rebate switch.
- Device: 135→645, portrait rebate, date imprint, compose apply/undo.

**Acceptance**

- [ ] 2.0x cards still 116x125 dp.
- [ ] Apply renders once; closing the sheet discards.
- [ ] Four gates green.

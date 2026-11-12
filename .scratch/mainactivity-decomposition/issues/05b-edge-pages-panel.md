# 05b: Extract EdgePagesPanel

**Type:** task
**Blocked by:** 05
**Status:** ready

**What to build:** Move the remaining Edge secondary pages out of
`MainActivity` into `ui.panel.EdgePagesPanel`.

- The Rebate page: switch, gate card, tone sliders, structure choice rows and
  seed values.
- The Date back page: switch, editable date text, inset/size/EV sliders,
  placement/corner/style rows, capture-data and second-frame text.
- The compose sheet's action shell stays until its own slice; do not move it
  here if the draft/apply seam would need a second Look copy.

**Why it is split:** both pages are built from the Activity's shared row
factories (`rebateValueRow`, `rebateSliderRow`, `edgeChoiceRow`) and the
`setEnabledDeep`/sync guards. The panel needs a `Rows` seat for those factories
before the page bodies can move without duplicating styling.

**TDD seams**

- `RebateControlsCheck`, `DateBackCheck`, `ChoiceRowCoverageCheck` stay owners.
- Robolectric: rebate switch enables the control group; date rows keep
  日期角位/日期样式; theme rebuild keeps an open page.
- Device: rebate on/off, date 角位 + 点阵, draft/apply unchanged.

**Acceptance**

- [ ] No rebate/date builder left in `MainActivity`.
- [ ] Open page survives a theme rebuild.
- [ ] Four gates green.

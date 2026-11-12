# 05: Extract ScaleEdgePanel

**Type:** task
**Blocked by:** 04
**Status:** in progress

**Resolution so far:** `ui.panel.ScalePanel` owns the Scale gate strip and its
cards: card geometry from `FormatCardLayout`, checked/selected styling, the
empty "no Gate fits" state, accessibility role, tap selection and reveal-scroll.
`ScalePanel.Host` supplies the active Gate and applies a tap; the Activity keeps
the header, compose entry, foot value, `pickScale` and the Look.

**Verification:** device tap on 135 半格 selects 24 mm, the foot reads `24 mm`
and the strip recomputes to `24 / 65 / 56 / 6×6`; `scaleCardsGrowWithTheFont`
reads the panel strip and still asserts 116×125 dp at 2.0×. Four gates green.

**Remaining:** Edge home, rebate page, date page and the compose sheet still
live in the Activity; move them into the same panel in the next slice.

**TDD seams**

- `GateScaleCheck`, `DateBack`, `RebateControls` checks stay the owners.
- Robolectric: format cards at 1.0x/2.0x, date rows, rebate switch.
- Device: 135→645, portrait rebate, date imprint, compose apply/undo.

**Acceptance**

- [ ] 2.0x cards still 116x125 dp.
- [ ] Apply renders once; closing the sheet discards.
- [ ] Four gates green.

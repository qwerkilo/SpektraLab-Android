# 05: Extract ScaleEdgePanel

**Type:** task
**Blocked by:** 04
**Status:** completed (rebate/date/compose split into 05b)

**Resolution so far:** `ui.panel.ScalePanel` owns the Scale gate strip and its
cards: card geometry from `FormatCardLayout`, checked/selected styling, the
empty "no Gate fits" state, accessibility role, tap selection and reveal-scroll.
`ScalePanel.Host` supplies the active Gate and applies a tap; the Activity keeps
the header, compose entry, foot value, `pickScale` and the Look.

**Verification:** device tap on 135 半格 selects 24 mm, the foot reads `24 mm`
and the strip recomputes to `24 / 65 / 56 / 6×6`; `scaleCardsGrowWithTheFont`
reads the panel strip and still asserts 116×125 dp at 2.0×. Four gates green.

**Also landed:** `ui.panel.EdgeHomePanel` owns the 边框 landing page — section
head slot, the 片边 / 日期水印 entry cards, live summaries through the Activity's
refresher list, and the footer note. Its `Host` supplies both summaries and
page entries.

**Device:** on `10.110.67.69:43657` the 边框 page shows both entry cards at
their 176 dp width with the live summaries (`未开启 · 点进去设置`), and tapping
片边 opens the REBATE page with 开启片边, 片门格式 135 全画幅, 片边文字, 片基雾
and 漏光. Screenshots `edge-home.png` / `edge-rebate-page.png`.

**Split:** the rebate page, date page and compose sheet need the Activity's row
factories, so they are carried by `05b-edge-pages-panel.md` rather than forcing
a second styling tree here.

**TDD seams**

- `GateScaleCheck`, `DateBack`, `RebateControls` checks stay the owners.
- Robolectric: format cards at 1.0x/2.0x, date rows, rebate switch.
- Device: 135→645, portrait rebate, date imprint, compose apply/undo.

**Acceptance**

- [ ] 2.0x cards still 116x125 dp.
- [ ] Apply renders once; closing the sheet discards.
- [ ] Four gates green.

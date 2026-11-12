# 05b: Extract EdgePagesPanel

**Type:** task
**Blocked by:** 05
**Status:** completed

**Resolution:** `ui.panel.EdgePagesPanel` now owns both secondary pages —
`rebatePage()`, `datePage()`, `syncRebate()`, `syncDate()`, `syncDateDetail()` —
and every page widget, including the gate card, switch rows, four rebate
sliders, four date sliders, choice/value rows, seed and text dialogs' bindings,
the page head and the `setEnabledDeep` dimming. The page row factories
(`rebateValueRow`, `rebateSliderRow`, `edgeChoiceRow`) moved with the pages
because only these two pages used them and keeping them in the Activity would
have duplicated the style tree. The Activity keeps the shared `switchRow` /
`m3TextButton` / dialog / `commitRebate` seats as `EdgePagesPanel.Rows` and
`Host`, plus `edgePageIndex`, page visibility and the Look.

`EdgeLookAccess` is the borrowed, typed view of one live Look's edge/date
fields. It keeps `Look`'s fields package-private while letting the panel read
and write the same object; it is created from `live()` per call and never
stored across a Frame switch. The panel adds its row refreshers to the
Activity's existing list, so `refreshEdgeCards()` still repaints the landing
cards and the open page in one pass.

`ChoiceRowCoverageCheck` now scans both `MainActivity.java` and
`EdgePagesPanel.java`, so every `DateBack` / `RebateControls` choice list must
still appear inside an `edgeChoiceRow(...)` call. `PanelRowsTest` adds
`rebateSwitchEnablesTheControlGroup`; the existing date-row and theme-rebuild
tests still pass.

**Verification:** `checkstyleGovernance`, `checkJvm`, `lintDebug`,
`testDebugUnitTest` (21 Robolectric tests, including the new rebate switch) and
`python app/build/check-manifest.py` (30 checks) are green in the worktree.

**Device risk:** the JVM suite cannot measure the page's pixel rows. On a
device, verify the rebate page still shows 开启片边, the gate card, 片边文字,
片基雾, 漏光, 片边张数, 光圈值, 片边模式, 片门形状, 齿孔, 片夹, 片边种子 and
帧种子 in order, and the date page still shows 日期角位 / 日期样式 / 点阵 plus
the live detail line; slider drag should preview and release should commit once.

## TDD seams

- `RebateControlsCheck`, `DateBackCheck`, `ChoiceRowCoverageCheck` stay owners.
- Robolectric: rebate switch enables the control group; date rows keep
  日期角位/日期样式; theme rebuild keeps an open page.
- Device: rebate on/off, date 角位 + 点阵, draft/apply unchanged.

## Acceptance

- [x] No rebate/date builder left in `MainActivity`.
- [x] Open page survives a theme rebuild.
- [x] Four gates green.

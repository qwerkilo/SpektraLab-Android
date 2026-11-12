# 07: Medium-width single content column

**Type:** bug
**Blocked by:** none
**Status:** completed

**Symptom:** at exactly 600 dp the two-column shell left the Frame column
`600 - 80 rail - 300 control = 220 dp`. The 2×2 readout clipped its labels, the
film section head clipped to `FILM S TOC...`, and the paper row truncated.

**Fix:** the rail still starts at 600 dp, but the stage and control deck stack
in one column until 720 dp. `ConsoleLayout.isTwoColumn()` owns the split; the
Activity reparents `controls` between `body` (phone/two-column) and `mainCol`
(medium single column). In medium mode the rail replaces the bottom tabs, so
the deck is `CONTROLS_OPEN_DP - TABS_HEIGHT_DP`.

**Gotcha found during the fix:** `setPanelCollapsed(false)` ran after the
responsive pass and reset the deck to `MATCH_PARENT` because it keyed only on
`wideLayout`. It now keys on `isTwoColumn`, otherwise the deck eats the stage.

**Evidence (device `10.110.67.69:43657`, release-signed 16 KB):**

- 390 dp: phone shell unchanged (screenshot `n3.png`).
- 600 dp (`wm size 1800x2608`): rail + stage + full-width 2×2 readout + film
  panel, no clipping; `FILM STOCK / 胶片` head fits.
- 720 dp (`wm size 2160x2608`): two columns with a 340 dp stage and 300 dp
  panel.
- Device restored to 1200×2608 / 480 dpi / font 1.0.

**Acceptance**

- [x] 600–719 dp: rail visible, stage above controls, both full content width.
- [x] ≥720 dp: two columns.
- [x] 390 dp phone unchanged.
- [x] `ConsoleLayoutCheck` covers 600/720; full-width and two-column sums are
      asserted separately.
- [x] Device screenshots at 600, 720 and 390 dp.

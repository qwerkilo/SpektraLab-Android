# 07: Medium-width single content column

**Type:** bug
**Blocked by:** none
**Status:** ready

**Symptom:** at exactly 600 dp (the rail breakpoint) the two-column shell
leaves the Frame column `600 - 80 rail - 300 control = 220 dp`. At that width
the 2×2 readout table clips its labels, the film section head clips to
`FILM S TOC...`, and the paper row truncates. Reproduced on `10.110.67.69:43657`
with `wm size 1800x2608` (600 dp at 480 dpi), before and after the panel
decomposition.

**Cause:** `ConsoleLayout` documents 220 dp as an accepted consequence, but it
is not usable. The stage floor (280), control floor (300) and rail (80) cannot
fit 600 dp, so the two-column layout must not start at 600.

**Fix direction:** add a medium mode at 600–719 dp: keep the navigation rail
and stack the stage and control deck in one content column (rail + single pane,
M3 medium guidance). Start the two-column split at 720 dp. This keeps the rail
at 600 dp and gives the Frame and panel the full 520 dp content width.

A first attempt wrapped `mainCol` + `controls` in a `content` column and flipped
its orientation, but the stage collapsed to zero height; revert was clean.
Implement with a layout test that asserts the stage and controls both get a
non-zero height in medium mode before relying on a screenshot.

**Acceptance**

- [ ] 600–719 dp: rail visible, stage above controls, both full content width.
- [ ] ≥720 dp: two columns as today.
- [ ] 390 dp phone unchanged.
- [ ] `ConsoleLayoutCheck` covers the medium boundary.
- [ ] Device screenshots at 600, 720 and 390 dp.

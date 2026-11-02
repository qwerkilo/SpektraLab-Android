# 05: Scale and Edge panels

**What to build:** Bring the scale and edge panels into the Console. Scale chooses the Frame's long edge and the Gate that fits it. Edge owns the Gate, the Rebate, and the date imprint, with the Frame and Rebate geometry kept consistent between preview and export.

**Blocked by:** 02 (Console shell)

**Status:** ready-for-agent

- [ ] Scale presents the shipped long-edge choices and marks the active one.
- [ ] Choosing a scale chooses the matching Gate; a Gate and a scale that cannot belong together are not offered.
- [ ] Edge exposes the Gate, the Rebate toggle, and the date imprint, using the existing engine parameters.
- [ ] Turning the Rebate on or off changes the preview and the export consistently; the edge print does not land on the picture.
- [ ] The date imprint appears only when enabled and uses the existing date text.
- [ ] Both panels work in both Theme modes at 390 dp and keep their values in the current Look.
- [ ] No new Gate, format, or engine parameter is introduced.

# 02: Extract StagePanel

**Type:** task
**Blocked by:** 01
**Status:** ready

**What to build:** Move the stage into `ui.panel.StagePanel`.

- Own the image view, scrims, file/exposure readouts, histogram, scan overlay,
  scan readout, busy label and progress.
- Host: `displayedBitmap`, scan state, toggle histogram, scan sample.
- `MainActivity` keeps the render thread and only calls `stage.show(bitmap)`.

**TDD seams**

- Robolectric: stage children exist; both themes; readout values.
- Device: import/render, scan on/off, histogram toggle, compare.

**Acceptance**

- [ ] Stage row order and values match the pre-slice screenshot.
- [ ] No render on theme rebuild or scan toggle.
- [ ] Four gates green.

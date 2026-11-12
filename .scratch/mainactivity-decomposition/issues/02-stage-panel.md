# 02: Extract StagePanel

**Type:** task
**Blocked by:** 01
**Status:** done

**Resolution:** `ui.panel.StagePanel` builds the whole stage:
Compare layer, Scan overlay, histogram, scrims, file/exposure readouts,
empty/RAW/busy states, Scan chip and readout card, progress bars and the
readout-width listener. `MainActivity` keeps per-frame state and render thread,
receives the panel's children as aliases, and talks to it through
`StagePanel.Host` (`displayedBitmap`, `scanBitmap`, import menu, scan toggle,
scan sample). Histogram visibility and scan-overlay/readout/chip visuals are
panel-owned; `syncScanAvailability` is one call. The host deliberately keeps the
source, scan fraction and histogram request because those are Frame state, not
view state; the panel owns only their presentation.

**Verification:** device import → source preview → Scan on (card + sample) →
theme rebuild keeps the image, source, scan line and readout; `StagePanelTest`
row guard added; four gates green.

**TDD seams**

- Robolectric: stage children exist; both themes; readout values.
- Device: import/render, scan on/off, histogram toggle, compare.

**Acceptance**

- [ ] Stage row order and values match the pre-slice screenshot.
- [ ] No render on theme rebuild or scan toggle.
- [ ] Four gates green.

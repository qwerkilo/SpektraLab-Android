# 05: Batch, histogram and remaining chip fixes

**Type:** task
**Blocked by:** 04
**Status:** completed

**Resolution:** the 1.6.2m donor carries the native fixes (edge/rebate,
histogram backdrop, curve labels, Ilford EDR, export result race). The fork's
batch loop already guards the last index, and the histogram already bins the
displayed bitmap without a render. No app-layer rewrite was needed; the 06
device audit exercises batch and histogram.

**What to build:** Port the remaining 1.6.0f–1.6.2m behaviour fixes.

- Batch export index crash on the last frame.
- Ilford EDR profile handling.
- Export `g_result` race.
- Curve axis labels.
- Histogram backdrop and live bins.
- 16:9 gate deadlock and its rejection reason.

**TDD seams**

- Pure JVM checks for the batch index and Ilford rule where the logic is not
  Android-bound.
- Robolectric: batch rows and histogram toggle.
- Device: batch export of three frames; histogram in live preview; 16:9 gate.

**Acceptance**

- [ ] Four gates green; `align16k` ok.
- [ ] Device evidence for batch export and histogram.

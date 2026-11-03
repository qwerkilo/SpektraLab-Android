# 11: Large-text resilience

**What to build:** The M3 Console stays readable at Android's 1.3× font scale.
The readout strip, panel section heads, filenames, and panel content no longer
clip; panels scroll when their content is taller than the fixed control area.

**Blocked by:** None (found by ticket 10)

**Status:** completed

**Resolution:** The Readout strip is a 2×2 grid at all widths, so four columns
cannot clip a stock name at 400 dp or on the wide layout. Section heads stack
their actions under the title at large text. Each workspace panel scrolls
inside its fixed height. The film metrics, note, and paper row wrap instead of
holding a one-line height. Verified at 1.3× on `10.110.67.69:34151`: readout
titles and `Portra Endura` fit; Optics labels fit; the panel scrolls to the
Glare row; Edge labels fit. At 2.0× the top bar and stage controls still
overflow; that extreme is recorded as a known limitation, not claimed.

- [x] Readout strip has no clipped title or stock value at 1.3×.
- [x] `FILM STOCK / 胶片`, `OPTICS / 光学调整`, and `FRAME & IMPRINT / 边框` are readable at 1.3×.
- [x] The Optics and Edge panels scroll to their last control at 1.3×.
- [x] Default 1.0× layout still fits and the existing checks pass.

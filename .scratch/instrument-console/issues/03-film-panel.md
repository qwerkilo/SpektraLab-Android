# 03: Film panel

**What to build:** Turn the film panel into the Console instrument: stock chips with an explicit active chip, a spectral curve for the selected film stock, the stock's measured values, and a short stock note. The Readout strip stays in sync with the panel.

**Blocked by:** 02 (Console shell)

**Status:** ready-for-agent

- [ ] Every shipped film stock appears as a chip; the active chip is obvious without reading the note.
- [ ] Choosing a chip updates the film stock, the Look, the Readout strip, and the spectral curve, and starts the normal render path.
- [ ] The curve is drawn from the selected stock's shipped spectral data across 380–780 nm.
- [ ] When a stock has no spectral samples, the panel shows its measured values and an explicit unavailable state instead of a fabricated curve.
- [ ] Measured values sit beside the curve and use the project's units and vocabulary.
- [ ] The panel works in both Theme modes at 390 dp without clipping or horizontal scrolling.
- [ ] The film panel does not keep a second copy of the film stock; it reads and writes the current Look.

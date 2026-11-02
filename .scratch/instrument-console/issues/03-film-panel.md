# 03: Film panel

**What to build:** Turn the film panel into the Console instrument: stock chips with an explicit active chip, a spectral curve for the selected film stock, the stock's measured values, and a short stock note. The Readout strip stays in sync with the panel.

Data source: the shipped profile JSON carries `data.wavelengths` (81 samples, 380–780 nm) and `data.log_sensitivity` (81×3, complete in sampled profiles); `data.channel_density` is the density alternative with real gaps. The stock catalog currently drops `data`. Extend the stock-catalog generator to bake the chosen series into the catalog; do not add an engine/JNI accessor. Preserve nulls as gaps. If the profiles are absent from a checkout, the unavailable state is the only truthful one.

**Blocked by:** 02 (Console shell)

**Status:** ready-for-agent

- [ ] Every shipped film stock appears as a chip; the active chip is obvious without reading the note.
- [ ] Choosing a chip updates the film stock, the Look, the Readout strip, and the spectral curve, and starts the normal render path.
- [ ] The curve is drawn from the selected stock's shipped spectral data across 380–780 nm.
- [ ] The curve uses the baked `log_sensitivity` series (or the declared density series); null samples render as gaps and are never interpolated or plotted as zero.
- [ ] The curve is not the prototype's static SVG path, and the panel does not claim a measured wavelength where it only has a bin index.
- [ ] When a stock has no spectral samples, the panel shows its measured values and an explicit unavailable state instead of a fabricated curve.
- [ ] Measured values sit beside the curve and use the project's units and vocabulary.
- [ ] The panel works in both Theme modes at 390 dp without clipping or horizontal scrolling.
- [ ] The film panel does not keep a second copy of the film stock; it reads and writes the current Look.

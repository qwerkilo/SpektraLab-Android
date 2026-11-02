# 02: Console shell

**What to build:** Replace the floating wordmark layout with the Console shell: a header row for brand, Theme mode, and export; a full-bleed preview with corner readouts and a spectral rail; a Readout strip showing film stock, print stock, exposure, and spectrum; and four tabs for film, optics, scale, and edge. The existing panels, source strip, and shelf stay reachable underneath.

**Blocked by:** 01 (Theme modes on the current editor)

**Status:** ready-for-agent

- [ ] At 390 dp the Frame fills the stage, with the header and Readout strip outside it and no horizontal overflow.
- [ ] The header exposes brand, Theme mode, and export; both Theme modes are legible.
- [ ] The Readout strip updates when the loaded Frame's film stock, print stock, or exposure changes.
- [ ] The spectrum cell reads the selected stock's shipped spectral data; when that stock has no samples it shows an explicit unavailable state and never invents a value.
- [ ] The spectral rail gives the spectrum a 380–780 nm reference without claiming to be a measurement.
- [ ] The four tabs switch the panel below; the active tab is visually and accessibly distinct.
- [ ] The existing source strip and shelf still reopen Frames, and import, render, and export still complete in both Theme modes.
- [ ] The shell does not introduce a second navigation system or a second Activity.

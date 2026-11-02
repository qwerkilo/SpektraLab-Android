# SpektraLab

The language of the film and paper editor: how a photograph, a look, and the darkroom controls are named.

## Editing

**Frame**:
One imported photograph together with the Look that belongs to it. A Frame owns the chip in the source strip and remembers its own Look.
_Avoid_: Image, photo, shot, slide

**Source strip**:
The row of Frame chips under the preview.
_Avoid_: Film strip, carousel

**Shelf**:
The folded source strip on the preview, showing a short stack of Frame thumbs when the full strip is hidden.
_Avoid_: Tray, drawer

**Source**:
The imported pixels of a Frame before any Look is applied.
_Avoid_: Original (the Original is the compare state, not the pixels)

**Original**:
The compare state that shows a Frame's Source without its Look.
_Avoid_: Before, source

**Look**:
The complete set of choices that turn a Frame's Source into a print: film stock, print stock, exposure, grain, halation, glare, scale, and edge settings.
_Avoid_: Preset, filter, style, theme

**Render**:
The engine work that applies a Look to a Frame's Source and produces the preview or the exported pixels.
_Avoid_: Develop, process, export (export is writing the file)

## Film and paper

**Film stock**:
The emulsion chosen for a Look.
_Avoid_: Film, negative, profile

**Print stock**:
The paper chosen for a Look.
_Avoid_: Paper, print, profile

**Exposure**:
The print exposure multiplier. 1.00 is the calibrated default.
_Avoid_: Brightness, EV (EV is the displayed unit, not the setting)

**Grain**:
The amount of emulsion grain in the print.
_Avoid_: Noise, texture

**Halation**:
The red glow around highlights caused by light scattering in the emulsion.
_Avoid_: Bloom, glow

**Glare**:
The amount of veiling glare in the print.
_Avoid_: Bloom, flare

## Frame geometry

**Scale**:
The Frame's long edge in millimetres. It selects the Gate that fits the Frame.
_Avoid_: Size, resolution, format

**Gate**:
The camera aperture that defines a Frame's dimensions, such as 135, 645, or 6×6.
_Avoid_: Format, edge, mask

**Rebate** (片边):
The film edge outside the Gate, including the edge markings.
_Avoid_: Border, margin, frame

**Overscan**:
The engine feature that draws the Rebate around the Frame.
_Avoid_: Edge, border

**Edge**:
The tool panel that owns the Gate, the Rebate, and the date imprint. It is a UI area, not a synonym for Rebate.
_Avoid_: Border, overscan

**Date imprint**:
The date a point-and-shoot camera burned into the Rebate.
_Avoid_: Timestamp

## Interface

**Console**:
The chosen editor shell: a full-bleed preview, a readout strip, four tabs, and an instrument panel. It is the A direction of the Instrument prototype.
_Avoid_: Dashboard, UI

**Readout strip**:
The row under the preview that shows the current film stock, print stock, exposure, and spectrum.
_Avoid_: Status bar, toolbar, HUD

**Spectral rail**:
The 380–780 nm reference scale along the preview edge. It gives the Spectrum a visual reference; it is not itself a measurement.
_Avoid_: Spectrum, color bar

**Spectrum**:
The selected film stock's response across 380–780 nm, plotted in the film panel and sampled by Scan inspect.
_Avoid_: Density curve, response graph

**Scan inspect**:
A read-only mode that samples the current preview along a draggable scan line and reports RGB and a wavelength estimate. It never changes a Look.
_Avoid_: Eyedropper, picker, probe

**Δ (delta)**:
The signed difference between an optics value and that panel's default. It is display-only.
_Avoid_: Offset, diff

**Theme mode**:
The light or dark presentation setting. It is saved as a preference and is not part of a Look.
_Avoid_: Skin, style

**Palette role**:
A named colour job, such as background, surface, ink, or accent, resolved to a colour by one Palette instance. Views ask for a role, never for a raw hex value.
_Avoid_: Color constant, theme colour

**Palette**:
The set of Palette roles resolved for one Theme mode.
_Avoid_: Theme, skin, colour scheme

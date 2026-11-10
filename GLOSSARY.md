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

**Frame Look session**:
The live Look and its undo history for one Frame. A Frame owns exactly one; switching Frame switches the session, not a copy of the Look.
_Avoid_: Editor state, session, working copy

**Built-in look**:
A shipped one-tap film/paper combination authored as a sparse overlay on the current Look. It keeps every value it does not name, including composition, crop, date and view settings.
_Avoid_: Preset, built-in preset, style

**Preset**:
A named, complete Look snapshot saved by the user. Applying it replaces the Frame's Look; it is not the same object as a Built-in look.
_Avoid_: Built-in look, recipe, style

**Preset strength**:
The 0–1 blend from the Look before a preset is applied to that preset's full Look. Continuous values interpolate; categorical values switch at 50 %.
_Avoid_: Opacity, mix, amount

**Session document**:
The app-private JSON that restores the current source URI, Look and selected page after the process is recreated. It is not the in-memory Frame Look session and does not store multi-frame undo history.
_Avoid_: Session, sidecar, editor state

**Transactional export**:
A gallery write whose `IS_PENDING` row is deleted on failure and published only after the copied bytes match the staged file's length and SHA-256 digest.
_Avoid_: Atomic export, safe save

**Render**:
The engine work that applies a Look to a Frame's Source and produces the preview or the exported pixels.
_Avoid_: Develop, process, export (export is writing the file)

## Film and paper

**Film stock**:
The emulsion chosen for a Look.
_Avoid_: Film, negative, profile

**Brand**:
The maker family a Film stock belongs to: Kodak, Fujifilm, Ilford, CineStill, or Other. The Film panel groups Film stocks by Brand; it is not a film format or a Look.
_Avoid_: Series, line, maker filter, category

**Reversal film** (反转片 / 幻灯):
A Film stock developed to a positive transparency. It is not printed onto paper in this editor; it is developed and scanned directly.
_Avoid_: Slide film, positive, transparency, chrome

**Direct scan**:
The stage used for a Reversal film: the developed film is scanned instead of printed, so the Print stock does not affect the result.
_Avoid_: Scan mode, no-paper, film scan

**Camera filter**:
A Wratten filter in front of the taking lens. It is not the enlarger's dichroic filter pack, which belongs to the print stage.
_Avoid_: Lens filter, colour filter

**Protection norm**:
The scene measurement used by highlight and shadow protection: power, luminance, or peak. It changes how protection is calculated, not how much is asked for.
_Avoid_: Exposure mode, metering mode

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

**Parameter strip**:
The horizontal row of M3 chips in the Optics workspace. It selects the active optical parameter; it does not itself change a value.
_Avoid_: Knob strip, slider list

**Active parameter**:
The one optical parameter currently bound to the Optics slider. It is a UI selection, not part of a Look.
_Avoid_: Selected knob, current control

**Secondary page**:
A page inside the Edge workspace for Rebate or Date back, opened from one of the two Edge cards and dismissed with Back. It carries its own on/off switch.
_Avoid_: Subscreen, dialog, nested panel

**Look re-statement**:
Sending the complete Look, including the Film stock and every parameter, to the engine for an export instead of relying on defaults left by an earlier render.
_Avoid_: Full params, export sync

**Theme mode**:
The light or dark presentation setting. It is saved as a preference and is not part of a Look.
_Avoid_: Skin, style

**Palette role**:
A named colour job, such as background, surface, ink, or accent, resolved to a colour by one Palette instance. Views ask for a role, never for a raw hex value.
_Avoid_: Color constant, theme colour

**Palette**:
The set of Palette roles resolved for one Theme mode.
_Avoid_: Theme, skin, colour scheme

## Material 3

**Material 3 (M3)**:
The Android design system supplied by the Material Components library: color
roles, type roles, shape, state layers, and component behaviour. It is the
control system, not a synonym for the Palette.
_Avoid_: Material Design, MD3 theme

**M3 color role**:
A semantic slot in the M3 scheme, such as primary, on-surface, surface-container,
or outline. Controls ask for the role; they do not carry a hex value.
_Avoid_: Palette role (the Palette role is the stage/spectrum exception)

**Dynamic color**:
The M3 scheme generated from the device wallpaper on Android 12+. It applies to
controls and surfaces, never to the photographic stage or the spectral R/G/B.
_Avoid_: Material You everywhere, adaptive palette

**M3 type role**:
A named position in the M3 type scale, such as display-small, title-medium, or
label-small. The role selects size, weight, and line height; the family stays
Inter or OPPOSans.
_Avoid_: Font size, text style

**Top app bar**:
The M3 header component that owns the Console wordmark, Theme mode, and Export.
It replaces the hand-built header row but not the Readout strip.
_Avoid_: Header, toolbar, nav bar

**Navigation rail**:
The M3 vertical navigation component used at ≥600 dp. It carries the same four
workspaces as the phone tabs and shares their selected state.
_Avoid_: Sidebar, drawer, side tabs

**State layer**:
The M3 translucent overlay that expresses hover, focus, pressed, or dragged
state without changing a control's bounds.
_Avoid_: Ripple (a ripple is one implementation of a state layer)

**M3 token**:
A named design value (color role, type role, shape, elevation, motion) that a
component reads instead of a hard-coded value.
_Avoid_: Constant, style variable

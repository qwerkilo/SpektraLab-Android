# Instrument Console — verification

Date: 2026-11-02. Scope: ticket 08, the final device pass for the Console shell.
The integration branch is `feat/instrument-console`; this note lives on
`ticket/08-device`.

## Device

- ADB target `10.110.67.69:43191`
- Android 17
- 1200 × 2608 px at 480 dpi, 400 dp wide

## Observed on the integration build

The main session installed the integration build and observed:

- **Header**: the brand stays on the page without competing with the console
  label, Theme mode chip, and Export command at 400 dp.
- **Readout strip**: the paper readout is shortened and film, paper, exposure,
  and spectrum still read at a glance.
- **Import**: the import crash is fixed; a JPEG import completed.
- **Theme modes**: light and dark both render; the header chip shows the active
  mode.
- **Scan inspect**: the toggle enables the scan line, drag and tap place it,
  RGB and the wavelength estimate update from the preview, and the readout sits
  above the shelf.
- **Film tab**: Portra 400 draws its shipped curve (81 samples), and the scan
  position is marked on that curve.
- **Wide window**: at and above 600 dp the shell reflows to the two-column
  Console with a capped control column.

Screenshots from that pass exist for light and dark at phone width and for the
tablet width.

## Fixed in ticket 08

- **Scan readout caption.** A bare `addView` in a vertical `LinearLayout` takes
  `MATCH_PARENT` width, so the caption was measured against the narrower RGB
  line and clipped to `SCAN INSPECT · CENT` at 390 dp. The caption and the λ
  line now wrap their content. The caption is the block's widest child, so the
  readout keeps one stable width while the sampled numbers change.
- **Wide controls column.** The rail now uses the surface role and the film
  curve takes the spare panel height. The default Film tab fills the column
  instead of leaving its lower half as bare page; Optics already distributed
  its four rows. No second navigation model was added.

These two edits were compiled but were not installed on the device from this
worktree.

## Unverified in this pass

- The rebuilt APK from this worktree, including the two fixes above.
- Final signing and the release build.
- The full export matrix: JPEG, TIFF, and DNG, each Theme mode, Original
  compare, Scale and Rebate changes, date imprint, theme toggle after a render,
  and restart after a theme change.
- `SpektraLabJNI` and `SpektraLabRAW` logcat review on this exact build.
- Tablet screenshots after the rail and curve change.

## Local checks

- `javac -source 17 -target 17` over `app/src/main/java`, against the local
  Android 34 SDK jar and JDK 21: passes. The Termux SDK 36 dex build was not
  run on this host; the integration owner rebuilds and installs the APK.
- Pure checks: `PaletteCheck` (126), `ThemeModeCheck` (13),
  `SpectrumSamplesCheck` (26), `SpectrumCurveCheck` (42), `OpticsDeltaCheck`
  (20), `GateScaleCheck` (117), `ScanSampleCheck` (946), `ConsoleLayoutCheck`
  (5225): all pass.
- `git diff --check`: clean.

## Known issues

- Final signing and the update channel are outside this ticket.
- On a tall tablet the Scale and Edge tabs still top-align their controls, so
  they leave breathing room inside the surface rail; Film and Optics fill.
- The caption and rail fixes need one confirming render in both Theme modes on
  the device.

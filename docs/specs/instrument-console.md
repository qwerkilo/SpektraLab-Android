# Instrument Console — implementation spec

Status: `ready-for-agent`. Tracker: GitHub Issues are disabled, so this spec lives in the repository until the tracker is enabled. Decision record: ADR-0001 (Instrument Console as the editor shell). Vocabulary: GLOSSARY.md.

## Problem Statement

The editor is currently a light-only page. Its wordmark floats over the preview, its tool panels separate the photograph from the controls, and its blue accent has no low-glare counterpart. Photographers working in a darkroom or at night have no dark mode, and the exposure, print stock, and spectrum they need while judging a Look are buried inside panels rather than visible beside the Frame.

The editor also has no way to inspect the colour under a point. Choosing a Look is therefore a guess until the render completes, and there is no bridge between the numbers in the panels and the pixels in the preview.

## Solution

Ship the Console shell from the Instrument prototype, with light and dark Theme modes designed independently:

- A full-bleed preview with corner readouts and a spectral rail.
- A readout strip under the preview showing film stock, print stock, exposure, and spectrum.
- Four tool tabs: film, optics, scale, edge.
- A film panel with stock chips, a spectral curve, and the stock's measured values.
- A draggable Scan inspect line that samples the current preview and reports RGB and a wavelength estimate.
- A Δ value on each optics row, showing how far the value moved from that panel's default.
- A theme toggle in the header that preserves the loaded Frame and its Look.

The photograph remains the primary surface. The instrument readouts support it rather than compete with it.

## User Stories

1. As a photographer, I want a light Theme mode, so that the editor feels like a paper proof under daylight.
2. As a photographer, I want a dark Theme mode, so that editing does not wash out my night vision.
3. As a photographer, I want the theme toggle in the editor header, so that I can switch without leaving the editor.
4. As a photographer, I want my theme choice remembered, so that the next launch opens in the same mode.
5. As a photographer, I want a theme switch to keep my loaded Frame and its Look, so that switching does not cost an import or a re-render.
6. As a photographer, I want a theme switch to avoid a white flash on launch, so that the editor never blinds me in the dark.
7. As a photographer, I want the system bars to match the active theme, so that the screen has no light band in dark mode.
8. As a photographer, I want body text and controls to stay legible in both themes, so that I do not have to strain to read values.
9. As a photographer, I want the Frame to fill the stage, so that the photograph remains the primary surface.
10. As a photographer, I want the readout strip to show film stock, print stock, exposure, and spectrum together, so that I can read the current Look at a glance.
11. As a photographer, I want the spectral rail to give the spectrum a spatial reference, so that I can connect the numbers to the image.
12. As a photographer, I want four clearly labelled tabs, so that I can reach film, optics, scale, and edge without hunting.
13. As a photographer, I want the active tab to be obvious, so that I know which panel the next control will change.
14. As a photographer, I want a film panel with stock chips and an explicit active chip, so that I can compare stocks without guessing which one is applied.
15. As a photographer, I want a spectral curve for the selected film stock, so that the stock's character is visible before I render.
16. As a photographer, I want the stock's measured values beside the curve, so that the curve is not the only description.
17. As a photographer, I want optics controls with values, units, and a reset, so that I can return to the calibrated default.
18. As a photographer, I want a Δ value on each optics row, so that I can see how far I moved from the default.
19. As a photographer, I want the scale control to choose the Gate that fits the Frame, so that the Frame and its Rebate cannot disagree.
20. As a photographer, I want the Edge panel to own the Gate, Rebate, and date imprint, so that film geometry has one home.
21. As a photographer, I want to enable or disable the Rebate, so that I can choose between a clean frame and a film edge.
22. As a photographer, I want a date imprint option, so that I can reproduce a point-and-shoot look.
23. As a photographer, I want a scan line over the preview, so that I can inspect the colour under a point before I commit a Look.
24. As a photographer, I want the scan readout to show real pixel RGB and a wavelength estimate, so that the numbers describe the image rather than a mock.
25. As a photographer, I want to move the scan line by drag, by tap, and with arrow keys, so that Scan inspect works with touch and keyboard.
26. As a photographer, I want the scan position marked on the spectral curve, so that the line and the curve stay legible together.
27. As a photographer, I want Scan inspect disabled when no preview exists, so that the editor never shows invented numbers.
28. As a photographer, I want the Original compare to keep working in both themes, so that the core review action does not regress.
29. As a photographer, I want import, RAW decode, render, and export behaviour unchanged, so that the visual work does not risk the engine path.
30. As a photographer, I want the editor to remain responsive while I drag the scan line or a slider, so that inspection does not feel like a render.
31. As a photographer on a tablet or foldable, I want the panel grid to use the extra width, so that the editor is not a stretched phone column.
32. As a user with reduced motion, I want theme and panel transitions to respect the system setting, so that the editor stays comfortable.
33. As a user with a screen reader, I want the theme toggle, tabs, sliders, and scan line to expose meaningful labels and values, so that the editor is operable without sight.
34. As a user with limited dexterity, I want touch targets large enough to hit, so that the instrument controls do not require precision tapping.
35. As a returning user, I want an empty state that tells me how to load a Frame, so that the editor does not look broken before the first import.
36. As a user who hits a load or render error, I want the error to keep its `[E-xxxx]` code, so that I can report it accurately.

## Implementation Decisions

### Seams

- **Palette seam.** Add one pure palette module with named roles: background, surface, elevated surface, ink, muted ink, line, accent, on-accent, accent-soft, stage, overlay, and system-bar colours. It exposes a light instance, a dark instance, and a contrast-ratio function. Views ask for roles, never for raw hex values.
- **Scan seam.** Add one pure scan-sample module: given a row of preview pixels, a width, and a horizontal fraction, it returns RGB and a wavelength estimate, clamped at both edges. It does not know about views, the engine, or the Look.
- **Composition seam.** The existing Activity composition root remains the only place views are assembled. It takes the active palette and applies it during construction; the theme toggle rebuilds through the same root. No second navigation system, no second Activity.
- These are two new seams, both pure and Android-free, plus the existing composition root. The user confirmed this shape over a single deep `EditorUiState` module or no new seams.

### Theme

- Theme mode is a persisted preference, read before the view tree is built. Light and dark are independent designs, not an inversion of one another.
- The toggle writes the preference and rebuilds the view tree in place. Activity recreation is rejected because it would drop the loaded Frame, its Look, and the native session.
- After the rebuild, a restore pass reapplies the current Frame's Source and rendered preview, film and print selection, Look values, active tab, shelf state, and scan position. A theme switch must not trigger a render.
- System bars and status-bar icons follow the active palette. The window background is set from the palette before the first frame to avoid a white flash.
- Theme mode and Scan inspect are presentation state. They are not part of a Look, are not sent to the engine, and are not saved into a Frame.
- Reduced-motion settings suppress non-essential theme and panel transitions.

### Console shell

- Replace the floating wordmark with a header row: brand, theme toggle, export.
- A full-bleed stage carries the preview, the corner readouts, and the spectral rail.
- A readout strip under the stage shows film stock, print stock, exposure, and spectrum.
- Four tabs sit below the readout strip: film, optics, scale, edge. The active tab owns the panel below it.
- The existing source strip and shelf remain the way Frames are revisited; they are restyled, not replaced.
- Light theme uses warm paper and ink-green. Dark theme uses graphite and phosphor green. Both palettes are designed as complete systems, not sampled from each other.

### Panels

- **Film.** Stock chips, an explicit active chip, a spectral curve, measured values, and a stock note. Changing a chip updates the film stock and starts the normal render path.
- **Optics.** Exposure, grain, halation, and glare with values, units, and reset. Each row gains a display-only Δ against that panel's default.
- **Scale.** The Frame's long edge in millimetres, with the Gate that fits it. Choosing a scale chooses the matching Gate; the Frame and Rebate must not disagree.
- **Edge.** Gate, Rebate, and date imprint. The Rebate and date imprint remain independent toggles with their existing engine parameters.
- The readout strip and panels read from the same Look. No panel keeps a duplicate of a Look value.

### Scan inspect

- Scan inspect samples the current preview bitmap. It reports R, G, B and a wavelength estimate derived from the sampled colour.
- It is read-only: never mutates a Look, never starts a render, never changes export.
- It is reachable by drag, tap, and arrow keys. The scan line exposes a 0–100 value to accessibility services.
- The spectral curve marks the same fraction as the scan line.
- When no preview bitmap exists, Scan inspect is disabled rather than showing placeholder numbers.

### Type, colour, and accessibility

- Keep the existing Inter and OPPO Sans stack. The prototype's display and mono faces are references for weight and spacing, not new binary assets.
- Body text targets 4.5:1 contrast in both palettes; large text and control boundaries target 3:1.
- Theme toggle, tabs, sliders, and scan line are focusable and labelled. Touch targets meet the platform minimum.
- The `[E-xxxx]` codes in user-facing errors are preserved when error text changes.

### Engine boundary

- No engine, shader, tiling, parameter, import, or export change is implied by this UI work.
- Raw and JPEG import, native session lifetime, render locking, and export formats stay as they are.

## Testing Decisions

- A good test checks externally visible behaviour, not private fields or view internals. For this feature that means: the persisted theme survives a restart, a theme toggle keeps the Frame and its Look, a scan sample matches a known pixel, and import/render/export still complete.
- **Palette module.** Test every role is present in both instances; body-text contrast is at least 4.5:1; large text and control boundaries are at least 3:1; the two backgrounds differ. This is a pure JVM check, run with the same `javac`/`java` toolchain as the app. No new test framework.
- **Scan-sample module.** Test a uniform row, the left and right edges, a midpoint, a row with a single coloured pixel, and the no-preview case. The wavelength estimate must be monotonic across the horizontal fraction and stay within 380–780 nm. Pure JVM check, no Android dependency.
- **Composition root.** No automated UI test exists. Verify manually on a device: toggle theme with no image, with a RAW, and with a JPEG; confirm the Frame, its Look, the active tab, the shelf state, and the scan position survive; confirm no render starts on toggle.
- **Prior art.** The repository has no test suite and no JVM test harness. The existing verification is compile, install, and logcat. These pure checks are a small addition for the two new seams, not a new UI test framework.
- **Manual matrix.** No image; RAW loaded; JPEG loaded; Original compare; each tab; each theme; one export; one Edge and Scale change; Scan inspect by drag, tap, and arrow keys; theme toggle after a render; restart after a theme change; tablet width once.
- **Logcat.** Watch `SpektraLabJNI` and `SpektraLabRAW` during one import and one render in each theme. A theme switch must produce no render or engine error.

## Out of Scope

- New film or paper profiles.
- Engine, shader, tiling, parameter, or export changes.
- A new brand identity, logo, icon set, or bundled display/mono fonts.
- Animated page transitions beyond theme and panel state changes.
- A full tablet information architecture; this spec requires the panel grid to reflow, not a second navigation model.
- Theming of system pickers and third-party activities.
- Persisting the loaded Frame across process death.
- A full accessibility audit beyond the controls named here.

## Further Notes

- ADR-0001 records why Console was chosen over Darkroom and Contact Sheet. Do not reopen that choice in implementation without a new ADR. See [ADR-0001](../adr/0001-instrument-console-ui.md) and [GLOSSARY.md](../../GLOSSARY.md).
- Contact Sheet contributes the Δ column; Instrument contributes Scan inspect. Those are the only cross-direction borrowings.
- The prototype is a throwaway primary source. Where Android type sizes, touch targets, or system insets disagree with it, Android wins.
- GitHub Issues are disabled on the repository. Until they are enabled, this spec is the tracker record and tickets use the local-file fallback.
- Work happens on the `feat/instrument-console` branch. `master` is not touched until the branch is verified and approved for merge.
- Update the glossary when a UI term is resolved. Update ADR-0001 only when the decision changes, not when the implementation does.

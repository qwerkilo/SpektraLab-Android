# Instrument Console — implementation spec

Decision (2026-11-02): the editor ships the **A · Console** direction. C's scan line becomes an inspect mode; B's Δ column merges into the parameter panel. Every screen must support light and dark themes.

Prototype: `prototypes/ui-instrument.html` (`?variant=A|B|C&theme=light|dark`). It is a throwaway primary source, not production code.

## Problem Statement

The current editor is a single light-only page: a floating wordmark over the preview, a source strip, four tool panels, and a blue accent. Photographers working in a darkroom or at night have no low-glare mode. The preview also lacks the instrument readouts the prototype proved valuable: exposure, paper, and spectrum are buried in panels instead of visible beside the frame.

## Solution

Rebuild the editor shell as a calibrated instrument:

- A full-bleed preview with corner readouts and a spectral rail.
- A readout strip under the preview: film, paper, exposure, spectrum.
- Four tabs: film, optics, scale, edge.
- A film panel with stock chips, a density curve, and measured values.
- Light and dark palettes designed independently. Light is warm paper and ink-green, not an inverted dark theme.
- An optional scan inspect mode: a draggable line samples the preview and reports RGB and λ, with a density spectrum.

## User Stories

1. As a photographer, I want a light theme, so that the editor feels like a paper proof under daylight.
2. As a photographer, I want a dark theme, so that editing does not wash out my night vision.
3. As a photographer, I want the theme toggle in the editor header, so that I can switch without leaving the frame.
4. As a photographer, I want my theme choice remembered, so that the next launch opens in the same mode.
5. As a photographer, I want a theme switch to keep my loaded frame and look, so that switching does not cost an import or a re-render.
6. As a photographer, I want the frame to fill the stage, so that the photograph remains the primary surface.
7. As a photographer, I want exposure, film, paper, and spectrum visible beside the frame, so that I can read the current look without opening a panel.
8. As a photographer, I want a film strip with explicit active state, so that I can compare stocks without guessing which one is applied.
9. As a photographer, I want a density curve for the selected stock, so that the stock's character is visible before rendering.
10. As a photographer, I want optics values with units and a reset, so that I can return to the calibrated default.
11. As a photographer, I want the B-ledger Δ column beside each parameter, so that I can see how far I moved from the default.
12. As a photographer, I want a scan line over the preview, so that I can inspect the colour under a point before I commit a look.
13. As a photographer, I want the scan readout to show real pixel RGB and a wavelength estimate, so that the numbers describe the image, not a mock.
14. As a photographer, I want to move the scan line by drag, by tap, and with arrow keys, so that the inspect mode works with touch and keyboard.
15. As a photographer, I want the scan spectrum to mark the same position as the line, so that the two readings stay legible together.
16. As a photographer, I want the original-frame compare to keep working in both themes, so that the core review action does not regress.
17. As a photographer, I want the system bars and status bar to match the active theme, so that the screen does not flash a light band in dark mode.
18. As a photographer on a tablet or foldable, I want the panel grid to use the extra width, so that the editor is not a stretched phone column.
19. As a photographer, I want export, import, RAW decode, and render timing unchanged, so that the visual change does not risk the engine path.
20. As a user with reduced motion, I want theme and panel transitions to respect the system setting, so that the editor stays comfortable.

## Implementation Decisions

- **Palette seam.** Introduce one UI palette object with named roles: background, surface, elevated surface, ink, muted ink, line, accent, on-accent, accent-soft, stage, overlay, and system-bar colours. Views read roles, never raw hex values. The palette has a light and a dark instance.
- **Theme mode.** Persist `light` or `dark` in preferences. The editor reads it before building the view tree. The toggle writes it, then rebuilds the tree in place.
- **Rebuild seam.** `buildUi()` remains the single composition root. A post-build restore step reapplies the current source frame, before/after bitmaps, film/print selection, look values, active tab, shelf state, and scan position. Do not use Activity recreation for the toggle; it would drop the loaded frame and the native session.
- **Composition.** Replace the current floating header with the prototype's header row: brand, theme pill, export. Keep the existing stage, source bar, and panel holder as the wiring points; restyle them rather than inventing a second navigation system.
- **A · Console.** Full-bleed stage, spectral rail, readout strip, four tabs, and a film panel with stock chips plus a density curve. The prototype is the visual source; Android type sizes and touch targets take precedence where they differ.
- **B · Ledger contribution.** Add a Δ value to each optics row. Δ is the difference from the panel default, shown with a sign and two decimals. It is display-only; it does not become a second state.
- **C · Scan inspect.** Add an inspect mode over the preview. It samples the current preview bitmap at the scan line, reports R/G/B and a λ estimate, and draws a marker on a density spectrum. It is read-only: it never mutates `Look`, never renders, and never changes export. If no preview bitmap exists, the mode is disabled rather than showing invented numbers.
- **State model.** `Look` remains the single source for film, print, exposure, grain, halation, glare, scale, and edge settings. Theme and inspect mode are presentation state and do not travel into the engine or saved frames.
- **Accessibility.** Theme pill and scan line are focusable; the scan line exposes a value between 0 and 100 and supports left/right arrows. Contrast in both palettes targets 4.5:1 for body text and 3:1 for large text and control boundaries.
- **Fonts.** Keep the existing Inter/OPPO Sans stack. The prototype's Space Grotesk and IBM Plex Mono are references for weight and spacing, not new binary assets.
- **No engine changes.** No protocol, parameter, import, tiling, or export change is implied by this UI work.

## Testing Decisions

- There is no automated test suite and no JVM test harness. A good test still checks external behaviour: the theme survives a rebuild, the frame survives a toggle, the scan readout matches a known pixel, and the existing import/render/export path still completes.
- Verification is: compile the Java/dex path, install on a device, then capture light and dark screenshots at 390 dp and one tablet width. Watch logcat tags `SpektraLabJNI` and `SpektraLabRAW` during one import and one render in each theme.
- Add a small palette contrast check as a build-time script if the palette lands as data. It should fail when body text falls below 4.5:1. This is a pure check, not a UI test.
- Prior art: there is no UI test suite; the existing verification is manual build, install, and logcat. Do not invent a new test framework for this feature.
- Manual matrix: no image, RAW loaded, JPEG loaded, original compare, each tab, each theme, one export, one edge/scale change, scan drag, scan keyboard, and a theme toggle after a render.

## Out of Scope

- New film or paper profiles.
- Engine, shader, tiling, or export changes.
- A new brand identity, logo, or icon set.
- Animated page transitions beyond theme and panel state changes.
- A full tablet information architecture; this spec only requires the grid to reflow, not a second navigation model.

## Further Notes

- The repo currently has GitHub Issues disabled, so this spec lives in `docs/specs/` and tickets will use the local-file fallback until the tracker is enabled.
- Work happens on `feat/instrument-console`; `master` is not touched until the branch is verified and approved for merge.

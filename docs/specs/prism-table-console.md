# Prism Table console

Status: `in-progress` — slices 1 (shell) and 2 (dock) are implemented; the
device polish matrix in slice 3 is still open.

The selected liquid-glass direction for the M3 console. Prism Table keeps the
Prism HUD's full-bleed photograph, circular exposure readout and bottom sheet,
then quiets the chrome: a 2×2 readout table over the stage, a slim text dock,
and a low-contrast glass sheet.

Prototype: `prototypes/ui-prism.html?variant=C` (throwaway; the prototype folder
stays out of production).

## What changes

- **Glass surface language.** Translucent surfaces with a 1 px specular top edge,
  a faint inner highlight, and a soft shadow. No hard-coded colour or text size:
  every value comes from an M3 role or a new `GlassStyle` token set.
- **Floating top bar.** The wordmark, theme action, undo and export move onto a
  glass capsule over the top of the photograph. The stage starts at the top of
  the window; the bar no longer consumes a separate 64 dp page row.
- **Readout table.** Film, paper, exposure and spectrum become a 2×2 glass table
  directly under the top bar. It reads the current `FrameLookSession.look()`; it
  is not a second copy of any Look value.
- **Slim dock.** The four tabs become a low, text-first dock at the bottom of the
  window. The panel sits above it as a glass sheet, with the same film / optics /
  scale / edge pages.
- **Quiet sheet.** The panel background becomes translucent and the existing rows
  keep their M3 controls. Stock rows stay text rows; no new card grid is added.
- **Both themes.** Light is a frosted-white surface with a teal accent; dark is a
  smoked surface with the existing lime accent. Dynamic colour still wins at the
  chrome layer, and the photograph never changes with the theme.

## What does not change

- `FrameLookSession` remains the only live Look owner; preview, export and RAW
  import keep `RenderSnapshot`.
- One render per gesture, one render at a time, and no render during a theme
  rebuild.
- The 390 dp phone column and the ≥600 dp rail layout keep their behaviour. The
  Prism Table changes the composition, not the responsive contract.
- Scan, compare, histogram, export, presets, metering, hue and gamut keep their
  current capabilities.

## Technical note: what "glass" means on Android

Android has no CSS `backdrop-filter` for arbitrary in-Activity views. Prism
Table therefore uses a **simulated glass** surface: a translucent gradient,
specular border and inner highlight over the live stage, with elevation and
alpha. `Window.setBackgroundBlurRadius` is available only on API 31+ and only
for windows (dialogs / sheets), so the first slice must not claim true backdrop
blur. If a later slice converts the film sheet to a `BottomSheetDialog`, that
sheet may use window blur on API 31+; the in-page dock and top bar still use the
simulated surface.

## Seams and checks

| Seam | Kind | Check |
|---|---|---|
| `GlassStyle` tokens (alpha, border, elevation, accent) | pure | `PaletteCheck` contrast + `M3TokenCheck` |
| Top bar / readout / dock composition | Activity wiring | `PanelRowsTest` (Robolectric) |
| Theme rebuild keeps the stage, tab and sheet | Activity wiring | existing `themeSwitchKeepsTheOpenEdgePage` + new Prism row assertions |
| Panel rows survive the shell change | Activity wiring | existing Date / Optics / view assertions |

## Slices

1. **Tokens + shell.** Add `GlassStyle`, move the top bar over the stage, make the
   readout a floating table, and give the page a glass background. No panel
   content changes.
2. **Dock + sheet.** Move the tabs to a slim bottom dock, put the panel above it
   as a glass sheet, and reorder the controls column without changing `setTab`.
3. **Polish + device pass.** Light/dark, 390 dp and ≥600 dp, large text, and the
   no-extra-render checks. Capture `prism-*.png` in `prototypes/shots/`.

## Implemented

- `app/src/main/java/app/spektralab/ui/GlassStyle.java` owns the radii, alpha,
  outline and elevation tokens; `GlassStyleCheck` keeps them in range.
- `MainActivity.buildUi()` adds the top bar as a `FrameLayout` overlay after the
  stage/body, so the photograph runs under the bar without a page row.
- The 2×2 readout table and source strip are floating glass cards; the panel is
  a glass sheet above a bottom tab dock (`CONTROLS_OPEN_DP` includes the 8 dp
  shell pad).
- On the ≥600 dp rail layout the sheet reserves the bar's height at the top of
  the control column so its heading never sits under the overlay.
- Device evidence: `prototypes/shots/prism-phone-light.png`,
  `prism-phone-dark.png`, `prism-wide.png`.
- Checks: `checkstyleGovernance`, `checkJvm` (including `GlassStyleCheck`),
  `lintDebug`, `testDebugUnitTest` (including the Prism dock order assertion).

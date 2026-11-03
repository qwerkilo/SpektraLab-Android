# Material 3 Console — implementation spec

Status: `device-audit complete with residual matrix`. The phone shell, themes,
rail, JPEG import, scan/compare and JPEG export pass on `25128PNA1C` (API 37,
400 dp). Ticket 11 fixed the 1.3× Readout/panel clipping; ticket 12 fixed the
brand facet staying dark after Dark → Light. RAW import, brand fallback,
TIFF/DNG export, TalkBack speech and a true 16 KB-page runtime were not
executable on that device. Evidence: `.scratch/m3-console/issues/10-device-audit.md`.
Tracker: GitHub Issues are disabled, so this spec
lives in the repository until the tracker is enabled. Decision record:
ADR-0004 (Material 3 as the Console design system). Vocabulary: `GLOSSARY.md`.
Map and tickets: `.scratch/m3-console/`.

## Problem Statement

The Console is a coherent instrument, but it is not an Android application in
the platform's own language. Its header, tabs, cards, sliders, sheets, and
state feedback are hand-built. They do not inherit Material 3 behaviour, do not
respond to the system's dynamic colour, and do not give an Android user the
touch, focus, and accessibility conventions they already know. The app also has
no dependency build, so using the real Material Components library is currently
impossible.

On a tablet or foldable, the phone tab row simply stretches. The same four
workspaces should become a navigation rail at ≥600 dp, with the selected state
shared across both forms. The type scale is ad hoc rather than an M3 scale, and
the control colours are a private palette rather than M3 color roles.

At the same time, the photograph must not become a Material surface. A dynamic
wallpaper tint around the image would change how a photographer judges colour,
and the spectral R/G/B must stay measured colours rather than scheme colours.

## Solution

Adopt Material 3 as the control system while keeping the Console's identity:

- A Gradle build with the real Material Components library, AppCompat, and M3
  DayNight themes.
- Dynamic colour for controls on Android 12+, with a brand-seed fallback on
  older devices.
- A fixed neutral stage and fixed spectral R/G/B, outside the dynamic scheme.
- M3 type roles applied to Inter and OPPOSans, not Roboto defaults.
- A Material top app bar with the wordmark, Theme mode, and Export.
- Secondary tabs on phones; a navigation rail at ≥600 dp; one selected state.
- M3 cards, chips, sliders, bottom sheets, and snackbars in the four panels.
- The spectral curve, Scan inspect, and compare rebuilt as M3-aware custom
  views inside Material surfaces, keeping their capabilities.
- A 16 KB-page installable APK for Android 15/16 devices.
- The existing Look, Frame, Render, and export semantics unchanged.

## User Stories

1. As an Android user, I want the editor to use the platform's Material 3 components, so that controls behave the way the rest of my phone does.
2. As an Android user, I want the app to follow my system light/dark setting on first launch, so that it does not open in the wrong theme.
3. As a photographer, I want to keep my explicit Theme mode choice, so that a system change does not override my darkroom preference.
4. As an Android 12+ user, I want control colours to follow my wallpaper, so that the app feels part of my device.
5. As a photographer, I want the photograph's surround to stay neutral, so that a wallpaper tint never changes my colour judgement.
6. As a photographer, I want the spectral R/G/B to stay measured colours, so that the curve remains a reading, not decoration.
7. As an Android 11 or older user, I want a branded fallback scheme, so that the app still looks intentional without dynamic colour.
8. As a photographer, I want the top app bar to carry the wordmark, Theme mode, and Export, so that the primary actions stay in one predictable place.
9. As a photographer, I want the active tab to be obvious through M3 state, so that I know which panel will change.
10. As a phone user, I want secondary tabs for Film, Optics, Scale, and Edge, so that I can reach a workspace with one thumb movement.
11. As a tablet or foldable user, I want a navigation rail at ≥600 dp, so that the wider window is used as a workspace rather than a stretched phone.
12. As a tablet user, I want tabs and the rail to share one selected state, so that rotating or resizing never loses my workspace.
13. As a foldable user, I want a resize across the rail breakpoint to preserve my Frame, Look, tab, and Scan state, so that unfolding does not reset my edit.
14. As a photographer, I want the film shelf to use M3 chips or cards, so that stock selection has platform-standard pressed, focused, and selected states.
15. As a photographer, I want the brand facet and the pinned active stock to survive the component swap, so that filtering never hides the stock I am using.
16. As a photographer, I want optics rows to use M3 sliders with a visible label, value, unit, Δ, and reset, so that I know exactly what each row changes.
17. As a photographer, I want each optics row to expose its real range to accessibility services, so that TalkBack announces the value I am adjusting.
18. As a photographer, I want the Scale and Edge panels to use M3 cards and switches, so that Gate and Rebate choices read as one system.
19. As a photographer, I want the paper and stock pickers in M3 bottom sheets, so that a long list does not become a full-screen context switch.
20. As a photographer, I want render and export failures in an M3 snackbar, so that feedback is visible without covering the photograph.
21. As a photographer, I want the spectral curve in an M3 surface with M3 labels, so that it belongs to the panel while remaining a precise plot.
22. As a photographer, I want the curve's plotter reveal to respect reduced motion, so that the plot appears at once when I ask the system to reduce animation.
23. As a photographer, I want Scan inspect to keep RGB and wavelength semantics, so that the readout stays a measurement rather than an M3 colour swatch.
24. As a photographer, I want the compare divider and hold gesture to keep their hit areas, so that the component migration does not make comparison harder.
25. As a photographer, I want every control to have a visible pressed, focused, and disabled state, so that I never tap something that looks active but is not.
26. As a TalkBack user, I want the M3 components to announce role, selected state, and range, so that the editor is operable without sight.
27. As a keyboard or switch-access user, I want focus order to follow the visual order, so that traversal does not jump between panels.
28. As a user with large text enabled, I want the M3 type roles to scale without clipping, so that labels and values remain readable.
29. As a user with reduced motion enabled, I want theme, tab, panel, and curve transitions to settle immediately, so that the editor does not animate against my setting.
30. As a photographer, I want a theme switch to preserve my loaded Frame and Look, so that changing the control scheme never costs a render.
31. As a photographer, I want the same Look to produce the same preview and export after the migration, so that visual changes do not alter the image.
32. As a photographer on Android 15/16, I want an installable 16 KB-page APK, so that the app launches without the compatibility warning.
33. As a maintainer, I want Gradle to be the primary build while the old scripts remain a pre-M3 fallback, so that a failed migration does not strand the shipped tree.
34. As a maintainer, I want the existing pure checks and ManifestPolicy to stay green through the migration, so that the security and palette invariants are not lost.
35. As a maintainer, I want the migration split into foundation, shell, tokens, panels, stage, and audit, so that each step is independently reviewable.
36. As a maintainer, I want the M3 mapping recorded in ADR-0004 and the glossary, so that future work does not re-litigate the control-versus-stage boundary.
37. As a maintainer, I want a device matrix for phone, tablet, foldable, both themes, dynamic colour, and brand fallback, so that component behaviour is verified where the JVM cannot see it.

## Implementation Decisions

- **Build.** Gradle 8.7 + AGP 8.2.2 is the primary build. Material 1.12,
  AppCompat, and AndroidX Core are the component dependencies. The wrapper is
  committed. The existing Termux and Windows scripts stay as the pre-M3
  fallback.
- **Themes.** `Theme.Material3.Light.NoActionBar` and
  `Theme.Material3.Dark.NoActionBar` replace the framework themes. Launch
  colours continue to mirror the Palette so the first frame does not flash.
- **Dynamic colour.** `DynamicColors.applyToActivitiesIfAvailable` runs in the
  Application. It supplies the M3 control scheme on API 31+; API 24–30 uses a
  brand seed. No wallpaper colour reaches the stage or the spectrum.
- **Fixed colours.** The stage keeps a neutral dark surround in both themes.
  The spectral red, green, and blue remain the measured plot colours from the
  Palette, not `primary`/`secondary`/`tertiary`.
- **Type.** M3 display/headline/title/body/label roles replace ad-hoc sizes.
  Inter Light, Inter Regular, and OPPOSans remain the families. Values use the
  display role; labels use the label role; Chinese and mixed strings use the
  Han family.
- **Shell.** A Material top app bar owns the wordmark, Theme mode, and Export.
  Phone widths use secondary tabs. Widths ≥600 dp use a navigation rail. Both
  reflect one `activeTab` value. `ConsoleLayout` remains the pure seam for the
  breakpoint and column widths. At the 600 dp floor the rail takes 80 dp and
  the control column its 300 dp minimum, so the Frame column is 220 dp; the
  spec records that consequence instead of carrying an unreachable 280 dp
  stage floor. Wider windows return the surplus to the Frame.
- **Panels.** Film chips/cards, Gate cards, optics sliders, paper pickers, and
  feedback migrate to Material components. The Look fields, defaults, ranges,
  and measured-value display do not change. Δ stays display-only.
- **Stage.** The spectral curve, Scan inspect, and compare keep their
  capabilities but are rewritten inside Material surfaces. State layers,
  labels, and accessibility follow M3; the plot geometry and the scan sample
  contract stay product-owned.
- **State.** Theme switching keeps the composition-root rebuild: capture the
  Frame, Look, selected tab, panel collapse, Scan fraction, and render state,
  rebuild, then restore. It must not recreate the Activity or the native
  session.
- **Exclusivity.** A custom view is used only where M3 has no equivalent. M3
  components are not reimplemented by hand to imitate their look.
- **16 KB pages.** The Gradle `align16k` task uses build-tools 36
  `zipalign -P 16` and re-signs the APK. The plain debug APK is not sufficient
  on Android 15/16.
- **SDK.** Gradle compiles and targets SDK 34 for this phase; platform 36 is
  not installed and AGP 8.2 does not support it. The manifest still declares
  target 36 for the legacy scripts, and Gradle overrides it. This is recorded
  as an interim build constraint.
- **Assets and native libraries.** Gradle stages them from a base APK
  (`-PbaseApk` override). A clean checkout without a donor APK cannot assemble
  a runnable package. The native engine itself remains out of tree.

## Testing Decisions

A good test observes an external contract: the component role and state a user
or accessibility service sees, the value a Look carries, the theme a window
uses, or the pixels an export produces. It does not assert a private helper, a
specific view class, or an animation frame.

- **Highest seam: the Gradle build and the device.** `./gradlew align16k`
  must produce an APK that installs, cold-starts, switches both themes, and
  opens both the phone shell and the rail. This is the only seam that can see
  Material component behaviour.
- **Pure JVM seam: M3 tokens.** A new `M3TokenCheck` owns the required color
  roles, the light/dark brand fallback, and contrast pairs. It must prove that
  the stage and spectral colours are not part of the dynamic scheme, and that
  the fallback roles are complete when dynamic colour is unavailable.
- **Pure JVM seam: responsive shell.** `ConsoleLayoutCheck` is extended to
  cover the rail breakpoint, rail width, and stage/panel widths at 600, 800,
  960, and 1200 dp. It remains Android-free.
- **Existing seams stay green.** `PaletteCheck` continues to guard the stage
  and spectrum contrast; `ThemeModeCheck` continues to guard preference
  round-trip; `ManifestPolicy` continues to guard permissions, the single
  exported launcher, cleartext, and backup. The other `*Check` classes and the
  stock-catalog generator check remain unchanged.
- **Device matrix.** `docs/testing/manual-device-checklist.md` is extended
  with M3 cases: dynamic on/off, rail at 600 dp, foldable resize, component
  pressed/focused/disabled states, TalkBack roles and ranges, large text,
  reduced motion, and the 16 KB APK.
- **Prior art.** The existing plain `*Check` classes are the prior art for
  pure seams. `check-manifest.py` is the prior art for a policy check that
  fails the build. The manual checklist is the prior art for behaviour that
  needs the device.

## Out of Scope

- Engine, Vulkan, tiling, JNI, Look semantics, and export encoding.
- A Compose rewrite.
- Rebuilding the native libraries for 16 KB ELF alignment; only the APK
  packaging and signing work is in scope.
- The web prototypes and the release announcement/version bump.
- Reworking the Frame or source-strip model.

## Further Notes

- Ticket 01 (Gradle + M3 foundation) is complete. Tickets 02–10 are the
  remaining frontier: tokens + phone shell, rail, film, optics, scale/edge,
  pickers, curve, scan/compare, and the device audit.
- GitHub Issues are disabled; the map and tickets live under
  `.scratch/m3-console/`.
- The migration deliberately does not change the image pipeline. If a Material
  component forces a state or layout change that would alter a Look, the
  component choice loses.
- The 16 KB APK task needs build-tools 36; the project otherwise builds with
  the SDK's default build-tools.

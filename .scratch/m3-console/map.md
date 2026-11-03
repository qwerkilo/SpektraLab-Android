# Wayfinder map: M3 Console

## Destination

A Material 3 Console that uses the real Material Components library (not a
lookalike), dynamic control colour with a fixed neutral stage, top tabs on
phones and a navigation rail at ≥600 dp, and M3 type/state tokens. The engine,
Look semantics, and the existing test seams stay intact.

## Notes

- Execution is in scope for this effort: tickets may land code on
  `feat/m3-console`, not only decisions.
- Read `GLOSSARY.md`, ADR-0001, ADR-0003, and ADR-0004 before touching the
  shell.
- Spec: `docs/specs/m3-console.md`.
- The app is Java + custom Views; Gradle and Material Components are now in
  place (ticket 01).
- Preserve the signature capabilities (spectral curve, scan inspect, compare);
  rewrite their implementation inside M3 containers.
- Every ticket ends with a device screenshot in both Theme modes and the
  relevant pure/Python checks.

## Decisions so far

- **M3 delivery**: real Material Components for Android via Gradle, not a
  hand-rolled token imitation.
- **Build**: Gradle becomes the primary build; `build-apk.sh` and the Windows
  helper remain as fallback until a Gradle APK is device-verified.
- **Colour**: dynamic controls/surfaces; the photographic stage and spectral
  R/G/B stay fixed so the surround never tints the image.
- **Navigation**: M3 tabs on phones, navigation rail at ≥600 dp.
- **Type**: M3 type scale roles with Inter / OPPOSans, not Roboto defaults.
- **Sequencing**: foundation → shell → panels → stage views → device audit.
- **Custom views**: keep the capabilities; rewrite the implementations as
  M3-aware views inside Material surfaces.
- **First ticket**: Gradle + Material 1.12 + DayNight/DynamicColors theme.
- **Foundation landed**: Gradle 8.7/AGP 8.2.2, Material 1.12, AppCompat, dynamic
  colours via `SpektraLabApp`, assets/libs staged from the base APK; Gradle APK
  installs and cold-starts. Both themes captured on device. `align16k` produces
  the Android 15/16 16 KB APK. Interim compile/target SDK 34.
- **Ticket 02 landed** (`85ee7e1`): M3 tokens, DynamicColors overlay, Material
  top app bar, secondary tabs, in-place theme restore. `checkJvm` 7,489,
  ManifestPolicy 26/26, `align16k` signed. Device screenshots were captured
  before the device dropped off the network; the merge gate could not reinstall.
- **Tickets 03–09 landed** through `6d09bcf`: rail, film chips/cards and
  FilmBrand.shelf, optics sliders, scale/edge cards and switches, bottom-sheet
  pickers with StockNames, SpectrumCurveView, and the scan/compare M3 controls.
  Final JVM gate is 8,583 checks; ManifestPolicy 26/26; the 16 KB APK is signed.
  Device verification is still blocked because `10.110.67.69:35443` is offline.
- **Ticket 11 large text (2026-11-03)**: fixed the 1.3× clipping with a 2×2
  Readout grid, stacked section heads, scrollable panels, and wrapping film
  metrics/paper row. Rechecked on the same device; 2.0× remains a known
  limitation.
- **Ticket 12 brand facet theme (2026-11-03)**: the brand filter chips kept
  their dark `ChipDrawable` after Dark → Light; explicit M3 colour state lists
  now resolve at build time. Rechecked on the device.
- **Implementation breakdown**: 02 tokens + phone shell, 03 rail, 04 film,
  05 optics, 06 scale/edge, 07 pickers, 08 curve, 09 scan/compare, 10 audit.
  Tickets 01–09 are merged on `feat/m3-spec`; only ticket 10 remains.
- **Ticket 10 audit (2026-11-03)**: device `25128PNA1C`, API 37, 400 dp,
  4 KB pages, dynamic colour. JVM 8,610 checks, `ManifestPolicy` 26/26 and
  the 16 KB APK gates pass. Phone shell in both themes, the 600 dp rail and
  two-column layout, the 599↔600 resize (Frame/stock/tab/Scan preserved),
  JPEG import, scan/compare and JPEG export pass. Close-out is blocked by
  Readout-strip clipping at `font_scale 1.3`; RAW import, brand fallback,
  TIFF/DNG export, TalkBack speech and a true 16 KB-page runtime are not
  executable on this device. See ticket 10 for the evidence list.

## Not yet specified

- Whether `MainActivity` splits into fragments/ViewModels or keeps one
  composition root during the migration.
- Test migration: JUnit/Robolectric vs keeping the plain `*Check` classes.

## Out of scope

- Engine, Vulkan, tiling, and JNI changes.
- A Compose rewrite.
- Web prototype changes.
- Release announcement/version bump.

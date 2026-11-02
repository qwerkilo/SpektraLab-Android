# Wayfinder map: M3 Console

## Destination

A Material 3 Console that uses the real Material Components library (not a
lookalike), dynamic control colour with a fixed neutral stage, top tabs on
phones and a navigation rail at ≥600 dp, and M3 type/state tokens. The engine,
Look semantics, and the existing test seams stay intact.

## Notes

- Execution is in scope for this effort: tickets may land code on
  `feat/m3-console`, not only decisions.
- Read `GLOSSARY.md`, ADR-0001, and ADR-0003 before touching the shell.
- The app is Java + custom Views; there is no Gradle build yet.
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
  installs and cold-starts. Interim compile/target SDK 34.

## Not yet specified

- Exact AGP / Material / AndroidX versions and whether platform 36 is
  installed or the Gradle build compiles against 34 initially.
- How the 30 MB assets and `arm64-v8a` native libraries enter a Gradle APK.
- Whether `MainActivity` splits into fragments/ViewModels or keeps one
  composition root during the migration.
- Dynamic-colour fallback for API 24–30 and the brand-colour seed.
- Which M3 components replace each current control (top app bar, tabs, cards,
  chips, sliders, sheets, snackbar).
- Test migration: JUnit/Robolectric vs keeping the plain `*Check` classes.

## Out of scope

- Engine, Vulkan, tiling, and JNI changes.
- A Compose rewrite.
- Web prototype changes.
- Release announcement/version bump.

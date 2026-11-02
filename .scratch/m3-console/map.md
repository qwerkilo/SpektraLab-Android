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
- **Implementation breakdown**: 02 tokens + phone shell, 03 rail, 04 film,
  05 optics, 06 scale/edge, 07 pickers, 08 curve, 09 scan/compare, 10 audit.
  Ticket 02 is the only frontier; 03–06 unlock after it; 07 waits on 04–06;
  08 waits on 04; 09 waits on 02; 10 waits on everything.

## Not yet specified

- Whether `MainActivity` splits into fragments/ViewModels or keeps one
  composition root during the migration.
- Test migration: JUnit/Robolectric vs keeping the plain `*Check` classes.

## Out of scope

- Engine, Vulkan, tiling, and JNI changes.
- A Compose rewrite.
- Web prototype changes.
- Release announcement/version bump.

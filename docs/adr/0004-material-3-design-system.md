# 0004 · Material 3 as the Console design system

Status: accepted (2026-11-02)

## Context

The Console was built as a programmatic Java UI with a role-based `Palette`.
It had no dependency build, no Material Components, and its own tabs, cards,
sliders, sheets, and state colours. That produced a coherent instrument look,
but it diverged from Android's interaction and accessibility conventions and
made every new control a bespoke implementation.

The requested direction is Material 3 (M3) as the UI system, not a visual
imitation. M3 has no component for the spectral curve, scan inspect, or the
compare divider, and the photographic stage must not take its colour from the
wallpaper.

## Decision

- **Use the real Material Components library.** Gradle 8.7 and AGP 8.2.2 are
  the primary build; Material 1.12, AppCompat, and AndroidX Core supply the
  components and themes.
- **Dynamic colour for controls, fixed colour for the photograph.** On API 31+
  `DynamicColors` supplies the control scheme. The stage keeps a neutral
  surround, and spectral R/G/B remain measured colours. API 24–30 uses a brand
  seed fallback.
- **M3 type scale with brand typefaces.** Type roles come from M3; Inter and
  OPPOSans remain the families, so the product keeps its voice.
- **Tabs on phones, a navigation rail at ≥600 dp.** Both drive the same four
  workspaces and the same selected state.
- **Panels migrate to M3 components.** Cards, chips, sliders, bottom sheets,
  and snackbars replace the bespoke equivalents. Look semantics and the
  measured-value display do not change.
- **Signature capabilities are rewritten, not deleted.** The spectral curve,
  scan inspect, and compare are custom views inside Material surfaces; their
  interactions and accessibility are rebuilt against M3 state layers.
- **Migration is phased.** Foundation → shell → tokens → panels → stage views
  → device audit. The engine, JNI, and Look model are untouched.
- **The legacy scripts remain a pre-M3 fallback.** They are not expected to
  build the M3 theme; the Gradle APK is the migration artifact.
- **16 KB page devices use `align16k`.** The ordinary debug APK is only 4 KB
  zip-aligned; the aligned task produces the installable APK.

## Consequences

- `MainActivity` may become an `AppCompatActivity` once a Material component
  needs it; the plain-JVM checks must keep compiling until the test migration.
- `Palette` remains the stage/spectrum source of truth even after M3 tokens
  land; it is no longer the source for control colours.
- Every theme switch still rebuilds the composition root and restores the
  Frame, Look, tab, and scan state; dynamic colour must not force an Activity
  recreation.
- Gradle stages the ~30 MB assets and `arm64-v8a` libraries from a base APK;
  a clean checkout without that donor cannot assemble a runnable APK.
- The migration temporarily targets SDK 34 because platform 36 and a matching
  AGP are not installed. This is a build constraint, not a product decision.

## Considered options

- **Imitating M3 with the existing Palette and custom views** — rejected: it
  would not provide the component behaviour, state layers, or accessibility
  that motivated the change.
- **Compose Material 3** — rejected for this effort: it requires a full UI
  rewrite and a new toolchain while the View hierarchy still carries the
  engine and import state.
- **Dynamic colour everywhere, including the stage** — rejected: a wallpaper
  tint around the photograph would compromise colour judgement.
- **Deleting the curve, scan, and compare** — rejected: they are the product's
  signature capabilities and have no M3 equivalent.

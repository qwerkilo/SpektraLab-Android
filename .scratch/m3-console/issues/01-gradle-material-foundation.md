# 01: Gradle + Material 3 foundation

**Type:** task
**Blocked by:** None (can start immediately)
**Status:** completed

**Resolution:** Gradle 8.7 + AGP 8.2.2 builds an installable APK. Material 1.12, AppCompat, and AndroidX Core resolve. The launch themes are Material3 DayNight; dynamic colour is applied by reflection in `SpektraLabApp` on API 31+. Assets and `arm64-v8a` libraries are staged from `app/build/SpektraLab-1.3.4-qa.apk`. `assembleDebug` succeeds, signs with the user keystore when `SPK_*` is set, installs on the device, and cold-starts without an AndroidRuntime crash. Both Theme modes were captured on device.
Android 15/16 16 KB pages: the plain debug APK triggered the compatibility dialog. `./gradlew align16k` (build-tools 36, `-P 16`) produces `app-debug-16k-signed.apk`; that installs and launches with no dialog.
Interim: Gradle targets SDK 34 (platform 36 is not installed); the manifest still says 36 and Gradle overrides it. Move to 36 when the platform/AGP pair is upgraded.

**What to build:** A Gradle/AGP build that produces an installable APK with
Material Components 1.12, an AppCompat/Material3 DayNight theme, and dynamic
colour applied on Android 12+ with a fixed brand fallback. The existing
scripts remain as fallback.

- [ ] Gradle wrapper, settings, root/app build files; compileSdk/targetSdk decision documented.
- [ ] Material Components + AppCompat dependencies resolve offline or from the configured repositories.
- [ ] `MainActivity` runs under `AppCompatActivity`; launch theme is `Theme.Material3.DayNight.NoActionBar`.
- [ ] DynamicColors applies on API 31+; API 24–30 uses the brand palette.
- [ ] Assets and `arm64-v8a` libraries are staged from a base APK or documented source.
- [ ] `assembleDebug` produces an APK; install and cold start pass on the device.
- [ ] Existing pure JVM checks and ManifestPolicy still pass.

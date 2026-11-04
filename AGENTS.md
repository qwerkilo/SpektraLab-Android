# SpektraLab for Android

Unofficial Android port of the SpektraLab film/paper simulation engine (Vulkan). RAW in, physically modelled film + paper out, on device.

Java UI + JNI over an **out-of-tree C++ engine**. Gradle is the primary build; the Termux scripts remain for the pre-M3 tree. Pure JVM checks run with `./gradlew checkJvm`; there is no instrumentation suite.

## Build: Gradle primary, Termux legacy

The Gradle wrapper is committed; see “Gradle / Material 3” below for the current path. The Termux scripts below are the legacy/pre-M3 fallback and assume the repo is at `$HOME/SpektraLab-apk`, the engine at `$HOME/SpektraLab-android-adreno/engine`, Termux prefix `/data/data/com.termux/files/usr`, SDK 36, NDK `28.2.13676358`.

Engine source: upstream `JamesQiu2005/SpektraLab` at `445e314`, then `git apply patches/engine-android.patch`.

Run in this order:

```bash
bash app/build/build-libs-raw.sh        # once; downloads LibRaw 0.22.2, adds HE/HE* sniff
bash app/build/build-engine.sh          # all engine TUs; pass names to rebuild a subset
bash app/build/build-native.sh          # both JNI units; links libspektrafilm_engine.so
bash app/build/build-libs.sh            # stages .so and rewrites DT_NEEDED
bash app/build/build-dex.sh             # javac + d8; regenerates app/build/sources.txt
python3 app/build/gen-stock-catalog.py  # only when profiles changed
bash build-apk.sh                       # aapt + zip + sign + check-libs; copies to /sdcard/Download
```

Never hand-copy `libspektrafilm_engine.so` into `app/build/apk/lib`; `build-libs.sh` renames `libjpeg.so.8` / `libz.so.1` to `libjpeg_8.so` / `libz_1.so` and rewrites every DT_NEEDED. Skipping it ships a library that cannot load (E-9000). `check-libs.py` fails `build-apk.sh` if the closure is broken.

`build-apk.sh` signs with `$HOME/SpektraLab-apk/debug.keystore` (gitignored). `app/build/apk/res` and most of `app/build/apk/assets` are local-only staging: no script creates them. `app/src/main/assets` is gitignored too (~30 MB of profiles and resources).

On Windows without Termux, `app/build/build-apk-windows.ps1` compiles, dexes with the R8 jar, reuses a base APK's assets and native libraries, then align-signs. Set `SPK_KEYSTORE`, `SPK_KS_PASS`, `SPK_KEY_PASS`, `SPK_KEY_ALIAS`; pass `-BaseApk` only when the newest `*-console.apk` is not the donor.

## Gradle / Material 3

`./gradlew assembleDebug` is the primary build for the M3 migration. It uses `JAVA_HOME` (CI sets JDK 17; local JDK 17 or 21 works), compiles and targets SDK 36, and stages `assets/` plus `arm64-v8a` libraries from `app/build/SpektraLab-1.4.4-qa.apk` (override with `-PbaseApk=<path>`). The donor's `stock_catalog.json` is deliberately excluded; `generateStockCatalog` rebuilds it from the staged profiles into `build/generated/stockAssets`, so `targetPrint` and the spectral series match the shipped profiles. Set the same `SPK_*` variables to sign with the release key; without them Gradle uses its debug key and the APK will not update an installed release build. For Android 15/16 devices with 16 KB pages, run `./gradlew align16k` (needs build-tools 36) and install `app/build/outputs/apk/debug/app-debug-16k-signed.apk`; the plain debug APK is only 4 KB zip-aligned. Never run `./gradlew clean`: `app/build/` holds tracked build scripts (`gen-stock-catalog.py`, `check-*.py`, the Windows/Termux helpers), and `clean` deletes them. Use `--rerun-tasks`, or remove only generated outputs. The legacy scripts remain the fallback for the pre-M3 tree.

## Upstreams

- **Engine**: `JamesQiu2005/SpektraLab`, branch `assist/vulkan-engine-1.3.4` at `445e314`. The engine is out of tree; sync by reapplying `patches/engine-android.patch` to the new baseline, never by merging the desktop app.
- **Android app**: `SAINTTaiYi/SpektraLab-Android`, branch `master` at `1.4.4` (`9875785`). The fork's `origin` is `qwerkilo/SpektraLab-Android`; pushing there does not update the user-facing channel. The 1.4.4 native/JNI core is merged; its new Console pages, canvas pinch, and undo are a follow-up port (issue #3). Merge upstream syncs with a **merge commit**, never squash: squash drops the upstream parent and GitHub reports the fork behind even when the trees match.
- On Windows, build-tools 34 `d8` crashes on `MainActivity$1.class` (`NullPointerException`). Use the R8 jar in `android-sdk/cmdline-tools/latest/lib/r8.jar` with JDK 17 instead.

## Where the code lives

- `MainActivity.java` — ~8.2k lines, programmatic `View` UI in one file. No XML layouts; the M3 theme, color roles, and type attrs live in `app/src/main/res/values/*` (`themes.xml`, `colors.xml`, `attrs.xml`), with the rail menu in `app/src/main/res/menu/console_nav.xml` and its icons in `app/src/main/res/drawable/`.
- `FrameLookSession.java` — the live per-Frame `Look` and undo history. The Activity holds only the current session; `look()` is read-only and edits go through `edit`/`record`. Film and print are already session-backed; Scale/Edge still use transitional fields, so do not add another copy of a Look value.
- `SpektraLabEngine.java` — JNI facade. `prepare(libDir)` loads bundled `.so` by absolute path before the engine; there is intentionally no `static {}` loader, so a load failure keeps its cause. One native result buffer, serialized by `RENDER_LOCK`.
- `spektralab_jni.cpp` — session, render, JPEG/TIFF/DNG export. `spektralab_raw_jni.cpp` — LibRaw decode.
- The engine itself is not in this repo; only `patches/engine-android.patch` and `docs/tiling.md` are.

## Traps

- `spk_result.row_stride_px` is in **pixels**, 4 uint16 per pixel: index `rgba16 + y * stride * 4`.
- Dispatches are capped at 65,535 workgroups × 256 threads; the engine chunks with a push-constant base. Per-plane binding range (`maxStorageBufferRange`) is still the hard import wall — `DeviceProbe` derives the cap and canary-tests it. The app caps imports at `HARD_PIXEL_CAP` 42 MP and `IMPORT_LONG_EDGE` 8000; the 16.7 MP dispatch ceiling is handled by the 1.4.4 banded renderer, not by the import cap.
- `build-engine.sh <unit>...` is incremental, but a header change forces a full rebuild (vtable/ABI skew). Keep that stamp.
- Both JNI units are recompiled every time on purpose; a stale `spektralab_raw_jni.o` links fine and breaks the first RAW open.
- `app/build/sources.txt` is generated by `build-dex.sh`; don't edit it.
- Reversal/slide film is allowed: `scan_film` must ride the stock delta, not the slider delta. The Print stock stays stored for a later negative but is unused while a slide is selected; preview and export must send the same value.
- `[E-xxxx]` codes in `SpkError.java` are user-facing: E-1001 binding range, E-1108 HE/HE*, E-9000 native load, E-2001/2002 update. Keep the number when changing the text.

## Release

- Version is duplicated in `AndroidManifest.xml` (`versionCode` + `versionName`), `app/build.gradle` (`defaultConfig`), and the channel files; `VersionCheck` fails `checkJvm` if they disagree. Current stable is `156` / `1.4.4-m3a`.
- Two announcement files: `preview.json` (pre-release build, channel `preview`) and `update.json` (last stable build; 1.3.4 clients read it, and it currently carries 156 / `1.4.4-m3a`). `UpdateCheck` compares version **and** channel, reads only the fork's endpoints, and a stable build never reads `preview.json`; `ReleaseUrl` allowlists `spektralab.workshop.skin`, the update host, and the fork's GitHub release paths.
- A stable announcement is gated by `docs/known-issues.md`. KI-1 (>16.7 MP rebate bottom print) is fixed by the 1.4.4 engine and recorded as `Release gate: accepted`; `1.4.4-m3a` was verified on device with a 4704×4345 export. Do not reopen the gate without a new reproduction or an explicit ADR-0005 reversal.
- `[CHANNEL=preview|stable] CF_TOKEN=... ./scripts/announce.sh <code> <name> <apk|-> "note"...` derives the channel from the version name when unset, writes/commits/pushes `preview.json` or `update.json`, purges jsDelivr, uploads a stable APK to KV (24 MiB limit; a preview APK goes out only as a GitHub Release asset), and creates a GitHub Release.
- **Identity split**: `announce.sh` and the GitHub Release target `qwerkilo/SpektraLab-Android`; the upstream `SAINTTaiYi` repository is the source reference, not this fork's release channel. `UpdateCheck` reads the fork's `spektralab.workshop.skin` and `qwerkilo` endpoints. Pushing upstream does not update the channel users read. Confirm the target before announcing.
- Default branch `master`; jsDelivr and raw.githubusercontent both read `@master`.

## CodeGraph

This checkout has a `.codegraph/` index. Prefer `codegraph explore "<symbols or question>"` (or the MCP `codegraph_explore`) over a grep/read loop for code questions. The database is local and gitignored; on a fresh clone run `codegraph init -y`. The `post-commit` and `post-merge` hooks run `codegraph sync --quiet` when the CLI is installed.

## Verify

No JUnit or instrumentation suite. Pure JVM checks live in `app/src/test/java/app/spektralab/*Check.java` (Palette, M3Tokens, ThemeMode, SpectrumSamples, SpectrumCurve, OpticsDelta, GateScale, ScanSample, ConsoleLayout, FilmBrand, StockNames, PrintTarget, SafeFileName, ReleaseUrl, UpdateGate, SpkError, VersionCheck, ViewAdjust, Composition, Look, LookRestatement, LookHistory, DateBack, RebateControls, Icon, FrameLookSession). Run them with `./gradlew checkJvm`; it compiles the checks against the app's real debug classpath and runs each one from the repository root, so `M3TokenCheck` can read `res/values/colors.xml` and `themes.xml`. They do not need a device. Plain `javac -cp android.jar` no longer compiles the main sources because `MainActivity` imports AppCompat and Material; the legacy Termux scripts are the pre-M3 fallback and do not build this tree. `python app/build/check-manifest.py` pins permissions, exports, cleartext, and backup; `python app/build/check-apk.py <apk>` re-checks the merged APK (target SDK, permissions, backup, cleartext); `python app/build/test-gen-stock-catalog.py` covers the profile generator. For Android 15/16 16 KB pages, `./gradlew align16k` and install `app-debug-16k-signed.apk`. Then install on the device and watch logcat tags `SpektraLabJNI` and `SpektraLabRAW`. On the device, `DebugSink` POSTs diagnostics to `127.0.0.1:8770` (Termux-side sink). Driver telemetry goes to `report.workshop.skin`. Manual device matrix: `docs/testing/manual-device-checklist.md`. When a new `*Check.java` is added, run `./gradlew checkJvm --rerun-tasks`: Gradle can otherwise report the task up-to-date and silently skip the new check.

## Device: DroidMind first

Use the global `droidmind` MCP for Android device work. Do not start with raw `adb`.

- `android-device` — list/connect/properties; call `list_devices` first and use the serial it returns. Wi-Fi ports change between sessions.
- `android-screenshot`, `android-ui`, `android-app`, `android-file`, `android-log`, `android-diag`, `android-shell` — prefer these over ad-hoc `adb shell`; use `android-shell` only for commands without a dedicated tool.
- The MCP launches `uvx --python 3.13 --from git+https://github.com/hyperb1iss/droidmind --with "mcp<2" droidmind --transport stdio --adb-path F:\android-sdk\platform-tools\adb.exe`. Keep the `mcp<2` pin (DroidMind 1.30 uses FastMCP) and the explicit ADB path.
- Fall back to `F:\android-sdk\platform-tools\adb.exe` only when the MCP is unavailable.
- Installing, clearing data, uninstalling, or rebooting is state-changing: name the target serial explicitly and only do it when the task calls for it.
- For a wide-window check, change `wm size` (for example `1800x2608`) and restore it; `wm density` recreates the Activity and clears the in-memory Frames, so do not use it to claim state preservation.

When the Termux engine toolchain is unavailable, a device build can replace `classes.dex` inside the shipped APK and re-sign it; the native libraries and assets stay from that APK. Do not hand-copy `libspektrafilm_engine.so`.

GitHub Actions (`.github/workflows/android.yml`) runs `checkJvm`, lint, the manifest/APK gates, and builds a signed 16 KB APK on `windows-latest`. It downloads the donor APK from upstream release `v1.4.4`; that download replaces the local `app/build/SpektraLab-1.4.4-qa.apk`.

## Commits

Conventional commits, one concern each: `fix(native):`, `feat:`, `chore(update):`,
`release:`. Integration merges use `merge: <topic>` as already recorded in the
history; merge commits are exempt from the conventional prefix because their
subject names the joined work, not a new concern.

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues (`qwerkilo/SpektraLab-Android`), managed with the `gh` CLI. See `docs/agents/issue-tracker.md`. Issues are enabled; publish new tickets with `gh issue create`.

### Triage labels

Five canonical roles, default label strings. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Pre-commit hook

Enable once per clone: `git config core.hooksPath .githooks`.
The hook syntax-checks staged `.sh` files with `bash -n`, staged `.py` files with `ast.parse`, rejects staged text that is not valid UTF-8, and runs `git diff --cached --check`.
It does not run the Android build. Edit UTF-8 docs with the editor tool or Python (`newline='\n'`); Windows PowerShell 5 `Set-Content -Encoding utf8` writes a BOM and has corrupted non-ASCII text.

## Git workflow

Never commit directly to `master`. Create a feature branch, verify the change, then merge into `master`. `master` is protected: the `build` Android check is required, force pushes and deletions are blocked, and admins are included. Land changes through a pull request.

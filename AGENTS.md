# SpektraLab for Android

Unofficial Android port of the SpektraLab film/paper simulation engine (Vulkan). Java UI + JNI over an **out-of-tree C++ engine**. Gradle is the primary build; the Termux scripts are the legacy path. Pure JVM checks run with `checkJvm`; Activity wiring uses JUnit 4 + Robolectric via `testDebugUnitTest`. There is no instrumentation suite.

## Build (Gradle)

On this Windows checkout use `.\gradlew.bat`; the wrapper is committed. Gradle needs `local.properties` with `sdk.dir=F:\\android-sdk`, JDK 17 or 21, SDK 36.

- `assembleDebug` stages `assets/` and `arm64-v8a` libraries from a donor APK, default `app/build/SpektraLab-1.6.0e-qa.apk`; override with `-PbaseApk=<path>`.
- The donor's `stock_catalog.json` is excluded on purpose. `generateStockCatalog` rebuilds it from the staged profiles, so `targetPrint` and spectral arrays match the profiles. A donor catalogue without `targetPrint` silently disables automatic paper selection.
- Sign with `SPK_KEYSTORE`, `SPK_KS_PASS`, `SPK_KEY_PASS`, `SPK_KEY_ALIAS`. Without them Gradle uses a debug key that cannot update an installed release build.
- For Android 15/16 (16 KB pages): run `.\gradlew.bat align16k` and install `app/build/outputs/apk/debug/app-debug-16k-signed.apk`; the plain debug APK is only 4 KB zip-aligned.
- **Never run `.\gradlew.bat clean`.** `app/build/` holds tracked scripts (`gen-stock-catalog.py`, `check-*.py`, build helpers) and `clean` deletes them. Use `--rerun-tasks`, or delete only generated outputs.

Legacy Termux build, only when rebuilding native code: `build-libs-raw.sh` (once) → `build-engine.sh` → `build-native.sh` → `build-libs.sh`. `build-libs.sh` renames `libjpeg.so.8` / `libz.so.1` and rewrites every `DT_NEEDED`; hand-copying `libspektrafilm_engine.so` ships a library that cannot load (E-9000).

## Verify

```powershell
.\gradlew.bat checkstyleGovernance checkJvm lintDebug
.\gradlew.bat testDebugUnitTest "-PbaseApk=<donor.apk>"
python app/build/check-manifest.py
python app/build/test-gen-stock-catalog.py
python app/build/check-apk.py <apk>    # after assemble/align16k
```

- `checkJvm` compiles the `*Check.java` programs against the real debug classpath and runs each one from the repo root. A new check needs `checkJvm --rerun-tasks`, or Gradle may report the task up to date and skip it.
- `testDebugUnitTest` runs the JUnit 4 + Robolectric tests in `src/test/java` (currently `PanelRowsTest`, SDK 35). AGP's unit-test compiler excludes `*Check.java`; those desktop-style programs stay on `checkJvm`.
- `checkstyleGovernance` enforces the Alibaba-style subset on `core/**`, `model/**`, `ui/**` and `MainActivity`. Do not exclude a file to make a change pass; move code to its owning package.
- `check-manifest.py` and `check-apk.py` pin target SDK, permissions, exports, cleartext and backup.
- CI runs on PR and `workflow_dispatch` only. `master` is protected by the PR `build` check, so a merge does not re-run the whole suite; dispatch manually to verify `master`. Gradle and the donor APK are cached, and a newer push to the same PR cancels the older run.
- Device checks: install, then watch logcat tags `SpektraLabJNI` and `SpektraLabRAW`. `DebugSink` posts diagnostics to `127.0.0.1:8770`; driver telemetry goes to `report.workshop.skin`. Manual matrix: `docs/testing/manual-device-checklist.md`.

## Code map

- `MainActivity.java` — large single-file composition root and programmatic `View` UI. No XML layouts; use CodeGraph before editing. M3 theme, color roles and type attrs live in `app/src/main/res/values/*`; the rail menu is `res/menu/console_nav.xml`.
- `FrameLookSession.java` — the live per-Frame `Look` plus undo. The Activity holds only the current session; `look()` is read-only, edits go through `edit`/`record`. Never add a second copy of a Look value.
- `RenderSnapshot.java` — one UI-thread snapshot of the Look, engine JSON, stock, long edge and view-adjust. Preview, export and RAW import build it once; workers must not re-read live state. Preview and export share the snapshot; export forces sRGB.
- `BuiltInLook.java` — six shipped film/paper looks authored as **sparse** overlays. Applying keeps every field the look does not name, including composition, crop, date and view.
- `PresetAmount.java` — the 0–1 draft blend used by the built-in/preset strength sheet. Numbers and equal-length numeric arrays interpolate; booleans, strings and seeds/turns switch at 50 %.
- `StockCatalogue.java` — the single store for film/print entries. The Activity exposes read-only views; do not add a parallel list.
- `core/SessionDocument.java` — the current source URI, Look JSON, tab, Scan state and Original/developed display state; written atomically under `noBackupFilesDir` and restored only when the source is still readable.
- `ui/GlassStyle.java` — Prism Table glass radii, alpha, outline and elevation tokens, guarded by `GlassStyleCheck`.
- `core/ExportDigest.java` — length + SHA-256 verification for a gallery export.
- `ui/widget/` — extracted custom views (`SpectralRail`, `ToolGlyph`, `ArrowView`, `GuideView`, `EdgeBackdrop`, `SnackbarHost`, `CompareView`, `ScanOverlayView`, `SpectrumCurveView`, `HistogramView`). New views go here. See “Material 3 components” before adding one.
- `core/` — framework-free rules and format helpers (`Metering`, `HueCorrection`, `Histogram`, `LivePreviewGate`, `ScanProfile`, …). `model/` — immutable records (`FilmStock`, `Frame`). `ui/` — presentation state (`UiState`, `ThemeMode`). The root package keeps the composition root and the remaining domain seams (`Look`, `GateScale`, `OpticsDelta`, `Palette`, `M3Tokens`, `UpdateGate`, `SpkError`).
- `SpektraLabEngine.java` — JNI facade; loads bundled `.so` by absolute path, no `static {}` loader. `spektralab_jni.cpp` — session/render/export; `spektralab_raw_jni.cpp` — LibRaw decode. The engine is not in this repo; only `patches/engine-android.patch` and `docs/tiling.md` are.

## Material 3 components

Prefer Material Components over framework widgets for every control and chrome surface: `MaterialToolbar` + `AppBarLayout`, `TabLayout`, `NavigationRailView` / `NavigationBarView`, `MaterialButton`, `MaterialCardView`, `Chip` / `ChipGroup`, `Slider`, `MaterialSwitch`, `MaterialRadioButton`, `MaterialAlertDialogBuilder`, `BottomSheetDialog`, `Snackbar`, `LinearProgressIndicator`, and `TextInputLayout` / `TextInputEditText`. Style text through `m3Label` / `mixedM3Label` / `applyTypeRole`, and colours through `m3Role` / `m3Primary` / `Palette.Role`; never hardcode a colour or text size.

- A raw `TextView` is only a typographic surface. Free-text dialogs use `TextInputLayout` + `TextInputEditText`; do not add a framework `EditText`. `M3ComponentCheck` fails `checkJvm` when `MainActivity` constructs a framework interactive widget.
- Custom Views in `ui/widget` are only for canvas or data visuals with no M3 equivalent. A new interactive control needs an M3 component first, a 48 dp touch target, and a TalkBack role/state description.
- Every screen must work in both themes, with dynamic colour, at 390 dp and at the ≥600 dp rail; M3 defaults must not be overridden with fixed colours.

## Traps

- `spk_result.row_stride_px` is in **pixels**, 4 uint16 per pixel: `rgba16 + y * stride * 4`.
- Imports are capped at 42 MP / 8000 long edge. The 16.7 MP dispatch ceiling is handled by the banded renderer in the 1.6.0e engine, not by the import cap.
- `build-engine.sh <unit>...` is incremental, but a header change needs a full rebuild; otherwise vtable/ABI skew. Both JNI units are recompiled every build on purpose; a stale `spektralab_raw_jni.o` links and breaks the first RAW open.
- `app/build/sources.txt` is generated by `build-dex.sh`; do not edit it.
- Reversal/slide film: `scan_film` must ride the **stock delta**, not the slider delta. The Print stock stays stored but unused while a slide is selected; preview and export must send the same value.
- `[E-xxxx]` codes in `SpkError.java` are user-facing. Keep the number when changing the text: E-1001 binding range, E-1108 HE/HE*, E-9000 native load, E-2001/2002 update.
- `Bitmap#setColorSpace` is API 29, not 26. Guard it with `SDK_INT >= 29`; `lintDebug` fails the build otherwise. `ColorSpace.get` alone is API 26.
- A one-line policy added to an 80-line method trips `MethodLength`. Put the reset in the callee (for example `openStillSession`) instead of growing the caller, and do not spend its blank-line budget.
- Restored sessions need a persistable URI grant. Add the read/persistable flags to every `ACTION_OPEN_DOCUMENT` path; `GET_CONTENT` and the Photo Picker may not survive a process death.
- A gallery export is published only after length + SHA-256 readback. Any copy, truncation or digest failure must delete the `IS_PENDING` row; do not update it to 0 in a catch.
- GitHub compares **ancestry**, not trees. Upstream syncs must be merge commits, never squash, or the fork shows as behind even when files match.

## Device: DroidMind first

Use the global `droidmind` MCP; do not start with raw `adb`.

- Call `android-device` `list_devices` first; Wi-Fi ports change, so use the returned serial.
- `android-ui` taps/swipes, `android-screenshot`, `android-app`, `android-file`, `android-log`, `android-diag` cover most work. Use `android-shell` only for commands with no dedicated tool.
- Config lives in `C:\Users\qwerkilo\.config\opencode\opencode.json`. It launches the local venv `C:\Users\qwerkilo\AppData\Local\Temp\opencode\droidmind-venv\Scripts\droidmind.exe --transport stdio --adb-path F:\android-sdk\platform-tools\adb.exe` with `mcp[cli]<2`; `uvx` fails when PyPI/GitHub TLS is flaky. If tools are missing, run `opencode mcp list`, then `opencode service restart`.
- Fall back to `F:\android-sdk\platform-tools\adb.exe` only when the MCP is unavailable.
- Installing, clearing data, uninstalling and rebooting are state-changing: name the target serial explicitly.
- For a wide-window check use `wm size 1800x2608` and restore it. Never use `wm density`: it recreates the Activity and clears in-memory Frames, so it cannot prove state preservation.
- On the 1200×2608 test phone the chat screenshot is shown at about 943×2048; multiply by 1.273 before `input tap`. The film panel's vertical order is 2×2 readout table → section head (预设 / 全部胶片 / 重置当前页) → brand filter row → film shelf → spectrum card. Tap the shelf row for a stock; the brand row sits directly above it.
- MIUI `GestureStub` / `AntiMistakeTouch` and other apps' full-screen windows can swallow taps over the app; an ignored tap is not proof of an app bug.

## Upstreams

- **Engine**: `JamesQiu2005/SpektraLab`, branch `assist/vulkan-engine-1.3.4` at `445e314`. Sync by reapplying `patches/engine-android.patch`; never merge the desktop app.
- **Android app**: `SAINTTaiYi/SpektraLab-Android` at `1.6.0e` (`08183cd`). The fork's `origin` is `qwerkilo/SpektraLab-Android`; pushing there does not update the upstream channel. The 1.6.0e native core is merged; live preview, batch import/export, metering, hue, histogram and preview gamut are in the M3 console. Upstream 58/77 MP controls remain the follow-up M3 port.
- On Windows, build-tools 34 `d8` crashes on `MainActivity$1.class`. Use the R8 jar in `android-sdk/cmdline-tools/latest/lib/r8.jar` with JDK 17.

## Release

- Version is duplicated in `AndroidManifest.xml`, `app/build.gradle` and the channel files; `VersionCheck` fails `checkJvm` on a mismatch. Current stable: **156 / `1.4.4-m3a`** (`update.json`). Current preview: **326 / `1.6.0e-m3`** (`preview.json`).
- `UpdateCheck` compares version **and** channel, and reads only the fork's endpoints. `ReleaseUrl` allowlists `spektralab.workshop.skin`, the update host, and the fork's GitHub release paths.
- Stable is gated by `docs/known-issues.md`. KI-1 (>16.7 MP rebate bottom print) is fixed in the 1.4.4 engine and recorded as `Release gate: accepted`; do not reopen it without a new reproduction.
- `[CHANNEL=preview|stable] CF_TOKEN=... ./scripts/announce.sh <code> <name> <apk|-> "note"...` writes and pushes the channel file, purges jsDelivr, uploads a stable APK to KV (24 MiB limit), and creates a GitHub Release. It targets `qwerkilo/SpektraLab-Android`; confirm the identity before announcing.
- Default branch `master`; jsDelivr and raw.githubusercontent both read `@master`.

## Git workflow

- Never commit directly to `master`. Branch, verify, push, open a PR, wait for the required `build` check, then squash-merge.
- `master` is protected: required `build`, up-to-date branch, no force-push or deletion, admins included.
- Conventional commits with one concern: `fix(native):`, `feat:`, `chore(update):`, `release:`. Merge commits use `merge: <topic>` and are exempt.
- Enable the hook once per clone: `git config core.hooksPath .githooks`. It checks staged `.sh` with `bash -n`, staged `.py` with `ast.parse`, rejects non-UTF-8 text, runs `git diff --cached --check`, and blocks commits on `master`/`main` unless `ALLOW_MASTER_COMMIT=1`.
- Edit UTF-8 docs with the editor or Python (`newline='\n'`). Windows PowerShell 5 `Set-Content -Encoding utf8` writes a BOM and has corrupted Chinese text.

## CodeGraph

`.codegraph/` is present and gitignored. Prefer `codegraph explore "<symbols or question>"` (or MCP `codegraph_explore`) over grep/read loops. `post-commit` and `post-merge` run `codegraph sync --quiet` when the CLI is installed; on a fresh clone run `codegraph init -y`.

## Agent docs

- Issue tracker: `docs/agents/issue-tracker.md` (GitHub Issues in `qwerkilo/SpektraLab-Android`, `gh` CLI).
- Triage labels: `docs/agents/triage-labels.md`.
- Domain docs: `GLOSSARY.md` and `docs/adr/`; see `docs/agents/domain.md`.
- Code style and layering: `docs/agents/code-style.md`.
- Current borrow spec: `docs/specs/spektrafilm-borrow.md`; decisions in `docs/adr/0009-spektrafilm-app-layer-borrow.md`.

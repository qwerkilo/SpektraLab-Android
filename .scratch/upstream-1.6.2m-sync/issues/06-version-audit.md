# 06: Version, docs and device audit

**Type:** task
**Blocked by:** 05
**Status:** completed

**Resolution:** preview is 385 / `1.6.2m-m3` in the manifest, Gradle,
`VersionCheck` and `preview.json`; `update.json` stays 156. `checkstyleGovernance`,
`checkJvm` (including the three new checks), `lintDebug`, `testDebugUnitTest`
and `align16k` + `ApkPolicy` 15/15 pass.

Device on `10.110.67.69:43683` (1200×2608, Android 17): fresh launch restored
frame-a on 已恢复 · 原图 with its 800T Look; imported frame-b as an empty Look;
switching back to frame-a returned to 原图 with 800T still shown; JPEG export
reported `780×860 · 0.4 MB · 渲染 0.096 秒` and `logcat -s SpektraLabExport`
printed `canvas NCC 0.986`, so the self-check ran and passed. Evidence in
`prototypes/shots/u162/`. The `libz_1.so` 4 KB ELF warning still appears at
launch; it predates this sync and is recorded.

**What to build:** Close the sync.

- Bump preview to **385 / `1.6.2m-m3`** in the manifest, Gradle, `VersionCheck`
  and `preview.json`; leave `update.json` at 156.
- Update `AGENTS.md` (upstream commit, version, new seams), the test matrix and
  the manual checklist.
- Device audit: theme switch, frame switch, scan, crop 1:1, portrait rebate,
  batch export, export self-check, 16 KB install.

**Acceptance**

- [ ] `VersionCheck` green; preview and stable channels agree with the docs.
- [ ] Device audit screenshots saved under `prototypes/shots/upstream-1.6.2m/`.
- [ ] PR body lists the ported fixes and the deferred upstream UI.

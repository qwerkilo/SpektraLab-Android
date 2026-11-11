# 06: Version, docs and device audit

**Type:** task
**Blocked by:** 05
**Status:** ready

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

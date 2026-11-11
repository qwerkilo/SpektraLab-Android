# 02: Per-frame Original and render epoch

**Type:** task
**Blocked by:** 01
**Status:** ready

**What to build:** A Frame remembers whether it was left on 原图, and a render
that lost its canvas cannot leave RENDERING behind.

- Add `Frame.original`; reopen uses it, and a legacy frame without the flag opens
  on its print.
- Add `renderEpoch`; bump it on import/reset/frame replace. A stale callback
  skips painting and calls `clearBusyPlate`.
- Store the flag in the session document; do not keep a global Original state.

**TDD seams**

- Pure JVM `FrameOriginalCheck`: flag round-trip, legacy default.
- Robolectric: chip returns to 原图 after a frame switch.
- Device: switch frames during a render; no stuck RENDERING, no stale bitmap.

**Acceptance**

- [ ] `checkJvm`, `testDebugUnitTest`, `checkstyleGovernance` green.
- [ ] Device screenshot of the 原图 chip restored.

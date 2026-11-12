# Decomposition review

Review of `master..HEAD` on `feat/upstream-1.6.2m-sync`, two axes
(behavior drift + standards/spec). Gates at review time: checkstyleGovernance,
checkJvm, lintDebug, testDebugUnitTest — all green, 21 Robolectric tests.

## Fixed

1. **Stage readouts under the floating bar.** `StagePanel.TOP_CLEARANCE_DP`
   had been retyped as 52 instead of the Activity's 78. Restored 78; the
   `未选择照片 / NO FRAME` and `PRINT EXPOSURE` rows now start below the bar
   (device `10.110.67.69:43657`).
2. **Optics typefaces.** `lockNote` and `range` restored to `tfHan`, `delta`
   to `tfDisplay`.
3. **Scan chip source state.** `StagePanel.Host.sourcePresent()` is now read on
   every `syncScan()`, so a RAW import cannot leave the chip hidden until the
   next rebuild.
4. **Dead seams.** Removed `OpticsPanel.Host.lockReason()`, the unused
   `opticsStrip` field/assignment, and `EdgeHomePanel`'s stored `head`.
5. **Docs.** ADR 0011 and the spec now name `ScalePanel` + `EdgeHomePanel` +
   `EdgePagesPanel`; the compose sheet is recorded as a later slice.

## Accepted, not changed

- `EdgeLookAccess` is a typed view of the same live `Look`, not a copy. It
  exists because `Look`'s edge fields are package-private. Delete it when those
  fields gain public accessors.
- The stage status-line max width uses `stageWidth - 96 dp` rather than 60 % of
  the window. Same result at 390 dp; check at ≥600 dp on device.
- `EdgePagesPanel` mutates the live Look directly, matching the pre-extraction
  Edge pages; it does not introduce a new `FrameLookSession` bypass.

## Still device-only

Film shelf scroll after theme rebuild/frame switch, one-render-per-gesture
counts, scan overlay fraction after frame switch, histogram/Compare, Scale card
reveal, Edge row heights/dimming/dialogs at 390 and 600 dp, TalkBack traversal,
RAW import, export self-check.

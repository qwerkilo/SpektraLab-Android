# 03: Activity wiring and device pass

**Type:** task
**Blocked by:** 02
**Status:** ready

**What to build:** delete the old export helpers from `MainActivity`, route
single and batch exports through `ExportRunner`, and run the device matrix.

## Delete when unused

- `renderExportFile`, `renderExportAttempts`, `finishExport`
- `exportMatchesCanvas`, `applyExportResult`, `applyExportFailure`
- `exportImage(format, tier, onDone)` worker body (keep the UI entry)
- export temp/diagnostics helpers that moved

## Keep

- `startBatchExport` / `runBatchExport` / `finishBatchExport` (03 only moves
  them if the runner owns the loop; otherwise leave the loop here)
- `writeExportToGallery` adapter or its moved form
- `RenderSnapshot`, `Look`, `FrameLookSession`, render mutex

## Device matrix

- JPEG preview and full size; TIFF; DNG
- one forced NCC retry (log `SpektraLabExport` canvas NCC)
- batch: two frames, one film stock → one file, summary says `1 张成功，1 张跳过`
- dark theme rebuild keeps the Frame and export entry

## Acceptance

- `checkstyleGovernance`, `checkJvm`, `lintDebug`, `testDebugUnitTest` green.
- `align16k` + `ApkPolicy` green.
- `MainActivity` line count recorded before/after.

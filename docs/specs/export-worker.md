# Export worker

**Status:** ready-for-agent
**Decision:** ADR 0011 (MainActivity decomposition), ticket 06 follow-on
**Base:** `master` at `dec2ff7`

## Why

The export cluster is the largest non-UI block left in `MainActivity`
(about 680 lines): temp file, render attempts, canvas self-check, gallery
publish, batch reopen loop and the completion/status policy. Ticket 06 moved
the sheets, the picker and the plan; this spec moves the worker.

The engine stays out of scope. This is an app-layer extraction with the same
JNI calls and the same output bytes.

## Stories

1. A single export renders, writes one file and reports its size and render
   time, exactly as today.
2. A JPEG whose canvas NCC is below the threshold retries once, then reports
   `suspect` instead of silently accepting a corrupt file.
3. TIFF and DNG skip the NCC check: the bitmap comparator only reads JPEG.
4. A second `suspect` result is reported, not retried forever.
5. A failure deletes the temp file and reports the original engine message.
6. A batch export walks only the eligible frames, reopens each one, exports it,
   closes the session, and restores the starting frame.
7. The batch completion message is published after the reopen step; the
   restore path must not overwrite it.
8. Frames without a film stock are skipped and counted separately from
   failures.
9. Back during a batch stops after the current frame; no new frame starts.
10. The progress bar is hidden and the UI step is restored on success, failure
    and abort.
11. A dead render lock never leaves the batch worker waiting forever: each
    frame has a timeout.
12. Diagnostics (`export-params`, `export-selfcheck`, `export-progress`,
    `vk-log`) keep their current tags and content.
13. The gallery write keeps `IS_PENDING`, length + SHA-256 readback, publish
    only on match, and delete on failure.
14. The Activity keeps the UI thread: no panel or view is touched from the
    worker.
15. The module never reads the live `Look`; it consumes the snapshot it was
    given.
16. The new seam is covered by a pure-JVM test for the retry policy and the
    batch plan; the gallery path stays under `GalleryExportCheck`.

## Shape

- `core/ExportAttempts` — retry/suspect policy over two callbacks (`render`,
  `ncc`). No Android types.
- `export/ExportRunner` — temp file, `renderExportFile`, `finishExport`,
  gallery publish and diagnostics, behind a `Host` for the engine session and
  the UI callbacks.
- `core/BatchPlan` stays the batch order/denominator/label owner.
- `MainActivity` keeps the Look/Frame ownership, the render mutex, the batch
  thread start/stop flags and the `reopenFrame` callbacks.

## Out of scope

- No engine, JNI or native change.
- No new export format, tier or UI.
- No change to `RenderSnapshot` or `LookRestatement`.
- No new dependency.

## Test seams

| Seam | Kind | Covers |
|---|---|---|
| `ExportAttemptsCheck` | pure JVM | retry, suspect, TIFF/DNG skip, temp discard |
| `BatchPlanCheck` | pure JVM | skip/failure split, labels, summary |
| `ExportSheetCheck` | pure JVM | rows/routing/note (existing) |
| `GalleryExportCheck` | pure JVM | publish/discard (existing) |
| device | manual | one JPEG/RAW export, one corrupt retry, batch with a film-less frame |

## Acceptance

- The exported bytes and the status text match the pre-slice build for a
  normal JPEG, TIFF and DNG.
- `checkstyleGovernance`, `checkJvm`, `lintDebug`, `testDebugUnitTest` green.
- `align16k` + `ApkPolicy` green.
- `MainActivity` loses the export cluster; the line count is recorded, not
  used as a gate.

# 02: export.ExportRunner

**Type:** task
**Blocked by:** 01
**Status:** ready

**What to build:** `app.spektralab.export.ExportRunner`, owning one export
from temp file to gallery row.

## Host

- current engine session and render mutex access (`renderExportFile` body)
- displayed bitmap for the NCC check
- UI callbacks: progress text/bar, result, failure, step
- gallery writer (the existing `writeExportToGallery` adapter)
- diagnostics sink

## Responsibilities

- allocate the temp file in the cache dir, delete it in `finally`
- call `renderExportAttempts` (from 01)
- verify length/`IS_PENDING`/digest via `GalleryExport`
- format the success/failure/suspect text exactly as today
- keep every `DebugSink` tag and payload

## Out of scope

Gallery MediaStore adapter, `RenderSnapshot`, `LookRestatement`, the batch
loop. Those stay in the Activity for 03.

## Acceptance

- One JPEG, one TIFF and one DNG export produce the same files and status text.
- Failure deletes the pending row and the temp file.
- `checkJvm` green; no new Android dependency.

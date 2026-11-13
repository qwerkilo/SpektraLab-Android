# 02: export.ExportRunner

**Type:** task
**Blocked by:** 01
**Status:** completed

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

## Landed

pp.spektralab.export.ExportRunner owns the worker, the temp file, render + RAW session, the NCC retry, the MediaStore row and diagnostics. MainActivity keeps the Host (session, mutex, bitmap, resolver, UI callbacks) and lost the render/finish/gallery helpers; 8235 → 7938 lines.

**Device (2026-11-13):** JPEG preview exported 2048×1536 · 1.4 MB · 0.576 s, byte size identical to the pre-extraction run (1,512,246 B). JPEG full size exported 8192×6144 · 37.3 MB · 30.5 s; a second full-size run produced the same 39,075,022 B file, so the tier and temp lifecycle hold.

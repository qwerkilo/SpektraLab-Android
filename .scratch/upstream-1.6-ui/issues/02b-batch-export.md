# 02b — Batch export

Status: `implemented` (device pass pending)
Blocked by: 02 (batch import), 01 (live preview).

## Scope

- Export every Frame in the strip, one at a time, through the existing
  `reopenFrame` + `exportImage` path.
- Force a reopen even for the frame already on screen; close the session and
  trim the pool between files.
- Cancelable progress; failures count and continue.
- Restore the Frame the batch started on.

## Implementation

- `exportImage(format, tier, IntConsumer)` reports success or failure after the
  gallery result reaches the UI; the old two-argument form delegates with a
  null callback.
- `reopenFrameForced` bypasses the same-frame shortcut.
- `startBatchExport` snapshots the Frame list, skips Frames with no film, and
  restores the starting Frame in `finishBatchExport`.
- `Back` sets `batchExportAbort`, which stops after the current file.

## Acceptance

- N Frames with a chosen film produce N files.
- A Frame with no film is counted as skipped, not written.
- The canvas returns to the starting Frame after the batch.

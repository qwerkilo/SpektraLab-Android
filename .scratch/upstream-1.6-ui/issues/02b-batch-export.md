# 02b — Batch export

Status: `pending`
Blocked by: 02 (batch import), 01 (live preview).

## Scope

- Export every Frame in the strip, one at a time, through the existing
  `reopenFrame` + `exportImage` path.
- Force a reopen even for the frame already on screen; close the session and
  trim the pool between files.
- Cancelable progress; failures count and continue.
- Restore the Frame the batch started on.

## Acceptance

- N Frames with a chosen film produce N files.
- A Frame with no film is counted as skipped, not written.
- The canvas returns to the starting Frame after the batch.

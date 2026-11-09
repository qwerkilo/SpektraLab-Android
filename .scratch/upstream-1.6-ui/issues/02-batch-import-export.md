# 02 — Batch import

Status: `implemented` (device pass pending)
Blocked by: 01.

## Scope

- Multi-select picker returns a `ClipData`; the Activity registers each URI as
  a Frame without decoding it.
- The first added Frame opens through the normal `reopenFrame` path, so its
  decode is the same one a single pick uses.
- Unsupported RAW extensions are skipped and counted.
- Thumbnails come from `ContentResolver.loadThumbnail` on API 29+; older
  devices show a blank chip.

## Acceptance

- Picking several files adds several chips and opens the first new chip.
- No full decode happens for the other picks until they are opened.
- Batch export is a separate ticket (`02b`), because it needs a forced reopen
  loop and a completion callback on `exportImage`.

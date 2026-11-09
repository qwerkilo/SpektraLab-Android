# 02 — Batch import and export

Status: `pending`
Blocked by: 01.

## Scope

- Multi-select picker appends Frames through the same `loadImage` path.
- A batch export queue runs the existing export code one Frame at a time.
- One cancelable progress sheet; failures name the Frame and continue.
- No engine changes and no new export format.

## Acceptance

- Importing N files yields N Frames with independent Look sessions.
- Exporting N Frames writes N files; cancelling stops after the current file.
- `checkJvm`/Robolectric green; device verification listed separately.

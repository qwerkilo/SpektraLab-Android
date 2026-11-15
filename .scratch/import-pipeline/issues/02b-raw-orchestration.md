# 02b: RAW orchestration

**Type:** task
**Blocked by:** 02a
**Status:** in progress

Move the RAW open/solve/size orchestration out of `MainActivity`.

## Landed

- `core/RawImportParams.build(filmStock, printStock, exposure)` — the open JSON
  (film omitted when null, ProPhoto input, sRGB output, 2048 preview edge).
- `core/RawSize.isShrunk(source, width, height)` — the device-cap predicate.
- `RawImportCheck` 14 checks.

`openRawForImport` and `applyRawDimensions` call them.

## Remaining

`openRawForImport` (caps + `engine.openRawSession`), `solveRawSession`,
`applyRawDimensions` UI notice and `importRawOnWorker` still live in the
Activity. The remaining slice needs an engine Host and a UI callback boundary.

## Device evidence

`2026-11-15`, `10.110.67.69:38509`: `raw16.dng` re-imported after the swap;
`RAW PREVIEW` and `RAW metadata loaded`, new frame in the strip, no crash.

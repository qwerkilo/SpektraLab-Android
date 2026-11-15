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

## Second landed slice

- `core/RawOpenPlan.of(nativeSize, maxPixels, longEdge)` — native size disables both caps.
- `importer/RawOpener.open(Sessions, path, params, plan)` — the engine call behind a
  one-method interface, so the cap policy is testable without JNI.
- `RawOpenCheck` 8 checks (plan fields + captured engine args).
- `MainActivity` 7704 → **7707**.

## Third landed slice

`importer/RawSolver.solve(Solver, "both")` owns the solve call and its
"failure never fails the import" policy; `RawOpenCheck` grows to 10 checks
(mode captured, a throwing solver does not escape). `MainActivity` 7707 → **7698**.

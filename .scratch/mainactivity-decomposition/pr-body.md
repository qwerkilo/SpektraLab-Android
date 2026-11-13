# refactor: finish the import/export extraction boundary

## What

Closes ticket 06 of the MainActivity decomposition at its sheet/picker/plan boundary, and fixes the two batch-export counting defects the split exposed.

- `ui/panel/ImportPicker` owns both intent chains (gallery, Documents), their order, and the first-resolvable launch. Gallery asks `ACTION_GET_CONTENT` first because the HyperOS `ACTION_PICK_IMAGES` component resolves but finishes immediately; Photo Picker is now the fallback.
- `ui/panel/ExportSheet` also owns the export note (`note(limited, driverLine)`), so the Activity no longer carries the TIFF/DNG copy.
- `core/BatchPlan` owns batch order and progress. Frames without a film stock are skipped rather than failed, the denominator counts only exportable frames, the progress label names the skipped count, and `summary(ok, failed)` reports skipped and failed separately.
- Request code comes from `ImportPicker.REQUEST_CODE`; the stale Photo-Picker-first Javadoc and the orphan launcher Javadoc are gone.

Still in the Activity, deliberately: the export writers, the per-frame reopen loop, the budget decision and `onActivityResult`. The under-4,500-line target in ticket 06 was not reached and is recorded as such.

## Evidence

- `checkstyleGovernance`, `checkJvm`, `lintDebug`, `testDebugUnitTest` green; `align16k` + ApkPolicy 15/15.
- New coverage: `ImportPickerTest` (Robolectric; gallery/Documents order, persistable grant, fallback, no-picker, SDK 30 chain), `BatchPlanCheck` 15, `ExportSheetCheck` 24, `PanelRowsTest.spectrumCellTogglesTheStageHistogram`.
- Device (`10.110.67.69:37069`, Android 17): gallery and Documents open real pickers; a 58.5 MP image shows the budget sheet with 55.2 MP and 6576×4749; two frames with one film stock export exactly one file and skip the other; the spectrum cell toggles the stage histogram.
- Not verified on device: the batch summary/label after this last wording change (the panel was collapsed during the attempt), and the budget sheet's fit/original rows (this ROM does not deliver `input tap` to that dialog; keyboard only reached Cancel).

## Door

Two-way: all moved code is app-layer, the engine and channel files are untouched, and reverting the branch restores the old inline chains.

# 06: Extract ImportExportController

**Type:** task
**Blocked by:** 05
**Status:** in progress — export sheet extracted, import path remains

**What to build:** Move picking, budget, batch and export into
`ui.panel.ImportExportController`.

- Own the picker launchers, import-budget dialog, batch import/export and export
  sheet.
- Host: current session, register frame, start render, export callback.
- Keep persistable URI grants and transactional gallery writes.

**Landed:** `ui.panel.ExportSheet` owns both export sheets (eight single rows
including the batch entry, four batch rows) and their format/tier routing;
`ExportSheetCheck` pins the order. `ui.panel.ImportBudgetSheet` owns the
oversized-import body, fit size and three rows; `ImportBudgetCheck` pins the
arithmetic and order. `core.BatchPlan` now drives batch order and progress:
frames without a film stock are skipped instead of being counted as failures,
and the denominator counts only frames that will export. The Activity still
owns the picker launchers, writers, batch runner and the per-frame reopen loop.

Device-checked export sheets on `10.110.67.69:38555`; the budget sheet has
check coverage but its on-device pass is still owed (the phone locked during
the attempt).

**TDD seams**

- `ImportBudgetCheck`, `ExportDigestCheck` stay the owners.
- Robolectric: export sheet rows; batch rows.
- Device: JPEG/RAW import, budget dialog, three-frame batch, export.

**Acceptance**

- [ ] Import and export behaviour matches the pre-slice build.
- [ ] `MainActivity` under 4,500 lines after this slice. (Not reached: the
  remaining coordination, picker and worker code is still in the Activity. The
  line target is deferred to a later controller slice rather than pretending the
  current split meets it.)
- [ ] Four gates green; `align16k` ok.

**Additional extraction (2026-11-13):** ui.panel.ImportPicker owns both intent chains and the first-resolvable launch; ImportPickerTest covers order, grant, fallback and no-picker. core.BatchPlan skips frames with no film and keeps the progress denominator honest (BatchPlanCheck 12). The Activity keeps onActivityResult, the budget decision, writers and the reopen loop.

**Device (2026-11-13):** on HyperOS both entries now open a real picker. 「从相册选择」 originally did nothing because ACTION_PICK_IMAGES resolved but finished immediately; ImportPicker now asks ACTION_GET_CONTENT first, which opens the system photo picker with multi-select and the 100-photo cap, and keeps ACTION_PICK_IMAGES only as a fallback. 「从文件管理器选择」 opens the system file picker.

**Note extraction (2026-11-13):** ExportSheet.note(limited, driverLine) now owns the TIFF/DNG explanation; ExportSheetCheck 23 checks pin both variants. The Activity no longer carries the sheet copy.

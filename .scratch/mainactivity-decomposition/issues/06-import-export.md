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
`ExportSheetCheck` pins the order. The Activity still builds the explanatory
note from the device probe and budget, and still owns the writers, batch runner,
picker launchers and budget dialog. Device-checked on `10.110.67.69:38555`.

**TDD seams**

- `ImportBudgetCheck`, `ExportDigestCheck` stay the owners.
- Robolectric: export sheet rows; batch rows.
- Device: JPEG/RAW import, budget dialog, three-frame batch, export.

**Acceptance**

- [ ] Import and export behaviour matches the pre-slice build.
- [ ] `MainActivity` under 4,500 lines after this slice.
- [ ] Four gates green; `align16k` ok.

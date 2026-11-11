# 06: Extract ImportExportController

**Type:** task
**Blocked by:** 05
**Status:** ready

**What to build:** Move picking, budget, batch and export into
`ui.panel.ImportExportController`.

- Own the picker launchers, import-budget dialog, batch import/export and export
  sheet.
- Host: current session, register frame, start render, export callback.
- Keep persistable URI grants and transactional gallery writes.

**TDD seams**

- `ImportBudgetCheck`, `ExportDigestCheck` stay the owners.
- Robolectric: export sheet rows; batch rows.
- Device: JPEG/RAW import, budget dialog, three-frame batch, export.

**Acceptance**

- [ ] Import and export behaviour matches the pre-slice build.
- [ ] `MainActivity` under 4,500 lines after this slice.
- [ ] Four gates green; `align16k` ok.

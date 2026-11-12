# 03: Extract FilmPanel

**Type:** task
**Blocked by:** 02
**Status:** in progress

**Resolution so far:** `ui.panel.FilmPanel` builds the section head slot, brand
facets, shelf, spectral card, measured row, film note and paper row. The
Activity keeps the catalogue, Look and refresh policy, receives the panel's
children as aliases, and supplies `Host.onPaperPicker`. Theme rebuild now
captures and restores the shelf scroll through `UiState.filmScrollX`.

**Verification:** device KODAK facet switches the shelf to 17 stocks;
Kodachrome 64 renders `DIRECT SCAN` with one undo entry; 预设 → 外观 · Portra 400
applies Portra 400 + Portra Endura at 100 %, one undo reverts it; theme switch
keeps the KODAK facet and the scrolled shelf. `PanelRowsTest` adds the brand,
shelf and paper-row guard.

**Remaining:** move the refresh policy and catalogue reads behind the panel so
the Activity no longer touches `brandRow` / `filmChipRow`. Frame-switch brand
retention still needs a two-frame device pass.

**TDD seams**

- `FilmBrandCheck` / `StockCatalogueCheck` stay the rule owners.
- Robolectric: brand row, shelf, preset button, paper row.
- Device: brand filter, Portra 400, preset apply and undo.

**Acceptance**

- [ ] Row order and labels match the pre-slice screenshot.
- [ ] Brand filter survives a frame switch.
- [ ] Four gates green.

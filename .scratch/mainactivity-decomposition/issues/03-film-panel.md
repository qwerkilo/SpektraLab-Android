# 03: Extract FilmPanel

**Type:** task
**Blocked by:** 02
**Status:** done

**Resolution:** `ui.panel.FilmPanel` owns the film view tree: section head
slot, brand facets, shelf, spectral card, measured row, film note and paper
row. The Activity is the host and keeps the catalogue, Look and refresh policy;
it wires `Host.onPaperPicker` and receives the panel's children for refresh.
Theme rebuild captures and restores the shelf scroll through
`UiState.filmScrollX`.

**Verification:** device KODAK facet switches the shelf to 17 stocks;
Kodachrome 64 renders `DIRECT SCAN` with one undo entry; 预设 → 外观 · Portra 400
applies Portra 400 + Portra Endura at 100 %, one undo reverts it; theme switch
keeps the KODAK facet and the scrolled shelf. `PanelRowsTest` adds the brand,
shelf and paper-row guard.

Brand retention across a frame switch is a shared Activity field rather than a
per-frame value, so the panel never resets it; a two-frame device pass remains
on the checklist.

**TDD seams**

- `FilmBrandCheck` / `StockCatalogueCheck` stay the rule owners.
- Robolectric: brand row, shelf, preset button, paper row.
- Device: brand filter, Portra 400, preset apply and undo.

**Acceptance**

- [ ] Row order and labels match the pre-slice screenshot.
- [ ] Brand filter survives a frame switch.
- [ ] Four gates green.

# 03: Extract FilmPanel

**Type:** task
**Blocked by:** 02
**Status:** ready

**What to build:** Move the film workspace into `ui.panel.FilmPanel`.

- Own the brand facets, shelf, preset entry, film note, paper row and spectrum
  card.
- Host: `look`, apply film/print, apply preset, render request.
- Preserve the shelf scroll position and brand pinning.

**TDD seams**

- `FilmBrandCheck` / `StockCatalogueCheck` stay the rule owners.
- Robolectric: brand row, shelf, preset button, paper row.
- Device: brand filter, Portra 400, preset apply and undo.

**Acceptance**

- [ ] Row order and labels match the pre-slice screenshot.
- [ ] Brand filter survives a frame switch.
- [ ] Four gates green.

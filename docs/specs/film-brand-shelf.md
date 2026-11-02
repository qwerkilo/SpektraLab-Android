# Film brand shelf and reversal direct scan

## Problem

The Film panel lists every shipped stock in one horizontal row. With 25 films,
finding a Kodak or Fujifilm emulsion means scrolling past the whole catalogue.
The original project had a `brandFilter` row; the Console rebuild dropped it.

Separately, the panel refuses every `positive` stock with "反转片不能印到相纸上".
That refusal is a UI limit: the engine already supports `scan_film`, and the
desktop app develops and scans reversal film directly instead of printing it.

## Solution

Add a 48 dp brand facet under the "FILM STOCK / 胶片" heading. It shows the
whole catalogue first, then each brand with its count. Selecting a brand filters
the stock chips without changing the current Look. If the active stock belongs
to another brand, pin it first after 原图 so the shelf never hides the Look in
force.

Allow reversal stocks. A positive stock sends `scan_film: true` with the stock
delta; a negative sends `false`. The Print stock stays in the Look for a later
negative but does not affect the reversal result. The readout shows
`DIRECT SCAN` and the film note says `REVERSAL · DIRECT SCAN · PAPER NOT USED`.

## Decisions

- **Brand, not "series".** The facet is Kodak, Fujifilm, Ilford, CineStill, or
  Other. It is not a film type, format, or Look.
- **Row height 48 dp.** It sits between the section head and the stock chips;
  the fixed panel budget still fits the curve, metrics, note, and paper row.
- **Counts come from the catalogue.** Empty brands are omitted. Unknown ids
  fall into Other.
- **Selection survives filtering.** Switching brands preserves the Look. The
  active chip is pinned when it is outside the filter.
- **Reset rules.** 重置当前页 and a new import return the filter to 全部; a
  Theme rebuild and a Frame switch keep it.
- **Direct scan rides the stock delta.** `scan_film` is not a slider-layer
  field; the stock change already asks the engine to rebuild.
- **Paper stays stored.** The paper row remains selectable for a later negative
  Look; the reversal readout says it is not used.

## Acceptance

- The brand row reads 全部 25, KODAK 17, FUJIFILM 6, ILFORD 1, CINESTILL 1 on
  the shipped catalogue.
- Selecting KODAK shows 原图 plus only Kodak chips; selecting 全部 restores all.
- The active stock remains visible and selected when another brand is chosen.
- Selecting Fujifilm Provia 100F renders, shows POSITIVE, and reports
  `REVERSAL · DIRECT SCAN · PAPER NOT USED`.
- The paper readout reads `DIRECT SCAN` for a reversal Look.
- `FilmBrandCheck` covers mapping, matching, labels, and counts.

## Out of scope

- Format or film-type filters.
- Exporting a reversal Look (still to be checked on device).
- Persisting the brand filter across app restarts.

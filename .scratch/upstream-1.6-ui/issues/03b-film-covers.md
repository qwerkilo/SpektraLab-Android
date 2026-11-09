# 03b — Film card covers

Status: `blocked`
Blocked by: 03, and by a source of stock cover art.

## Why blocked

The repository has no cover assets, the donor `stock_catalog.json` has no cover
field, and `gen-stock-catalog.py` does not read one. Drawing a cover from the
frame thumbnail would mislabel a photograph as a stock.

## Scope when unblocked

- Add an optional `cover` field to the catalogue generator and record.
- Show it on the 120×52 dp film card only when present; otherwise keep the
  current text card.
- Bundle the art as drawable or asset files and add the source to
  `docs/agents/` so the next stock update can refresh it.

## Acceptance

- Every stock with art shows a cover; stocks without art keep the text card.
- Missing art never blocks the shelf, selection or export.

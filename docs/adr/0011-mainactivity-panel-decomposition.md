# 0011 — Decompose MainActivity into feature panel modules

- **Status**: accepted
- **Date**: 2026-11-11
- **Supersedes**: none
- **Related**: 0001, 0004, 0007, 0010

## Context

`MainActivity.java` is 9,628 lines and still holds every workspace: stage,
readouts, film shelf, optics strip, scale cards, edge pages, import, export,
frame strip, presets and theme rebuild. Checkstyle enforces method length and
complexity, and its builders are split into small methods, but the file still
has one owner for every responsibility. Adding an upstream behaviour or a new
control means editing the same class as everything else.

The shell is already M3, the build is Gradle, and the domain seams
(`FrameLookSession`, `RenderSnapshot`, `Look`, `StockCatalogue`) are in place.
There is no Compose or ViewModel.

## Decision

Make `MainActivity` a **composition root** and extract vertical feature
**panel modules** into `app.spektralab.ui.panel`:

- `StagePanel` — preview, scrims, file/exposure readouts, histogram, scan overlay.
- `FilmPanel` — brand facets, shelf, presets, film note, paper row, spectrum card.
- `OpticsPanel` — parameter strip, slider, View/Meter/Scanner entries.
- `ScaleEdgePanel` — format cards, gate, rebate page, date page, composition sheet.
- `ImportExportController` — picking, import budget, batch, export sheets.

Each panel owns its view tree and a `refresh()` path. It talks to the Console
through a narrow host contract (current Look, Palette/M3 roles, one render
request, one feedback request) rather than reaching into Activity fields. Shared
theme, typography and M3 token helpers move to `ui` helpers; pure rules stay in
`core`; records stay in `model`; custom views stay in `ui.widget`.

Work lands in vertical slices, each with a pure/Robolectric regression and a
device screenshot:

1. Stage.
2. Film.
3. Optics.
4. Scale/Edge.
5. Import/Export.

## Alternatives rejected

- **Only extract pure logic**: shrinks few lines and leaves the view ownership
  problem untouched.
- **Mechanical split by method name**: moves code without a seam, so every panel
  still depends on every Activity field.
- **Compose / MVVM rewrite**: a technology migration disguised as a cleanup; it
  would invalidate the M3 shell, the tests and the device checklist.

## Consequences

- `MainActivity` keeps the single-render invariant, the current Frame session and
  the theme rebuild coordinator; panels never own a render thread.
- Panel extraction must preserve row order, call order and undo refresh; the
  existing `ChoiceRowCoverageCheck` and `PanelRowsTest` patterns extend to each
  slice.
- Checkstyle scope grows to `ui.panel`, and `GLOSSARY.md` gains the panel and
  composition-root terms.

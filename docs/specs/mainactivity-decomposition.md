# MainActivity panel decomposition

Status: `ready-for-agent`. Decision record: ADR-0011. Vocabulary:
`GLOSSARY.md`. Tracker: local tickets under
`.scratch/mainactivity-decomposition/issues/`.

## Goal

Reduce `MainActivity.java` from 9,628 lines to a composition root by extracting
five vertical feature panels and the import/export flows. Every slice keeps the
M3 Console, the single-render invariant, per-Frame Look undo and the existing
device behaviour.

## Non-goals

- Compose, Kotlin, MVVM or a ViewModel migration.
- New engine or Look capabilities.
- Rewriting the stage visual design.
- Moving `Look`, `FrameLookSession` or `RenderSnapshot` out of the root package
  in this program.

## Shape

`MainActivity` keeps:

- Activity lifecycle, the current Frame index, the Frame list and
  `FrameLookSession` hand-off.
- The one render mutex / render thread and the one render-request path.
- Theme mode, window insets and the root rebuild coordinator.
- Wiring the panels; it does not build their view trees.

Each panel lives in `app.spektralab.ui.panel`, owns its view tree, and defines a
narrow `Host` interface with only the actions it needs. The Activity implements
the hosts. A panel is recreated on a theme rebuild; it never stores Look state.

| Panel | Owns | Host actions |
|---|---|---|
| `StagePanel` | stage image, scrims, file/exposure readouts, histogram, scan overlay and scan readout | `displayedBitmap`, `scanState`, `toggleHistogram`, `onScanSample` |
| `FilmPanel` | brand facets, shelf, preset entry, film note, paper row, spectrum card | `look`, `applyFilm`, `applyPrint`, `applyPreset`, `renderRequested` |
| `OpticsPanel` | parameter strip, one slider, View/Meter/Scanner entries, lock | `look`, `editOptics`, `openView`, `openMeter`, `openScanner` |
| `ScaleEdgePanel` | format cards, gate, rebate page, date page, composition sheet | `look`, `applyGate`, `openRebate`, `openDate`, `openCompose` |
| `ImportExportController` | picker, import budget dialog, batch import/export, export sheet | `currentSession`, `registerFrame`, `startRender`, `export` |

Shared theme, typography, M3 role and sheet helpers move to `ui/M3Chrome`
(static helpers) before the panels; no panel re-implements a type role or a
glass surface.

## Slices

1. **Chrome**: extract `M3Chrome` helpers (m3 roles, type roles, glass surface,
   chips, sheet body). Activity delegates; no visual change.
2. **Stage**: extract `StagePanel`; readouts, histogram, scan overlay and
   progress move with it.
3. **Film**: extract `FilmPanel`; brand row, shelf, presets, paper row and
   spectrum card move with it.
4. **Optics**: extract `OpticsPanel`; strip, slider, View/Meter/Scanner entries
   and the panel lock move with it.
5. **Scale/Edge**: extract `ScaleEdgePanel`; format cards, rebate, date and
   compose sheet move with it.
6. **Import/Export**: extract `ImportExportController`; picker, budget, batch
   and export sheet move with it.

## Test seams

- Pure JVM: existing `ConsoleLayoutCheck`, `GateScaleCheck`, `OpticsDeltaCheck`,
  `ImportBudgetCheck` stay the rule owners; a panel may call them, never copy
  their arithmetic.
- Robolectric (`PanelRowsTest`): each slice adds the rows and locks that panel
  owns; a panel is constructed and refreshed under both themes.
- Device: open each workspace after the slice, compare row order and count with
  the pre-slice screenshot; exercise one edit, one undo and one frame switch.

## Acceptance

- Each slice: `checkstyleGovernance`, `checkJvm`, `lintDebug`,
  `testDebugUnitTest` green; `align16k` builds.
- `MainActivity` after all six slices is **under 4,500 lines** and contains no
  feature panel builder.
- No new copy of a Look value; no second render path; no theme rebuild render.
- Brand shelf, scan, per-frame Original, session restore and export behave as
  before on the device.

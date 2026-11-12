# MainActivity decomposition — map

Spec: `docs/specs/mainactivity-decomposition.md`
Decision: `docs/adr/0011-mainactivity-panel-decomposition.md`

Order:

1. 01 shared M3 chrome
2. 02 StagePanel
3. 03 FilmPanel
4. 04 OpticsPanel
5. 05 ScaleEdgePanel
6. 06 ImportExportController

Target: `MainActivity` under 4,500 lines, feature panels in
`app.spektralab.ui.panel`, no second render path.

Status: 01, 02 completed — `M3Chrome`, `ConsoleType`, `ConsoleTheme` and
`ui.panel.StagePanel` extracted and device-checked. 03 completed —
`ui.panel.FilmPanel` owns the film view tree and the shelf scroll survives a
theme rebuild. 04 OpticsPanel completed — head, 22-chip strip, readout, slider
and both choice rows are panel-built; only the Look and commit policy stay in
the Activity. 05 Scale/Edge in progress — `ScalePanel` owns the Gate strip and
cards; Edge/rebate/date/compose still live in the Activity.

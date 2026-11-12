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
`ui.panel.StagePanel` extracted and device-checked. 03 FilmPanel in progress —
the film workspace is built by `ui.panel.FilmPanel`; the refresh policy and
catalogue reads still live in the Activity.

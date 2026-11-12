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

Status: 01 completed — `M3Chrome`, `ConsoleType` and `ConsoleTheme` extracted
and device-checked at 390/600 dp in both themes. 02 StagePanel in progress —
the whole stage tree is built by `ui.panel.StagePanel`; the Activity still owns
the scan-fraction/visibility and histogram state until the next slice.

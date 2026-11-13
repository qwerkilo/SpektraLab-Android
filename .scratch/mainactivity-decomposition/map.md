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
the Activity. 05 Scale/Edge completed for the Gate strip and Edge landing page;
05b carries the rebate/date pages and is completed. 07 completed — the rail
starts at 600 dp and the two-column split at 720 dp. 06 is completed for the extraction boundary: export and import-budget
sheets, `ImportPicker`, the export note and `BatchPlan` are extracted; the
export writers and the batch reopen loop remain in the Activity as a later
worker-controller slice. The under-4,500-line target was not reached.

# Export worker map

Spec: `docs/specs/export-worker.md`
Base: `master` at `dec2ff7`

1. **01 `core.ExportAttempts`** — retry/suspect policy, pure JVM.
2. **02 `export.ExportRunner`** — render + gallery + diagnostics behind Host;
   blocked by 01.
3. **03 Activity wiring and device pass** — delete the old helpers, run one
   JPEG/TIFF/DNG, one retry, one batch; blocked by 02.

Target: `MainActivity` loses the export cluster; `BatchPlan` stays the batch
order owner. No engine change.

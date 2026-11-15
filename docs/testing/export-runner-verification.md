# ExportRunner device verification

Device pass for the export worker split (#124), 2026-11-15.

- **Build:** `385 / 1.6.2m-m3`, `align16k` signed APK from `master` at `821ab96`.
- **Device:** `10.110.67.69:38509` (Android 17, 1200×2608).
- **Frame:** `raw16.dng`, CineStill 800T, Kodak Vision 2383, preview `2048×1364`.

## TIFF 16-bit · 实时画质

- Status: `已导出 2048×1364 · 13.5 MB（1 秒，其中渲染 0.75 秒）（TIFF：…`
- File: `Pictures/SpektraLab/SpektraLab_1794713186921.tif`, **14,202,286 B**
- Share affordance (`分享`) appeared beside `撤销`; no `AndroidRuntime` error.

## DNG · 全尺寸

- Status: `已导出 3474×2314 · 32.2 MB（5 秒，其中渲染 1.851 秒）（DNG：线性 16 位…`
- File: `Pictures/SpektraLab/SpektraLab_1794713300047.dng`, **33,808,955 B**
- No `AndroidRuntime` error.

Both files were listed with `ls -lt` on the device after the status line
reported success, so the check is a real file, not only a toast.

## Still open

- A forced NCC retry (needs a mismatch the canvas cannot produce on demand).
- A batch export run through `ExportRunner` (the earlier 1-success/1-skip pass
  predates #124; the per-frame path is the verified single export).

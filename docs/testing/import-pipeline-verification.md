# Import pipeline verification

Device pass for the split-out import modules, 2026-11-15.

- **Build:** `385 / 1.6.2m-m3`, `align16k` signed APK from `master` at `6e843c6`.
- **Device:** `10.110.67.69:38509` (Android 17, 1200×2608).
- **Install:** `adb install -r -g`, existing session restored (`926253.jpg`, C200).

## EXIF orientation

Source: `exif6.jpg`, 1200×800, EXIF `Orientation = 6`, red block top-left,
green block bottom-right, imported through 从文件管理器选择.

Observed:

- the frame's thumbnail is **portrait 800×1200**;
- the red block is at the **top-right** and the green block at the
  **bottom-left**, i.e. the pixels were rotated 90° clockwise;
- the stage header reads `Preview ready 800×1200`;
- the comparison preview is the rotated frame, not the unrotated bitmap.

This exercises `StillDecoder` (bounds/sample decode + EXIF mapping +
exact scale), `StillPreview` (fit/no-upscale + source recycle) and
`SourcePacker` (RGB float plane) in one import.

## Render

Selecting `CineStill 800T` on the imported frame produced
`Preview ready 800×1200`, `800T / Vision 2383`, with the graded stage image and
no `AndroidRuntime` error. The session opened from the packed plane and rendered
through the normal path.

## Not covered by this pass

- HEIC/HEIF fallback through `ImageDecoder` (no HEIC sample on the device).
- RAW import (`loadRawImage` still lives in the Activity).
- Oversized-import budget dialog; it was verified separately on
  2026-11-13 (58.5 MP source, 55.2 MP budget) and its row routing is covered by
  `ImportBudgetCheck`.

## RAW source preview

`raw16.dng` (a copy of `raw-sample1.dng`, 6.37 MB) imported through the same
file-manager path after the `RawDecoder` extraction.

Observed:

- stage header `raw16.dng / RAW PREVIEW`, footer `RAW metadata loaded`;
- the decoded sunset frame is on the canvas and the frame is added to the
  thumbnail strip;
- no `AndroidRuntime` error.

This exercises `RawDecoder.preview` (`exportSource("live")` →
`copyArgbRange` → bitmap) through the real LibRaw session.

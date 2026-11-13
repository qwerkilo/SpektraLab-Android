# Import pipeline

**Status:** ready-for-agent
**Decision:** ADR 0011 (MainActivity decomposition)
**Base:** `master` at `4e85643`

## Why

The still-image import is the last large cohesive block in `MainActivity`:
decode, EXIF rotation, exact scale, pixel packing and session open. The export
cluster moved to `export/ExportRunner`; this is the same move for the entry
path, in slices that keep the engine contract unchanged.

## Slices

1. **StillDecoder** — bounds sniff, `BitmapFactory` sample decode,
   `ImageDecoder` fallback (HEIC/HEIF), EXIF orientation, exact scale. No
   engine call, no UI callback.
2. **SourcePacker** (landed) — RGBA→RGB float plane, row buffer, engine
   allocator injection, release. Covered by `SourcePackerTest`.
3. **StillPreview** (landed) — fit/no-upscale and the source-recycle rule;
   `StillPreviewTest` covers 4 cases.
4. **RawDecoder** — the LibRaw path behind one result object.
5. **ImportController** — pixel packing, preview publish, session open and
   failure text, behind a Host.

## Stories

1. A JPEG imports at the memory budget without the full-size bitmap existing
   twice.
2. A HEIC that `BitmapFactory` refuses still decodes through `ImageDecoder`
   (API 28+), with the same size rule.
3. Orientation 1–8 rotates or flips the pixels; unreadable EXIF leaves them
   alone.
4. The scaled bitmap is the one packed into the engine buffer; the discarded
   bitmap is recycled.
5. A decode failure reports `[E-1107]` and offers the file picker, not a
   generic message.
6. RAW still takes its own path and keeps its own caps.
7. The Activity keeps the budget question, the packing and the session; the
   decoder never reads a Look or a View.
8. Scale arithmetic stays in `ImportBudget`; the decoder passes
   `availableBytes` in rather than probing the device.

## Test seams

| Seam | Kind | Covers |
|---|---|---|
| `ImportBudgetCheck` | pure JVM | scale/area caps (existing) |
| `StillDecoderCheck` | source scan | Activity no longer owns decode/EXIF; one owner |
| device | manual | JPEG, HEIC, EXIF portrait, oversized JPEG, RAW |

## Out of scope

No engine/JNI change, no new format, no UI change, no change to the memory
formula.

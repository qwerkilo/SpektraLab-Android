# 01: importer.StillDecoder

**Type:** task
**Blocked by:** none
**Status:** completed

**What to build:** `app.spektralab.importer.StillDecoder`, owning a still's
decode → EXIF rotation → exact scale.

## Interface

```java
public final class StillDecoder {
    public static final class Result {
        public final Bitmap bitmap;
        public final int width;
        public final int height;
        public final int orientation;
    }
    public static Result decode(ContentResolver resolver, Uri uri,
                                int capPixels, int edgeCap, long availableBytes);
}
```

- `null` when every decoder fails.
- `BitmapFactory` bounds sniff → `inSampleSize` → stream decode.
- Fallback to `ImageDecoder` on API 28+, software allocator, target size from
  `ImportBudget.scale(..., 0)`.
- EXIF orientation read from the stream, then the file descriptor; 0/negative
  leaves the bitmap alone.
- Final scale uses `ImportBudget.scale(w, h, capPixels, edgeCap, availableBytes)`;
  the old bitmap is recycled when a new one is made.
- Diagnostics keep the `decode`, `imagedecoder`, `exif` and `import-scale` tags.

## Activity after the slice

`decodeStillSource` keeps the budget/cap decision and the `[E-1107]` sheet; it
assigns `sourceBitmap`, `sourceWidth`, `sourceHeight` from the result.
`decodeImport`, `decodeViaImageDecoder`, `applyExifRotation`,
`rotateForOrientation` and `readOrientation` are deleted.

## Acceptance

- JPEG/HEIC/portrait imports behave as before on device.
- `StillDecoderCheck` asserts the Activity no longer contains those five
  method names and that `StillDecoder` contains the decoder entry points.
- `checkJvm`, `checkstyleGovernance`, `lintDebug`, `testDebugUnitTest` green;
  `importer/**` joins checkstyle.

## Landed

`importer/StillDecoder` owns the BitmapFactory sniff/sample, ImageDecoder fallback, EXIF read, rotation and exact scale. `core/ExifTransform` maps the eight orientation values to rotate/flip and is covered by `ExifTransformCheck` (14).

`MainActivity` 7938 → 7776 lines; `StillDecoderCheck` 13 asserts the Activity no longer owns decode/EXIF. Device import not re-run: the test phone was in use.

# 01b: importer.SourcePacker

**Type:** task
**Blocked by:** 01
**Status:** completed

**What to build:** the RGBA→RGB float plane packing, out of `MainActivity`.

## Interface

```java
public final class SourcePacker {
    public interface Allocator { ByteBuffer allocate(long bytes); }
    public static long bytesFor(int width, int height);
    public static ByteBuffer pack(Bitmap bitmap, Allocator allocator);
    public static void release(ByteBuffer buffer);
}
```

- One row buffer, 3 floats per pixel, native order, rewound.
- `release` calls `SpektraLabEngine.freeDirect`; a null buffer is a no-op.
- The Activity passes `SpektraLabEngine::allocDirect`.

## Landed

`SourcePacker` + `SourcePackerTest` (2 Robolectric tests: byte count, RGB order and
255/128 normalisation). `MainActivity` 7776 → **7757**. `packSourcePixels` now
releases the previous plane and packs through the module.

## Device evidence

`2026-11-15`, `10.110.67.69:38509`, build `385 / 1.6.2m-m3` from `master`.
`exif6.jpg` (1200×800, Orientation 6) imported as a portrait 800×1200 preview with
the red top-left block at the top-right and the green bottom-right block at the
bottom-left; selecting CineStill 800T rendered `Preview ready 800×1200`.
See `docs/testing/import-pipeline-verification.md`.

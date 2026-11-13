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

# 01c: importer.StillPreview

**Type:** task
**Blocked by:** 01
**Status:** completed

**What to build:** comparison-preview sizing and the source-recycle rule.

## Interface

```java
public final class StillPreview {
    public static int[] fit(int width, int height, int maxEdge);
    public static Bitmap create(Bitmap source, int maxEdge);
}
```

- Never upscales; every dimension floors at 1.
- `create` recycles the source only when `createScaledBitmap` made a copy.
  Recycling an unchanged source left `CompareView` holding a recycled bitmap.

## Landed

`StillPreview` + `StillPreviewTest` (4 Robolectric tests: landscape/portrait,
no upscale, 1-px floor, source recycled on scale). `MainActivity` 7757 → **7748**;
`publishStillPreview` now delegates.

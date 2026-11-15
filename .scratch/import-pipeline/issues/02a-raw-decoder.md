# 02a: importer.RawDecoder

**Type:** task
**Blocked by:** 01
**Status:** completed

**What to build:** the RAW source-preview decode, out of `MainActivity`.

## Interface

```java
public final class RawDecoder {
    public static Bitmap preview(SpektraLabEngine.Session session);
}
```

- `exportSource("live")` → `copyArgbRange` → ARGB bitmap.
- Null when the session exports no source frame; the caller keeps its
  "RAW loaded, no source preview" status.
- Diagnostics keep the `raw-view` tag and the centre-pixel probe.

## Landed

`RawDecoder` + `RawDecoderCheck` (5 source guards). `MainActivity` 7748 → **7710**.
`importRawOnWorker` calls `RawDecoder.preview(session)`.

## Device evidence

`2026-11-15`, `10.110.67.69:38509`, build `385 / 1.6.2m-m3`. `raw16.dng`
imported: stage `RAW PREVIEW`, footer `RAW metadata loaded`, decoded sunset on
the canvas, no crash. See `docs/testing/import-pipeline-verification.md`.

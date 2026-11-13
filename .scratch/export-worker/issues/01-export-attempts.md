# 01: core.ExportAttempts

**Type:** task
**Blocked by:** none
**Status:** completed

**What to build:** the retry/suspect policy currently inline in
`renderExportAttempts`, as a pure-JVM seam.

## Interface

```java
public final class ExportAttempts {
    public interface Render {
        Rendered render();     // engine call
        void discard();        // delete the temp file
    }
    public interface Ncc {
        double score();        // -1 when unreadable
    }
    public static final class Rendered {
        public final int[] info; // w,h,renderMs
        public final String canvas;
    }
    public static final class Outcome {
        public final Rendered rendered;
        public final boolean suspect;
        public final int attempts;
    }
    public static Outcome run(boolean tiff, boolean dng, Render render, Ncc ncc,
                              Consumer<String> log);
}
```

- Two attempts maximum.
- TIFF/DNG return after one render; no NCC callback.
- `ExportMatch.suspect(ncc)` false returns after one render.
- A suspect first attempt discards the file and renders again.
- A suspect second attempt returns `suspect = true`.
- `log` receives the NCC/suspect lines; null log is allowed.

## Acceptance

- `ExportAttemptsCheck` covers: clean first attempt, retry then clean, retry
  then suspect, TIFF/DNG one attempt with no NCC call, discard only between
  attempts, null log.
- `checkJvm` green.
- `MainActivity#renderExportAttempts` delegates to it; observable export
  behaviour unchanged.

## Landed

`core/ExportAttempts` + `ExportAttemptsCheck` (12). `MainActivity#renderExportAttempts` now delegates; TIFF/DNG skip the comparator, a suspect first attempt discards and retries once, and diagnostics keep their tags.

---
status: accepted
date: 2026-11-02
---

# Reversal film uses direct scan

The Android editor refused every `positive` Film stock with "反转片不能印到相纸上". That was a UI limit, not an engine limit: the engine already exposes `scan_film`, which bypasses the printing stage and scans the developed film directly, and the desktop app uses it for every slide stock. Keeping the refusal hid four shipped emulsions (Provia 100F, Velvia 100, Ektachrome 100, Kodachrome 64) and made the catalogue look incomplete.

The editor now allows a reversal Film stock and sends `scan_film: true` with the stock delta; a negative sends `false`. The Print stock stays in the Look so returning to a negative restores the paper the user had, but it does not affect the reversal result. The film note says `REVERSAL · DIRECT SCAN · PAPER NOT USED`.

Consequences:

- `scan_film` rides the stock delta, not every slider delta; the engine rebuilds the pipeline when the stock changes, which is where the paper stage is added or removed.
- The paper row remains visible and selectable. It is intentionally not disabled: it is still part of the Look that a later negative selection will use.
- Preview and export both send the same parameter set, so a slide export matches the preview.
- No engine, JNI, tiling, or catalogue change is implied.

Considered options: keep the refusal (rejected: the engine and the desktop app both support slides), print a positive onto paper (rejected: physically the wrong stage and visibly wrong), hide the paper row for slides (rejected: loses the stored negative Look).

# 01 — Live preview and canvas

Status: `in progress`
Blocked by: none.

## Scope

- Add `LivePreviewGate` (pure JVM) for coalescing and invalidation.
- Request a live frame from the Optics, View and Rebate sliders while a touch
  gesture is active.
- Render at `LIVE_LONG_EDGE` 800 with `renderBegin("live", true)`.
- Cancel live frames when a full render starts, a Frame changes, an import
  starts, or the Activity is destroyed.
- Keep live frames out of undo, persistence and the busy state.

## Acceptance

- A slider drag paints small frames; release still runs one 2048 px render.
- A live frame that finishes after the full render or a Frame change is dropped.
- `LivePreviewGateCheck` passes; `checkstyleGovernance` and `checkJvm` pass.

# ADR-0006: Adopt the 1.4.4 engine/JNI core, keep the M3 Console

**Status:** accepted · 2026-11-04

## Context

Upstream `SAINTTaiYi/SpektraLab-Android` reached 1.4.4 (`9875785`). The
1.4.1–1.4.4 work includes the banded renderer (the KI-1 fix), decode white
balance, additive halation, export metadata, and RAW fixes. It also rewrites
`MainActivity` with a composition page, canvas pinch, undo, white balance,
tone, colour and grading pages.

This fork has already replaced the upstream `MainActivity` with the Material 3
Console, per Look, scan inspect, and the M3 panels. A wholesale merge would
discard that design.

## Decision

Merge the **native engine, JNI, device probe, export metadata, and tiling
documentation** from 1.4.4. Keep the M3 `MainActivity` and its Look/export
semantics. Rebuild the stock catalogue from the 1.4.4 profiles and use the
1.4.4 APK as the donor for assets and `arm64-v8a` libraries.

The upstream 1.4.4 UI features (composition page, canvas pinch, undo, view
adjust pages) are a separate M3 port, not part of this merge. `update.json`
stays on the stable 1.3.4 build; `preview.json` carries `1.4.4-m3` until the
release gate is closed.

## Consequences

- The engine fixes ship to the M3 UI without reimplementing the old upstream
  screen in parallel.
- The banded renderer was verified on a 4500×3000 JPEG: a 4704×4345
  (20.44 MP) CineStill 800T rebate export keeps the bottom `2` and `2A` edge
  print. KI-1 is fixed in the engine; the repository release gate remains a
  release decision, not a code blocker.
- The new upstream controls remain unavailable until the M3 port lands; this
  is recorded as a follow-up, not silently dropped.
- `MainActivity` is not merged from upstream, so future upstream 1.4.x UI
  changes will need the same port discipline.

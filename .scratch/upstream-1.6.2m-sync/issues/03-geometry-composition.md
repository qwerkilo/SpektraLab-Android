# 03: Geometry snapshot and composition fixes

**Type:** task
**Blocked by:** 02
**Status:** ready

**What to build:** Capture composition values when the user commits, and send
the snapshot from the render thread.

- Add `geoSnap*`, `pendingGeometry`, `geometrySeq`; `sendGeometry` sends the
  snapshot before the ordinary delta.
- Clear `lastTapMs` on UP; a failed render restores the committed angle.
- `cropToRatio` re-cuts from the whole frame; gate cards map 1:1 to `EDGE_IDS`;
  a rejected 16:9 pick explains why.
- Keep the rotation reference grid and add the arc control.

**TDD seams**

- Pure JVM `GeometrySnapshotCheck`: capture, restore, stale-sequence discard.
- Robolectric: composition rows exist; draft/apply still one render.
- Device: commit -8.64°, verify the engine receives -8.64°; crop 1:1 on a wide
  frame cuts the full frame.

**Acceptance**

- [ ] Four gates green; `align16k` ok.
- [ ] Device screenshot of the composition sheet and the rendered result.

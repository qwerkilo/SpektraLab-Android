# 04: Panel gate, rebate legibility and export self-check

**Type:** task
**Blocked by:** 03
**Status:** ready

**What to build:** Three upstream behaviour fixes that share the render path.

- `panelsLive` gates optics, print, scale, rebate and date until a render exists;
  `opticsPanel` is assigned; the lock deep-disables.
- Rebate legibility: keep marks, thicken the stroke below a 3500 px long edge,
  drop type only below 900 px, and use `max(w, h)` so portrait frames qualify.
- `exportMatchesCanvas`: 48-column grayscale NCC; a mismatch logs
  `export-corrupt`, withholds success and retries once.

**TDD seams**

- Pure JVM `ExportMatchCheck`: normal/near/noise scores and the retry decision.
- Robolectric: a fresh frame has the optics panel locked; after a render it is
  enabled.
- Device: portrait 645 rebate shows the wordmark; a corrupted export is not
  announced and exactly one retry runs.

**Acceptance**

- [ ] Four gates green.
- [ ] Device screenshots of the locked panel and the portrait rebate.

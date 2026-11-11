# 01: Shared M3 chrome helpers

**Type:** task
**Blocked by:** None
**Status:** in progress

**Resolution so far:** `ui/M3Chrome` (checked colour pair, alpha, M3
opacity, weight synthesis, CJK ranges) and `ui/ConsoleType` (label,
mixedText, typed, m3Label, mixedM3Label, applyTypeRole, applyM3LineHeight,
TfSpan) now own the shared typography and colour primitives. `MainActivity`
delegates through one `type` object; 117 net lines removed. `M3ChromeCheck` 8;
all gates green. Remaining theme/card wrappers and the top-bar builder are
still in `MainActivity`.

**What to build:** Move the theme/typography/M3 helpers out of `MainActivity`
into `app.spektralab.ui.M3Chrome`, with no visual change.

- Move `m3Role`, `m3Primary`, the type-role helpers, `glassSurface`, chip and
  sheet-body helpers.
- `MainActivity` delegates; no panel yet.
- Add `M3Chrome` package Javadoc and a check that no hardcoded colour/size is
  introduced.

**TDD seams**

- Existing `M3TokenCheck`; new `M3ChromeCheck` for role mapping and type-role
  application.
- Device: both themes, 390 dp and 600 dp, identical screenshot.

**Acceptance**

- [ ] `MainActivity` shrinks by the moved helpers; no visual diff.
- [ ] Four gates green.

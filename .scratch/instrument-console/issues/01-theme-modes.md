# 01: Theme modes on the current editor

**What to build:** Add a persisted Theme mode, a role-based Palette for light and dark, and a header toggle that rebuilds the editor in place. A switch keeps the loaded Frame and its Look; system bars follow the active Theme mode; a launch opens in the stored mode without a white flash.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] A light Palette and a dark Palette expose the same named roles; no view reads a raw colour for a themed surface, text, line, accent, or system bar.
- [ ] The header toggle switches Theme mode with no image, with a RAW Frame, and with a JPEG Frame.
- [ ] A switch keeps the loaded Frame, its Look, the active tab, the shelf state, and the Scan inspect position; it does not start a render.
- [ ] The chosen Theme mode survives a restart; the first frame uses the stored palette, with no white flash.
- [ ] System bars and status-bar icons match the active Theme mode.
- [ ] Body text reaches 4.5:1 contrast in both palettes; large text and control boundaries reach 3:1.
- [ ] Reduced-motion settings suppress non-essential theme transitions.
- [ ] Theme mode is not written into a Look, a Frame, or the engine.
- [ ] The palette's pure checks pass for every role, both contrast thresholds, and two distinct backgrounds.

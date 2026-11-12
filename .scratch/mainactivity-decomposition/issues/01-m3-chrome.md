# 01: Shared M3 chrome helpers

**Type:** task
**Blocked by:** None
**Status:** completed

**Resolution:** `ui/M3Chrome` owns the checked colour pair, alpha, M3
opacity, weight synthesis, CJK ranges and is guarded by `M3ChromeCheck` 8.
`ui/ConsoleType` owns label, mixed CJK/Latin runs, typed runs, `m3Label`,
`mixedM3Label`, `applyTypeRole` and `applyM3LineHeight`. `ui/ConsoleTheme`
owns the M3 role resolvers (`surface`, `background`, `outlineVariant`,
`onSurface`, `onSurfaceVariant`, `primary`, `role`) and the Prism glass
surface; one instance is refreshed with the palette on every rebuild.
`MainActivity` delegates through `type` and `theme`; no m3 role helper remains
in the file. Device: light 390 dp, dark 390 dp and dark 600 dp render with the
same geometry and roles; the top-bar builder itself stays for 02.

**Acceptance**

- [x] `MainActivity` shrinks by the moved helpers; no visual diff.
- [x] Four gates green.

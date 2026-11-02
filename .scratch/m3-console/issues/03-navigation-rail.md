# 03: Navigation rail on wide windows

**Type:** task
**Blocked by:** 02 (M3 tokens and the phone shell)
**Status:** pending

**What to build:** A tablet, foldable, or landscape window at ≥600 dp gets a
Material navigation rail instead of a stretched tab row. The rail and the
phone tabs share one workspace selection, and resizing across the breakpoint
keeps the edit.

**Blocked by:** 02

- [ ] ≥600 dp shows a navigation rail with Film, Optics, Scale, and Edge; <600 dp shows secondary tabs.
- [ ] Rail and tabs share one selected state; switching forms does not change the workspace.
- [ ] Rail labels and icons meet 48 dp targets and expose selected state to accessibility services.
- [ ] `ConsoleLayoutCheck` covers 600, 800, 960, and 1200 dp: rail width, stage width, and control column width.
- [ ] Drag across 599↔600 in both directions and rotate; Frame, Look, tab, Scan, and panel collapse survive; no render starts.
- [ ] 600×960 portrait and 960×600 landscape captured in both themes.

# 02: M3 shell — top app bar, tabs, rail

**Type:** task
**Blocked by:** 01
**Status:** ready-for-agent

**What to build:** The Console shell rebuilt with Material 3 components:
`MaterialToolbar`/top app bar, `TabLayout` (secondary tabs) on phones, and
`NavigationRailView` at ≥600 dp. Theme toggle, export, and state survive the
shell swap.

- [ ] Phone shell uses M3 top app bar + secondary tabs; 48 dp targets.
- [ ] ≥600 dp uses a navigation rail; tabs and rail share one selected state.
- [ ] Rotation / foldable resize keeps Frame, Look, tab, and Scan state.
- [ ] Both Theme modes and dynamic colour checked on device.

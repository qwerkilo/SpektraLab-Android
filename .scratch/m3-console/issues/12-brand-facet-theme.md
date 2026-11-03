# 12: Brand facet survives Dark → Light

**What to build:** The brand filter chips (全部 / KODAK / FUJIFILM / ILFORD / …)
follow the active Theme mode after an in-place Dark → Light switch, just like
the film cards and the rest of the Console.

**Blocked by:** None (found after ticket 10)

**Status:** completed

**Resolution:** The brand chips were created with the Material filter-chip
style but no explicit colour state lists. After `setTheme` + `rebuildForTheme`
their `ChipDrawable` kept the previous dark scheme. They now resolve
`colorSurfaceContainerLow`, `colorSecondaryContainer`, `colorOnSurfaceVariant`,
`colorOnSecondaryContainer`, and `colorOutlineVariant` at build time and set
background, stroke, and text state lists explicitly. Reproduced Dark → Light on
`10.110.67.69:34151`; the chips now return to the light surface while the film
cards stay consistent.

- [x] Dark → Light leaves the brand chips light, not dark.
- [x] The selected chip still shows the checked state and count.
- [x] Light → Dark keeps the dark chip scheme.
- [x] `checkJvm` remains green.

# 04: Film panel in Material 3

**Type:** task
**Blocked by:** 02
**Status:** completed

**What to build:** The Film workspace uses Material chips or cards for Film
stocks, keeps the Brand facet and the pinned active stock, and hosts the
spectral curve inside an M3 surface. Choosing a stock still updates the Look,
the measured values, and the Readout strip.

**Blocked by:** 02

- [ ] Film stocks are M3 chips/cards with explicit pressed, focused, selected, and disabled states.
- [ ] Brand facet counts remain correct; switching Brand keeps the current Film stock and pins it first.
- [ ] 重置当前页 returns the facet to 全部 and the shelf to the start without changing the Look.
- [ ] The spectral curve sits in an M3 surface; its labels and legend follow M3 type roles.
- [ ] Selecting a stock starts one render; the Film, Paper, and Spectrum cells update together.
- [ ] Device check at 390 dp and ≥600 dp, both themes; no clipped labels or horizontal page scroll.

**Resolution:** M3 film chips/cards, ChipGroup brand facet, pinned active stock, and FilmBrand.shelf landed; FilmBrandCheck 39. Merged into feat/m3-spec. Device was offline, so the visual states remain for ticket 10.

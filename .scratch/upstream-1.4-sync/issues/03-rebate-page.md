# 03: M3 Rebate secondary page

**What to build:** The Edge workspace shows separate Rebate and Date back
entries; the Rebate entry opens an M3 secondary page with its own switch and
all upstream Rebate controls. Back returns to Edge without losing the Look.

**Blocked by:** 01 (Core parameter, Look, JNI, and catalogue sync)

**Status:** pending

- [ ] Edge shows two M3 cards, Rebate and Date back; each opens its own secondary page.
- [ ] The Rebate page has a working on/off switch; controls stay visible and are disabled/dimmed while off.
- [ ] Mode, Gate, holes, carrier, f-number, seeds, fog, leaks, and frame number are present and write the full Look.
- [ ] Gate choices follow the frame-shape rule; a Rebate cannot be enabled for a shape no Gate can hold.
- [ ] Back restores the Edge page and the current Frame/Look; switching Frame restores that Frame's Rebate state.
- [ ] Device check: enable, change every group, disable, re-enable, export; both themes.

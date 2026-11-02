# 07: Responsive and accessibility pass

**What to build:** Make the Console usable beyond a single phone width and operable without sight or precise touch: tablet and foldable reflow, focus order and labels, larger touch targets, reduced-motion behaviour, and a final contrast check across both Theme modes.

**Blocked by:** 03 (Film panel), 04 (Optics panel with delta), 05 (Scale and Edge panels), 06 (Scan inspect)

**Status:** ready-for-agent

- [ ] At 390 dp and at one tablet width the Console has no horizontal overflow and the Frame stays primary.
- [ ] On a wide window the panel grid uses the extra width instead of stretching a single phone column.
- [ ] Theme mode, tabs, sliders, film chips, the Rebate toggle, the date imprint, and the scan line expose meaningful labels, roles, and values to accessibility services.
- [ ] Touch targets for the theme toggle, tabs, chips, sliders, and scan line meet the platform minimum.
- [ ] Focus order follows the visual order and does not trap focus.
- [ ] Reduced-motion settings suppress non-essential transitions without hiding content.
- [ ] Body text reaches 4.5:1 and large text and control boundaries reach 3:1 in both palettes.
- [ ] The Original compare remains usable at every supported width.

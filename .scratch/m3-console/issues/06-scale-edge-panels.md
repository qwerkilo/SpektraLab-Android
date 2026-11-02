# 06: Scale and Edge panels in Material 3

**Type:** task
**Blocked by:** 02
**Status:** completed

**What to build:** The Scale and Edge workspaces use Material cards and
switches while preserving the Gate/Scale pairing, the Rebate, and the date
imprint. A Look keeps the same geometry and export behaviour.

**Blocked by:** 02

- [ ] Scale cards come from the shipped Gates and are filtered by the Frame's shape; selecting one moves the Edge Gate with it.
- [ ] Rebate and date use M3 switches with correct checked state and 48 dp targets.
- [ ] Legacy Look pairs normalize to the measured Gate edge; a mismatched pair never sends an invalid layout.
- [ ] Date row shows the imprint only while enabled; preview and export send the same value.
- [ ] The Gate picker, Scale, and Edge stay in sync in both directions; changing Scale still starts a render.
- [ ] Device check with 3:2, square, and 6×17 Frames; export geometry matches preview.

**Resolution:** M3 cards and MaterialSwitch rows landed; GateScale.Pair normalization and GateScaleCheck 445. Merged into feat/m3-spec. Device was offline, so geometry/export checks remain for ticket 10.

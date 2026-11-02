# 07: Pickers and feedback in Material 3

**Type:** task
**Blocked by:** 04, 05, 06
**Status:** pending

**What to build:** Long choices move into Material bottom sheets: Film stock,
Print stock, and Gate. Render, export, and error feedback use a Material
snackbar. The full choice and the short Readout label still come from the same
stock record.

**Blocked by:** 04, 05, 06

- [ ] Film stock, Print stock, and Gate pickers open as M3 bottom sheets with the current choice marked.
- [ ] Choosing in a sheet updates the Look and closes the sheet; dismissing changes nothing.
- [ ] The Readout strip keeps its short label while the sheet and picker keep the full name.
- [ ] Render, export, and error feedback appear in an M3 snackbar that clears the gesture bar and does not cover the preview centre.
- [ ] TalkBack reaches the sheet title, current choice, and each option; the sheet traps focus and restores it on close.
- [ ] Device check at 390 dp and ≥600 dp, both themes; no duplicate sheet, no lost selection.

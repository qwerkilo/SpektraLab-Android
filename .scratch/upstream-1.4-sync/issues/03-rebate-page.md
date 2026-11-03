# 03: M3 Rebate secondary page

**What to build:** The Edge workspace shows separate Rebate and Date back
entries; the Rebate entry opens an M3 secondary page with its own switch and
all upstream Rebate controls. Back returns to Edge without losing the Look.

**Blocked by:** 01 (Core parameter, Look, JNI, and catalogue sync)

**Status:** in progress — code and pure checks pass; device verification
blocked because `10.110.67.69:34151` stopped accepting adb after the install
prompt.

- [x] Edge shows two M3 cards, Rebate and Date back; the Rebate card opens its own secondary page.
- [x] The Rebate page has a working on/off switch; controls stay visible and are disabled/dimmed while off.
- [x] Mode, Gate, holes, carrier, f-number, seeds, fog, leaks, and frame number are present and write the full Look.
- [x] Gate choices follow the frame-shape rule; a Rebate cannot be enabled for a shape no Gate can hold.
- [x] Back restores the Edge page and the current Frame/Look; switching Frame restores that Frame's Rebate state.
- [ ] Device check: enable, change every group, disable, re-enable, export; both themes.
  Blocked: the QA device accepted `adb uninstall app.spektralab`, then the
  HyperOS USB-install prompt timed out (`INSTALL_FAILED_USER_RESTRICTED`) and
  the wireless-debugging port closed. The new 16 KB APK is built and signed at
  `app/build/outputs/apk/debug/app-debug-16k-signed.apk`; the device currently
  has no SpektraLab install.

**Scope note:** The Date back card is present and inert (a snackbar points to
ticket 04). The Date page itself is intentionally not implemented here.

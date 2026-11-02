# 08: Device verification and polish

**What to build:** Verify the finished Console on a real device, fix what the matrix exposes, and leave the branch ready to merge into `master` with evidence.

**Blocked by:** 03 (Film panel), 04 (Optics panel with delta), 05 (Scale and Edge panels), 06 (Scan inspect), 07 (Responsive and accessibility pass)

**Status:** ready-for-agent

- [ ] The Java/dex build completes and the APK installs on a device.
- [ ] The manual matrix passes: no image, RAW, JPEG, Original compare, each tab, each Theme mode, one export, one Scale change, one Rebate change, Scan inspect by drag, tap, and arrows, a theme toggle after a render, and a restart after a theme change.
- [ ] Light and dark screenshots exist at 390 dp and at one tablet width.
- [ ] Logcat tags `SpektraLabJNI` and `SpektraLabRAW` show no new errors during one import and one render in each Theme mode.
- [ ] A theme toggle starts no render and leaves the loaded Frame and its Look in place.
- [ ] Export output and the `[E-xxxx]` error codes are unchanged.
- [ ] Any fix found here lands on `feat/instrument-console`; `master` is not touched until the branch is approved.
- [ ] The branch is left with a short verification note listing the device, the matrix result, and the remaining known issues.

# 05: Export re-statement, fork version, and release gate

**What to build:** One Look per Frame is re-stated in full for export,
including the stock and every new parameter; a RAW export opens its own
session. The fork identifies as 1.4.0-m3, and a build with the known engine
export defect cannot be announced as stable.

**Blocked by:** 01, 02, 03, 04

**Status:** pending

- [ ] Preview and export use one parameter builder; changing a stock or any parameter is reflected in the exported file.
- [ ] A RAW export runs in its own session and does not disturb the preview or another Frame.
- [ ] Version becomes versionCode 69 / versionName 1.4.0-m3; the fork's update.json matches; the upstream announce channel is not adopted.
- [ ] The >16.7 MP rebate bottom-print defect is documented with the workaround and a release-blocking note.
- [ ] `checkJvm`, `check-manifest.py`, the generator fixture, and the 16 KB APK build pass.
- [ ] Device check: preview/export comparison after changing every parameter group, plus the known-issue size case or a recorded block.

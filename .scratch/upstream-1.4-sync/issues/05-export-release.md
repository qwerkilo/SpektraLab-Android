# 05: Export re-statement, fork version, and release gate

**What to build:** One Look per Frame is re-stated in full for export,
including the stock and every new parameter; a RAW export opens its own
session. The fork identifies as 1.4.0-m3, and a build with the known engine
export defect cannot be announced as stable.

**Blocked by:** 01, 02, 03, 04

**Status:** completed (app/build; device RAW and >16.7 MP cases deferred to 06)

**Resolution:** `LookRestatement` is now the single parameter builder: it turns
a `Look` plus the file's own facts (RAW, long edge, Reversal stock, edge print,
imprint text) into the engine delta, and both preview and export call it.
`MainActivity.restatedParams` snapshots the live controls into a Look, so the
Film stock, Print stock, all 22 optics, camera filter, protection norm, every
Rebate field, and every Date back field are re-stated; the inline format string
and the duplicate grain/monochrome/JSON helpers are gone. A RAW export opens a
second `openRawSession` with the caps stored at import, sets the same Look on
it, exports, and closes it inside the render lock; the preview session and
another Frame are untouched. Version is 69 / `1.4.0-m3` in the manifest,
Gradle, and `update.json`; the fork URL is kept, the notes are fork-written,
and the channel is `preview`. `docs/known-issues.md` records the >16.7 MP
rebate bottom-print loss, its canvas workaround, and `Release gate: open`;
`VersionCheck` pins the four version strings and refuses a `stable` channel
while the gate is open. `checkJvm` adds `LookRestatementCheck` (183) and
`VersionCheck` (22), and the existing checks, `check-manifest.py`, the
generator fixture, and the 16 KB APK all pass. Device: 16 KB APK installed on
`10.110.67.69:39613` (Android 17); a 3000×2000 JPEG exported after changing
film, exposure, camera filter, protection norm, Rebate, and Date back shows
every change in the file and the bottom edge print is present at 3156×2897.
RAW export and the >16.7 MP case stay for ticket 06 (no RAW sample on device;
the device was taken over by a WeChat call before the big-canvas pass).

- [x] Preview and export use one parameter builder; changing a stock or any parameter is reflected in the exported file.
- [x] A RAW export runs in its own session and does not disturb the preview or another Frame.
- [x] Version becomes versionCode 69 / versionName 1.4.0-m3; the fork's update.json matches; the upstream announce channel is not adopted.
- [x] The >16.7 MP rebate bottom-print defect is documented with the workaround and a release-blocking note.
- [x] `checkJvm`, `check-manifest.py`, the generator fixture, and the 16 KB APK build pass.
- [ ] Device check: preview/export comparison after changing every parameter group, plus the known-issue size case or a recorded block.

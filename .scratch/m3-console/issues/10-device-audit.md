# 10: Device audit and M3 close-out

**Type:** task
**Blocked by:** 03, 04, 05, 06, 07, 08, 09
**Status:** blocked

**Blocker:** `10.110.67.69:35443` is unreachable (`adb connect` timeout 10060, no attached device), so the install, cold start, theme/dynamic-colour, rail, TalkBack, large-text, and 16 KB device checks cannot run. No emulator or system image is installed locally.

**What to build:** The full M3 Console is audited on real devices and closed
out against the spec. This is the only ticket that may claim the migration is
complete.

**Blocked by:** 03, 04, 05, 06, 07, 08, 09

- [ ] 390 dp phone and 600 dp tablet, portrait + landscape, both themes.
- [ ] Dynamic color on (API 31+) and brand fallback (API 24–30) checked.
- [ ] Import JPEG and RAW, film/paper, optics, scale/edge, scan, compare, export all pass.
- [ ] 16 KB APK installs and launches with no compatibility dialog on Android 15/16.
- [ ] Existing JVM checks, `ManifestPolicy`, and the device checklist remain green.
- [ ] Screenshots and a short close-out note are attached to the map; the spec status is updated.

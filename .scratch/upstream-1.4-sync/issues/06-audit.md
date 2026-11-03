# 06: M3 + upstream device audit

**What to build:** The merged M3 and upstream capabilities are audited
together on a real device. This ticket can only close when both the M3 baseline
and the new upstream features are observed, and the release gate is honest.

**Blocked by:** 05 (Export re-statement, fork version, and release gate)

**Status:** pending

- [ ] The existing ticket 10 M3 baseline is captured before the sync and remains in the audit record.
- [ ] Install the 1.4.0-m3 16 KB APK; cold start with no AndroidRuntime crash and no compatibility dialog.
- [ ] 400 dp phone and ≥600 dp rail, both themes, dynamic colour on API 31+; brand fallback is recorded as executable or not.
- [ ] Import JPEG/RAW, change all 22 parameters, camera filter, protection norm, Rebate and Date pages, then export.
- [ ] Brand facet, spectral curve, Scan inspect, sheets, snackbar, and security behavior remain intact.
- [ ] TalkBack roles/ranges, large text, reduced motion, and pressed/focused/disabled states are checked.
- [ ] The >16.7 MP bottom-print case is either observed fixed or recorded as the release blocker; no stable release is announced.

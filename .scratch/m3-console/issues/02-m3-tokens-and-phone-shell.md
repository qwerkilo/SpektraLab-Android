# 02: M3 tokens and the phone shell

**Type:** task
**Blocked by:** 01 (Gradle + M3 foundation)
**Status:** completed

**Resolution:** M3 roles/type/shape/state tokens and the phone shell landed;
`checkJvm` reports 7,489 checks, `ManifestPolicy` 26/26, `align16k` builds and
signs. Both themes and the in-place theme switch were captured on
`10.110.67.69:35443` by the implementer (same Frame, Look, Optics tab, and Scan
state). During the merge gate the device went offline, so the merged APK was
not reinstalled; the JVM/manifest/APK gates are green.

**What to build:** The phone Console becomes a Material 3 application. The
control system gets its M3 color roles, type roles, shape, and state layers;
the shell gets a Material top app bar and secondary tabs. The photograph stays
on a fixed neutral stage, and the spectral R/G/B stay measured colours.

**Blocked by:** 01

- [ ] Top app bar carries the wordmark, Theme mode, and Export; the four tabs use secondary tab behaviour and 48 dp targets.
- [ ] Every control colour and type size comes from an M3 role or the brand fallback, not a new ad-hoc hex or sp value.
- [ ] Dynamic color applies on API 31+; API 24–30 uses the documented brand seed; the stage and spectrum are excluded in both cases.
- [ ] `M3TokenCheck` proves the required roles, fallback completeness, and the stage/spectrum exclusions; `PaletteCheck` stays green.
- [ ] Theme switch, tab switch, rotation, and a 599/600 dp resize preserve the Frame, Look, selected tab, and Scan state without a render.
- [ ] Both themes captured on the device; no render, no white flash, no AndroidRuntime error.

# Upstream 1.4.0 sync into the M3 Console

Status: `ready-for-agent`. Tracker: GitHub Issues are disabled, so this spec
lives in the repository. Decision record: ADR-0005 (upstream 1.4.0 sync into
the M3 line). Vocabulary: `GLOSSARY.md`.

## Problem Statement

The M3 Console is now the product's UI, but it is behind the Android upstream.
Upstream 1.4.0 adds the 22 optical parameters the engine already exposes, a
rebuilt Rebate page, an editable Date back, per-photograph Looks, full export
parameter re-statement, and better Vulkan/permission errors. None of that is in
the M3 line, so the fork cannot use the newer engine controls and still looks
like an older build.

A direct upstream merge is not safe. Upstream's commit rewrites a large part of
the old single-file UI, while this fork has the M3 top app bar, tabs, rail,
chips, sheets, spectral curve, brand facet, Scan inspect, and the security
hardening from the QA pass. Merging upstream wholesale would bring back the old
UI and drop fork behaviour. Upstream also has a known full-size export defect
when the rebate canvas is larger than ~16.7 MP: the bottom edge print is lost.

## Solution

Port upstream 1.4.0's capabilities into the M3 line without adopting its UI or
its release channel:

- Keep the M3 Console shell and fork-only features.
- Add the 22 optical parameters behind an M3 parameter strip and one M3 slider,
  plus the camera filter and protection norm choices.
- Rebuild the Rebate and Date back experiences as M3 secondary pages inside the
  Edge workspace.
- Adopt the upstream Look model, export re-statement, RAW export session, JNI
  error detail, canvas trim, permission error, and `targetPrint` fallback.
- Keep existing Looks working by filling new parameters with defaults.
- Version the fork as `1.4.0-m3` (versionCode 69) and leave the upstream
  `update.json`/announce channel out of this work.
- Ship the code, but block a release until the engine-side >16.7 MP bottom
  rebate print defect is fixed or explicitly accepted.

## User Stories

1. As a photographer, I want all 22 optical parameters exposed, so that the app can use the engine's full optical model.
2. As a photographer, I want to pick a parameter from a compact strip and adjust it with one large slider, so that I can reach any parameter without a very tall panel.
3. As a photographer, I want the active parameter's name, value, unit, and default delta visible, so that I know what I am changing.
4. As a photographer, I want a reset control for the active parameter, so that I can return it to its calibrated default without resetting the whole Look.
5. As a photographer, I want each parameter to expose its real range and unit, so that the values match the engine and the exported file.
6. As a photographer, I want a camera filter choice with the eight Wratten filters, so that I can model the filter in front of the lens.
7. As a photographer, I want a protection norm choice, so that highlight and shadow protection follow the scene measurement I expect.
8. As a photographer, I want the Film, Paper, Spectrum, and Readout strip to survive the Optics changes, so that choosing a film and adjusting optics stay one workflow.
9. As a photographer, I want the Edge workspace to show separate Rebate and Date back entries, so that each feature has room for its own controls.
10. As a photographer, I want the Rebate page to have an on/off switch and greyed-out controls while off, so that the page explains what is available without pretending the controls work.
11. As a photographer, I want the Rebate page to expose mode, Gate, holes, carrier, f-number, seeds, fog, leaks, and frame number, so that the film edge is physically modelled rather than a border.
12. As a photographer, I want the Date back page to expose text, insets, size, exposure, corner, style, and data line, so that the imprint can match the camera that made the picture.
13. As a photographer, I want the Date back text to default from the frame's EXIF date, so that the imprint starts from the real capture date.
14. As a photographer, I want a Back action from either secondary page, so that I can return to the Edge workspace without losing the Look.
15. As a photographer, I want one Look per Frame, so that switching between photographs does not carry optical or Edge settings across.
16. As a photographer, I want an older Look to open with the new parameters at their defaults, so that upgrading does not reset my film, paper, exposure, or geometry.
17. As a photographer, I want the export to re-state the whole parameter set with the film stock, so that the exported file is not built from stale defaults.
18. As a photographer, I want a RAW export to open its own session, so that exporting a RAW does not disturb the preview or another Frame.
19. As a photographer, I want a render failure to name the actual Vulkan/driver cause, so that E-9000 is not used for every startup problem.
20. As a photographer, I want a revoked picker permission to say the picker grant expired, so that I know to import the photo again rather than reinstall the app.
21. As a photographer, I want the rebate canvas trimmed correctly on export, so that a full-size file does not show the rebate in the wrong place.
22. As a photographer, I want the known >16.7 MP bottom-print defect documented and release-blocking, so that a build with a known export loss is not announced as stable.
23. As a photographer, I want the brand facet, spectral curve, Scan inspect, M3 rail, and M3 sheets to remain, so that upstream features do not regress the M3 work.
24. As a photographer, I want the security fixes (safe file names, update allowlist, cleartext policy, backup) to remain, so that syncing upstream does not reopen fixed holes.
25. As a photographer, I want the 16 KB-page APK to remain installable, so that Android 15/16 does not show the compatibility warning.
26. As a photographer, I want the same preview and export after the sync, so that upstream parameter wiring does not change the image by accident.
27. As a TalkBack user, I want the parameter strip, slider, switches, and secondary pages to expose role, selected state, range, and value, so that the new controls are operable without sight.
28. As a user with large text, I want the parameter strip and secondary pages to scroll instead of clipping, so that labels and values stay readable.
29. As a user with reduced motion, I want page transitions to settle immediately, so that the new pages respect the system setting.
30. As a maintainer, I want the upstream feature commit isolated from its announce commit, so that the fork keeps its own version and update channel.
31. As a maintainer, I want old Look JSON to remain readable and new fields to round-trip, so that the data model can evolve without invalidating saved Frames.
32. As a maintainer, I want the JNI/error changes covered by pure checks, so that driver and permission failures do not regress silently.
33. As a maintainer, I want the stock catalogue's `targetPrint` fallback generated by the existing generator fixture, so that CineStill and Vision3 aliases print on the intended paper.
34. As a maintainer, I want the sync split into core, optics, Rebate, Date, export, and audit tickets, so that each change is independently reviewable.
35. As a maintainer, I want a final device audit on the merged tree, so that the M3 and upstream behaviours are verified together.

## Implementation Decisions

- **Source commit.** Port the feature commit that adds the 22 parameters,
  Rebate/Date pages, Look/export changes, JNI/error detail, catalogue
  `targetPrint`, and launcher icons. Exclude the following announce commit and
  its `update.json`, `versionCode` 100, and `versionName` 1.4.0.
- **Branch.** Work on a sync branch from the M3 integration line, after the
  current device audit has captured a baseline. Do not merge the upstream UI
  wholesale.
- **Version.** The fork becomes `versionCode` 69 and `versionName` `1.4.0-m3`;
  `update.json` is updated to match only when the fork release is prepared.
  The upstream `stable` channel and announce origin are not adopted.
- **Optics surface.** A parameter strip of M3 chips selects one of the 22
  parameters. One 48 dp M3 slider edits the active parameter; the row shows
  name, value, unit, display-only Δ, and reset. Camera filter and protection
  norm are M3 choice controls in the same Optics page.
- **Parameter contract.** Each parameter owns a stable id, label, glyph, min,
  max, step, unit, default, and clamp. The pure parameter model is the single
  source for the slider, the reset, the JSON wire name, and the test
  expectations.
- **Look compatibility.** New Look fields are written by the current app and
  defaulted on read when absent. An older Look keeps its existing film, paper,
  exposure, grain, halation, glare, scale, and edge values; it is never reset
  merely because new fields are missing.
- **Rebate page.** The Edge workspace shows two M3 cards (Rebate, Date back).
  Either opens a secondary page in the same workspace with a back action. The
  page's switch controls whether its controls are enabled; disabled controls
  stay visible and are dimmed, not hidden.
- **Date page.** The Date back controls follow the upstream set: text, left and
  top insets, size, exposure, corner, style, and capture-data line. The default
  text comes from the frame's EXIF date, not the current clock.
- **Export contract.** Export re-states the full Look, including the stock and
  all new parameters, so the engine cannot use stale defaults. A RAW export
  opens its own session. Preview and export use the same parameter builder.
- **JNI and errors.** The engine-create failure carries the engine's own
  reason; the canvas trim handles the top-left canvas case; E-1005, E-1006,
  E-1011, and E-1304 get distinct user-facing text. Error numbers stay stable.
- **Catalogue.** The stock generator gains the `targetPrint` fallback for
  CineStill and Vision3 aliases; the existing spectral series remain the
  source for the curve.
- **Fork features.** Brand facet, spectral curve, Scan inspect, M3 shell,
  safe file names, update URL allowlist, cleartext policy, backup policy, and
  the 16 KB APK path are preserved. Upstream UI code is adapted, not copied
  over them.
- **Known engine defect.** The >16.7 MP rebate bottom-print loss is an engine
  issue; this spec does not change the out-of-tree engine. The app-side trim
  is included, the issue and its workaround are recorded in
  `docs/known-issues.md` (KI-1), and a stable release is blocked until the
  engine is fixed or the issue is explicitly accepted there. While the gate is
  open, `update.json` must not use the `stable` channel.
- **Assets.** The upstream adaptive launcher icon may be adopted, but the M3
  colour roles stay the control source; the icon's plate colour is an asset,
  not a Palette/M3 role.

## Testing Decisions

A good test observes an external contract: a parameter's id, range, default,
and wire value; an older Look's final values; an error's user-facing code; a
catalogue entry's `targetPrint`; or a device behaviour on a real phone. It does
not assert private helper structure or a particular animation frame.

- **Highest seam: the device.** Install the merged 16 KB APK, import JPEG/RAW,
  select a film and paper, change every Optics parameter, open both Edge
  secondary pages, export, and compare preview with export. This is the only
  seam that can see Material components and engine behaviour together.
- **Pure JVM: parameter contract.** Extend the existing optics-delta check to
  all 22 parameters + camera filter + protection norm: stable ids, ranges,
  defaults, units, clamping, and display values.
- **Pure JVM: Look compatibility.** Add a pure Look-parameter check for an old
  JSON object without new fields: every old value survives, every new field
  takes its documented default, and a full round-trip is stable.
- **Pure JVM: errors.** Extend the existing error check for E-1005, E-1006,
  E-1011, and E-1304, plus the canvas-trim status string.
- **Generator fixture.** Extend the existing stock-catalog generator check for
  `targetPrint` from the profile and the CineStill/Vision3 fallback; the real
  spectral series must not be dropped.
- **Regression.** The existing Palette, M3Tokens, SpectrumCurve, ScanSample,
  FilmBrand, StockNames, GateScale, ConsoleLayout, ThemeMode, SafeFileName,
  ReleaseUrl, SpkError, ManifestPolicy, and stock-generator checks must stay
  green.
- **Prior art.** The existing plain `*Check` classes and
  `app/build/check-manifest.py` are the prior art for pure/contract checks.
  `docs/testing/manual-device-checklist.md` is the prior art for device work.
- **Release gate.** A stable release requires a >16.7 MP rebate export test
  with the bottom band present, or an explicit recorded acceptance.

## Out of Scope

- Changing the out-of-tree engine, Vulkan shaders, tiling, or the
  >16.7 MP bottom-print fix itself.
- Adopting upstream's old UI, release channel, announce script, or version
  code 100.
- Compose, a new navigation model, or a new design system.
- Rewriting the brand facet, spectral curve, Scan inspect, or compare.
- Re-encoding the stock catalogue's existing spectral data.
- Publishing a stable release while the engine defect is unresolved.

## Further Notes

- The upstream feature commit is based on a common ancestor before the M3 work;
  a direct merge conflicts in the UI, catalogue generator, colors, and icon.
  The sync is a port, not a merge of the upstream UI.
- The existing device audit must finish first so the M3 baseline stays useful.
- The version name is `1.4.0-m3` to make the fork lineage obvious without
  claiming the upstream 1.4.0 release.
- The final audit must include both M3 and upstream-specific cases; passing one
  side alone is not enough.

# 0005 · Upstream 1.4.0 sync into the M3 line

Status: accepted (2026-11-03)

## Context

The Android upstream shipped 1.4.0: 22 optical parameters behind one slider,
a rebuilt Rebate page, an editable Date back, per-photograph Looks, full export
parameter re-statement, RAW export sessions, and better Vulkan/permission
errors. The fork is now on the Material 3 Console, with a different shell,
brand facet, spectral curve, Scan inspect, and a security-hardening pass.

A direct merge is unsafe. The upstream commit rewrites a large part of the old
single-file UI, and its announce commit changes the version and update channel.
The merge has textual conflicts in the UI, stock generator, colours, and
launcher icon; auto-merged JNI and error files would still need review. The
upstream 1.4.0 release also has a known engine-side export defect above
~16.7 MP: the bottom rebate print is lost.

## Decision

- **Port the feature, not the UI.** Bring upstream 1.4.0's capabilities into
  the M3 line; keep the M3 shell, brand facet, spectral curve, Scan inspect,
  and security fixes.
- **Feature commit only.** The upstream announce commit, its version code 100
  and version name 1.4.0, and its `update.json` channel are excluded. The fork
  becomes `1.4.0-m3` at version code 69.
- **One parameter strip, one slider.** The 22 optical parameters use an M3
  chip strip and a single 48 dp slider with name, value, unit, delta, and
  reset. Camera filter and protection norm are M3 choice controls.
- **Secondary pages for Edge.** Rebate and Date back become M3 pages inside the
  Edge workspace, each with a back action and an on/off switch. Controls stay
  visible but dimmed while off.
- **Look compatibility.** New fields default on read; old Looks keep their
  existing values and are never reset because a field is absent.
- **Export re-states the full Look.** Preview and export share one parameter
  builder (`LookRestatement`); RAW export opens its own session so the preview
  and another Frame are untouched.
- **Engine defect stays out of tree.** The app-side canvas trim is included,
  the known >16.7 MP bottom-print loss is documented in
  `docs/known-issues.md` with its workaround, and a stable release is blocked
  until the engine fix lands or the issue is explicitly accepted there.

## Consequences

- The sync is a port with a large review surface in the UI, not a one-commit
  merge; the spec and tickets split it into core, optics, Rebate, Date, export,
  and audit.
- Existing M3 checks stay valid; the parameter and Look seams gain new checks.
- The fork's version name advertises its lineage without claiming the upstream
  release.
- A release can be built and installed for testing, but cannot be announced as
  stable while the engine export defect remains: the gate is recorded in
  `docs/known-issues.md` (KI-1), the gated build is announced only in
  `preview.json`, and `update.json` stays on the last stable build until the
  gate is fixed or explicitly accepted.
- The upstream old UI and release channel are deliberately left behind; future
  upstream syncs will need the same capability-level port rather than a merge.

## Considered options

- **Merge upstream master wholesale** — rejected: restores the old UI, drops
  M3/fork features, and adopts the wrong release channel.
- **Wait for upstream 1.4.1** — rejected for this effort: the 22 parameters and
  Rebate/Date work are independent of the engine print fix, and the release can
  stay blocked while the app is tested.
- **Port only the core JNI/parameters and leave the UI for later** — rejected:
  the new parameters would have no usable surface in the M3 app.
- **Keep the old upstream UI and reapply M3 later** — rejected: discards the
  current M3 baseline and doubles the UI work.

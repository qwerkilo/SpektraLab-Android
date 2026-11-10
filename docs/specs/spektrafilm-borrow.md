# Spektrafilm-android app-layer borrow

Status: `ready-for-agent`

## Problem Statement

The editor has strong film/paper physics but weak app-layer conveniences that the
reference Android port already proves useful: there is no one-tap way to reach a
recognisable film look, applying a saved preset is all-or-nothing, a process
death loses the current photograph and its Look, and a failed gallery write can
leave a pending MediaStore row or publish a truncated file. These are user-facing
gaps; none requires a new engine parameter.

## Solution

Borrow four behaviours from `thetechgeekko/Spektrafilm-android` and reimplement
them in the existing Java/View console:

1. Six **Built-in looks** authored as sparse overlays, listed above the user's
   presets, applied through a **Preset strength** sheet.
2. A **Session document** that restores the current photograph, its Look and the
   selected page after the process is recreated.
3. A **Transactional export** that deletes its `IS_PENDING` row on failure and
   verifies the copied bytes before publishing it.
4. No architecture rewrite: no Compose, no engine change, no masks/curves/output
   colour-management work in this slice.

## User Stories

1. As a photographer, I want six familiar film looks in the preset sheet, so I
   can start from Portra 400 or Tri-X without knowing which paper matches.
2. As a photographer, I want a built-in look to change only the film, paper and
   look parameters it authors, so my crop, rotation and date back stay untouched.
3. As a photographer, I want to see the film and paper a built-in look will use
   before I apply it, so the choice is not a surprise.
4. As a photographer, I want to dial a preset's strength from 0 to 100 %, so a
   look can be a subtle starting point rather than an all-or-nothing filter.
5. As a photographer, I want 0 % to leave the Frame exactly as it was, so I can
   back out of the sheet without a render or an undo entry.
6. As a photographer, I want continuous values such as grain, halation and
   temperature to cross-fade, so the strength control feels continuous.
7. As a photographer, I want film, paper, toggles and rotation to switch at a
   sensible midpoint, so a half-strength look is never an invalid mixture.
8. As a photographer, I want a strength change to record exactly one undo entry
   and render exactly once, so I can undo it in one tap.
9. As a photographer, I want a cancelled strength sheet to leave the Frame
   untouched, so opening a look is free.
10. As a photographer, I want the built-in look list and the strength sheet to
    work at 390 dp, the ≥600 dp rail and large text, so the new controls do not
    break the console.
11. As a photographer, I want my current photograph, its Look and the selected
    page to return after Android recreates the app, so a process death does not
    throw away my edit.
12. As a photographer, I want a restored Look to show the same film, paper,
    grain, halation, view and composition values I left, so restoration is not a
    partial state.
13. As a photographer, I want a missing or revoked source to be discarded with a
    readable message, so the app does not restore a broken Frame.
14. As a photographer, I want the session write to be atomic, so a crash during
    the write cannot leave a half-written JSON file.
15. As a photographer, I want only the current Frame restored in this slice, so
    the app does not spend minutes reopening a batch after a restart.
16. As a photographer, I want a failed gallery export to remove its pending
    MediaStore row, so the gallery is not littered with invisible entries.
17. As a photographer, I want a truncated export to be rejected before the row is
    published, so I never see a half-written image in the gallery.
18. As a photographer, I want a successful export to be published only after its
    copied length and SHA-256 match the staged file, so success means the file
    is actually there.
19. As a photographer, I want a failed export to keep the existing readable error
    and size context, so I can report what happened.
20. As a photographer, I want the export transaction to work for JPEG, TIFF and
    DNG without changing their render paths, so format quality is unchanged.
21. As a photographer, I want a session restore to respect the import budget, so
    a large source still asks before shrinking.
22. As a photographer, I want a restored slide film to keep its direct-scan
    behaviour, so a Kodachrome look does not try to print on paper.
23. As a photographer, I want a restored Frame to be undoable from its saved
    Look, so the first edit after a restart has the same history baseline.
24. As a maintainer, I want the built-in look and preset strength rules in pure
    JVM modules, so they are verified without a device.
25. As a maintainer, I want the session JSON and digest rules in pure JVM modules,
    so the file format and verification are testable.
26. As a maintainer, I want the Activity wiring pinned by Robolectric rows, so a
    later panel refactor cannot silently drop the built-in list or strength sheet.
27. As a maintainer, I want the new vocabulary in `GLOSSARY.md` and the trade-offs
    in an ADR, so later work does not call a full preset a built-in look.
28. As a maintainer, I want the manual device checklist to name the restore and
    export-failure paths, so the parts Robolectric cannot prove have an owner.
29. As a maintainer, I want the existing `*Check`, `testDebugUnitTest`, lint and
    device gates to stay green, so this slice does not lower the bar.
30. As a maintainer, I want AGENTS.md to point at the new seams and checks, so the
    next session does not rediscover them.

## Implementation Decisions

- A **Built-in look** is a sparse overlay, not a complete Look. It names a film,
  a paper and only the look fields it authors. Application merges those fields
  into a detached copy of the current Look.
- A **Preset strength** operates on the two complete Look JSON objects. Numeric
  leaves and equal-length numeric arrays interpolate. Booleans, strings and
  categorical numeric leaves (`cseed`, `fseed`, `gturn`, document `version`)
  switch at 50 %. The result is read back through the existing Look parser.
- Built-in looks appear first in the existing preset sheet; user presets and
  clipboard import keep their current actions. A user preset gains a second
  action for strength without changing the existing direct apply.
- The strength sheet is a draft: the slider updates a label only. Applying
  writes one `FrameLookSession.edit`, refreshes the same panels as a preset
  apply, and renders once. Zero strength dismisses without an edit.
- The **Session document** stores only the current source URI, its display name,
  the RAW flag, the current Look JSON, the selected page, and the Scan fraction
  and visibility. It contains no Bitmap, engine handle, URI grant token or
  multi-frame list.
- The session file lives under `noBackupFilesDir`, is written through a temporary
  file and an atomic rename, and is restored only after the source URI is still
  readable. A missing, malformed or revoked source discards the file.
- Restore reuses the normal import path and the normal import budget. The saved
  Look is attached to the Frame that the import creates; the Frame's first
  render then uses the restored Look. A restored slide keeps the existing
  positive-film direct-scan rule.
- **Transactional export** keeps the existing staging file and `IS_PENDING`
  row. A copy failure deletes the row. After copying, the app reopens the row and
  compares length and SHA-256 with the staged file; only a match publishes the
  row. The existing render formats and tier choices do not change.
- The work stays in Java/View. No Compose screen, no Hilt, no engine parameter,
  no native writer is introduced.

## Testing Decisions

- Tests assert external behaviour only: a built-in look keeps omitted fields; a
  strength of 0 returns the base Look, 1 returns the target, 0.5 interpolates
  continuous values and snaps categorical values; a session document round-trips
  and rejects malformed or missing-source JSON; a digest compare accepts equal
  bytes and rejects a one-byte difference.
- Seam 1: `BuiltInLook` and `PresetAmount`, pure JVM, covered by
  `BuiltInLookCheck` and `PresetAmountCheck`.
- Seam 2: the session document JSON and file round-trip, pure JVM, covered by
  `SessionDocumentCheck`.
- Seam 3: digest and byte-count verification, pure JVM, covered by
  `ExportDigestCheck`.
- Seam 4: Activity construction and sheet rows, JUnit 4 + Robolectric in
  `PanelRowsTest`: built-in looks listed, strength sheet opens, and a cancelled
  sheet does not change the Look.
- Seam 5: device/manual for MediaStore publication and process-death restore;
  Robolectric cannot provide a real provider or process kill.
- Prior art: `LookPresetCheck`, `LookCheck`, `PanelRowsTest`, and the existing
  `checkJvm` / `testDebugUnitTest` tasks. The `*Check` programs remain the
  fast gate; Robolectric only covers wiring.

## Out of Scope

- Compose, Hilt, multi-module rewrite.
- Full multi-frame session, undo/redo persistence, export-option persistence,
  masks, presets groups, or a pending-export journal.
- Tone curves, HSL, local adjustments, ICC profiles, PNG16, TIFF32F,
  scene-linear export, LUT export, Ultra HDR.
- CPU engine fallback, additional ABIs, engine changes.
- Reading or writing the reference project's Compose code; only behaviour and
  contracts are borrowed.

## Further Notes

- The reference project is GPLv3, like this repository. Any copied code would
  need source and modification attribution; this spec prefers reimplementation.
- The built-in look set mirrors the reference's six films but maps them to the
  stocks and papers this catalogue actually ships.
- The reference's preset amount is a flat-JSON cross-fade; this project's Look
  JSON is also flat except for the nested view object, which uses the same
  recursive rule.

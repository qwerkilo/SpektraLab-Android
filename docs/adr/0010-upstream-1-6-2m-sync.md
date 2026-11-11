# 0010 — Sync upstream 1.6.2m as a native core plus behaviour fixes

- **Status**: accepted
- **Date**: 2026-11-11
- **Supersedes**: none
- **Related**: 0005, 0006, 0007

## Context

The Android upstream moved from `1.6.0e` (`08183cd`) to **1.6.2m (385)**
(`b17f6d2`) in five commits. The delta is large in the app layer — `MainActivity`
grows by 2,831 lines — and also changes the JNI (`+184`) and the native build
(`CMakeLists.txt +177`). The upstream UI is still the old Console; this fork has
the Material 3 Prism Table Console.

The upstream commits contain two different kinds of work:

1. Native/JNI fixes and capabilities that every UI should get.
2. Behaviour fixes written against the old Console: panel gate, per-frame
   Original, portrait rebates, export self-check, rotation reference and
   geometry snapshots, plus a feedback sheet with a WeChat QR.

## Decision

Sync the native/JNI layer and the user-visible behaviour fixes, adapting each fix
to the M3 Console. Do not merge the upstream `MainActivity` or its old UI.

- The sync lands as a **merge commit** so upstream ancestry survives.
- The preview channel moves to **385 / `1.6.2m-m3`**; stable stays at 156.
- Upstream-only chrome (feedback QR, old Console layouts) is not ported.
- Every ported behaviour must keep the M3 component rules and the existing
  Look / RenderSnapshot / FrameLookSession seams.

## Alternatives rejected

- **Merge upstream UI wholesale**: throws away the M3 Console, the brand shelf,
  and the fork's hardening; the conflict surface is the whole 9.6k-line file.
- **Native/JNI only**: leaves known user-visible defects (per-frame Original,
  portrait rebate, export self-check) unfixed in the fork.

## Consequences

- The fork stays intentionally divergent in the app layer; each upstream sync
  must be reviewed commit by commit rather than fast-forwarded.
- The version and `preview.json` change with the sync; `update.json` stays on
  the stable build.
- The sync and the MainActivity decomposition are separate changes, so a
  regression can be attributed to one or the other.

# ADR-0007: Adopt the 1.6.0e engine/JNI core, keep the M3 Console

## Status

Accepted.

## Context

Upstream `SAINTTaiYi/SpektraLab-Android` moved from 1.5.0e (`a2ffbc3`) to
1.6.0e (`08183cd`). The release adds live preview, batch import/export, hue
correction, 58/77 MP rendering, a RAW decode cache, and a set of crash and
diagnostics fixes. Most of that work is either native (`spektralab_jni.cpp`,
`spektralab_raw_jni.cpp`) or written against upstream's `MainActivity`; the
fork replaced that Activity with the Material 3 Console and its per-Frame
Look/undo model.

A direct merge of `MainActivity.java` would overwrite the Console or leave a
hybrid that neither side owns. The native core, by contrast, is a shared
contract with the engine and the donor APK.

## Decision

Merge upstream as a **merge commit** so ancestry is preserved, but resolve the
UI conflict in favour of the fork:

- Take the upstream native sources, `SpektraLabEngine`, `Probe`, `DebugSink`,
  `CrashTrail`, the LibRaw camera script and the raw build helper.
- Keep the fork's `MainActivity`, hardening, versioning and release channels.
- Use the upstream 1.6.0e APK as the donor for assets and `arm64-v8a` libraries.
- Publish the result as the preview build `326 / 1.6.0e-m3`; `update.json`
  remains stable at `156 / 1.4.4-m3a`.
- Port the 1.6.0e UI capabilities into the M3 Console as follow-up work.

## Consequences

The engine contract and crash handling are current, while the Console keeps
its existing behaviour. Live preview, batch import/export, hue controls and the
58/77 MP UI are not reachable yet; they need their own M3 tickets.

## Alternatives considered

- **Merge upstream `MainActivity` wholesale** — loses the M3 Console and the
  per-Frame Look/undo model.
- **Ancestry-only merge** — keeps GitHub's ahead/behind clean but ships none of
  the 1.6.0e engine fixes.
- **Port every UI feature in the same merge** — turns a core sync into a
  multi-week UI project and makes the engine change hard to isolate.

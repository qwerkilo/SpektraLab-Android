# 04 — Metering

Status: `implemented` (device pass pending)
Blocked by: 01.

## Scope

- Add `Metering` as the one table for the seven engine meters plus off.
- Store the choice in the Frame's Look (`mtr` in the compact JSON), so undo,
  frame switching and presets carry it.
- Write `auto_exposure` and `auto_exposure_method` in `LookRestatement`.
- Exposure picker in the Optics header.
- Default: RAW imports use `protect_highlights`; JPEG imports use off.

## Acceptance

- A JPEG with off sends `auto_exposure=false` and `balanced`.
- A JPEG with a chosen meter sends `auto_exposure=true` and that method.
- A RAW with no explicit value still meters with `balanced`.
- `MeteringCheck` and `LookRestatementCheck` pass.

## Remaining (04b)

Hue correction, histogram overlay and preview gamut choice are separate; they
need their own engine/display contracts.

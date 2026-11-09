# 03 — Presets and film card covers

Status: `partly implemented` (presets done; covers blocked)
Blocked by: 01.

## Scope

- Save the current compact Look JSON, apply it to the current Frame, share and
  import it. Applying is an explicit action, not a live edit.
- Add a film card cover image without changing the stock catalogue contract.
- Keep the preset payload versioned and forward-compatible.

## Presets

- `LookPreset` carries a name plus a detached `Look`; `Look.copyFrom` replaces
  every field in one settled edit.
- The film section head has a `预设` action. Save uses `askText`, apply records
  one undo and renders once, share sends the JSON as `text/plain`, import reads
  the clipboard.
- Presets live in `SharedPreferences` (`look_presets` / `list`), capped at 12.

## Film covers

Blocked: the repository has no stock cover art and the donor catalogue carries
no cover field. Do not fake a cover from the frame thumbnail. Source the art
first, then add `cover` to the catalogue contract.

## Acceptance

- Saving and applying the same preset restores the same Look values.
- Applying a preset records one undo step and renders once.
- A malformed or older payload is rejected with a readable message.
- `LookPresetCheck` passes.

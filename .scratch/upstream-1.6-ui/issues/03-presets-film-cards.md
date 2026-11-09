# 03 — Presets and film card covers

Status: `pending`
Blocked by: 01.

## Scope

- Save the current compact Look JSON, apply it to the current Frame, share and
  import it. Applying is an explicit action, not a live edit.
- Add a film card cover image without changing the stock catalogue contract.
- Keep the preset payload versioned and forward-compatible.

## Acceptance

- Saving and applying the same preset restores the same Look values.
- Applying a preset records one undo step and renders once.
- A malformed or older payload is rejected with a readable message.

# 04 — Colour, metering and histogram

Status: `pending`
Blocked by: 01.

## Scope

- Seven metering modes, wired to the engine parameter that already exists.
- Hue-correction control from the film profile; unsupported stocks show why.
- Histogram overlay toggle on the readout strip, computed from the displayed
  bitmap only.
- Preview gamut choice; export keeps the selected values.

## Acceptance

- Metering and hue changes render through `RenderSnapshot`, not a parallel
  parameter path.
- The histogram does not trigger a render and follows the displayed bitmap.
- Device check covers the toggle, both themes, and a large-text layout.

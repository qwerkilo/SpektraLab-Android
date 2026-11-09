# 04b — Hue, histogram and preview gamut

Status: `pending`
Blocked by: 04.

## Scope

- Hue correction from the film profile; unsupported stocks say why.
- Histogram overlay computed from the displayed bitmap only.
- Preview gamut choice; export keeps the selected values.

## Acceptance

- Metering and hue changes render through `RenderSnapshot`, not a parallel
  parameter path.
- The histogram does not trigger a render and follows the displayed bitmap.
- Device check covers the toggle, both themes, and a large-text layout.

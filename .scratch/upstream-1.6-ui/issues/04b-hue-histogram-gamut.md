# 04b — Hue, histogram and preview gamut

Status: `implemented` (device pass pending)
Blocked by: 04.

## Scope

- Hue correction from the film profile; unsupported stocks say why. **Done**:
  `core/HueCorrection` pins the seven untrusted stocks to off and the view
  sheet's `胶片原色` toggle writes the preference into the Look and
  `hue_correction` into the engine delta.
- Histogram overlay computed from the displayed bitmap only. **Done**: the
  spectrum readout cell toggles a 132×64 dp overlay; the bins come from
  `core/Histogram` and never trigger a render.
- Preview gamut choice; export keeps the selected values. **Done**: the view
  sheet chooses sRGB or Display P3; the preview bitmap is tagged on API 29+,
  and `snapshotFor(..., true)` forces sRGB for export.


## Acceptance

- Metering and hue changes render through `RenderSnapshot`, not a parallel
  parameter path.
- The histogram does not trigger a render and follows the displayed bitmap.
- Device check covers the toggle, both themes, and a large-text layout.

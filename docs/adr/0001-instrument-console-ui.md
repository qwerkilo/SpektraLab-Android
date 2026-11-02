---
status: accepted
date: 2026-11-02
---

# Instrument Console as the editor shell

The editor is currently a light-only page: a floating wordmark over the preview, a source strip, four panels, and a blue accent. We prototyped three shells — Darkroom, Contact Sheet, and Instrument — and chose Instrument's **Console** variant: a full-bleed preview, a readout strip, four tabs, and a film panel with stock chips and a spectral curve. Contact Sheet had the stronger metaphor but gave up numeric precision; Darkroom was immersive but hid the numbers. Console keeps the photograph primary while making the instrument identity legible, and its light and dark palettes can be designed independently.

Consequences:

- The UI needs a role-based palette with one light and one dark instance; views ask for roles, not raw colours.
- Theme switching rebuilds the view tree from the existing composition root and reattaches the loaded Frame and its Look. Activity recreation was rejected because it would drop the native session and the import.
- Contact Sheet's Δ column merges into the optics rows; Instrument's scan line becomes a read-only Scan inspect mode, not the main shell.
- Scan inspect reads the preview bitmap only. It never changes a Look, triggers a render, or affects export.
- No engine, parameter, tiling, or export change is implied by this UI work.

Considered options: Darkroom (rejected: atmosphere over precision), Contact Sheet (rejected as the main shell: strongest metaphor, weakest numeric reading), Instrument's Scan variant (kept as an inspect mode).

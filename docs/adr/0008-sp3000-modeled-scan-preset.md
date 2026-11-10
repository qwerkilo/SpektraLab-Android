# ADR 0008 — SP-3000 stays a modeled scan preset until measured data exists

- **Status:** Accepted
- **Date:** 2026-11-10

## Context

Wayfinder map #97 asked for the Fujifilm SP-3000 scanning stage. Research (#98) found
that the reference project carries no SP-3000 profile, no measured MTF, and no
published LED spectrum; its scanning stage is a generic port of `scanning.py`.
Our engine already has the black/white correction inputs and an output colour
space, but `scanner_lens_blur` is zeroed in preview, `scanner.unsharp_mask` has no
JSON key, black/white levels are engine constants, and the viewing illuminant
comes from the film profile. The Android engine rebuild chain is documented only
for Termux/NDK 28.2 and has not been verified on this Windows checkout.

## Decision

1. The SP-3000 preset remains a **modeled preset**: black-point and white-point
   correction on the engine's defaults (`0.98 / 0.01`), sRGB output, the profile's
   D50 kept, and no spatial sharpening.
2. **No native engine patch** is made for SP-3000 in this effort. Lens blur,
   unsharp mask, adjustable correction levels, and a scanner illuminant override
   stay deferred until measured SP-3000 data and a verified Android engine build
   exist.
3. The UI labels the preset `SP-3000（模型）` and states that it is modeled, not a
   measured scanner calibration.
4. Preview and export parity is required for the parameters that are exposed;
   because only the correction switches are exposed, that parity holds.

## Consequences

- The visible SP-3000 effect is limited to black/white correction. This is
  honest: no invented MTF, spectrum, or sharpening is presented as SP-3000.
- A future calibrated preset needs: illuminant SPD, lens MTF/blur, unsharp
  parameters, black/white levels, and an output curve measured from a real
  SP-3000, plus a native parity ticket that opens those parameters in both
  preview and export.
- Ticket #103 is resolved by this decision; #102 remains the device-verification
  ticket.

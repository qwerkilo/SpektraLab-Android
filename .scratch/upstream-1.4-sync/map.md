# Upstream 1.4.0 sync — map

## Destination

The M3 Console gains upstream 1.4.0's 22 optical parameters, Rebate/Date
secondary pages, Look/export changes, JNI/error detail, and catalogue
`targetPrint` — without losing the M3 shell, brand facet, spectral curve, Scan
inspect, or security fixes. A stable release stays blocked until the engine
side >16.7 MP rebate export defect is fixed or explicitly accepted.

## Decisions so far

- **Target line:** `feat/upstream-1.4-sync`, from `feat/m3-spec`, after the
  current M3 device audit baseline. Engine upstream remains `445e314`.
- **Source:** feature commit only; exclude the 1.4.0 announce commit,
  `update.json` channel, versionCode 100, and versionName 1.4.0.
- **UI:** keep M3; port capabilities. One M3 parameter strip + one slider for
  22 parameters; M3 secondary pages for Rebate and Date.
- **Look:** old Looks default new fields; never reset existing values.
- **Release:** fork version 69 / 1.4.0-m3; app code may merge, stable release
  blocked on the engine export defect.
- **Test seams:** pure JVM parameter/Look/error/catalogue checks, then the
  device audit.

## Open frontier

- Ticket 01 is the only implementation frontier now.
- Tickets 02–04 unlock after 01; ticket 05 after 02–04; ticket 06 after 05.

## Out of scope

- Out-of-tree engine changes, including the >16.7 MP bottom-print fix.
- Upstream old UI, announce channel, version 1.4.0.
- Compose or a new design system.

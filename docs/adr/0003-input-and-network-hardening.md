# 0003 · Input and network hardening

Status: accepted (2026-11-02)

## Context

The QA pass found three avoidable risks in the app shell:

- A content provider owns `DISPLAY_NAME`, and the RAW import path used it
  directly as a cache file name. A name containing `../` could write outside
  the cache directory.
- The manifest advertised a `VIEW` filter for `content` and `file` image URIs,
  but the activity never read the incoming intent. The filter was dead and
  widened the exported surface.
- `usesCleartextTraffic` was enabled for the whole app to reach the loopback
  debug sink, and driver telemetry was opt-out rather than opt-in.

## Decision

- **Cache names are sanitized.** `SafeFileName` keeps only the final path
  segment and falls back to a fixed name for `.`, `..`, empty, or NUL input.
- **The `VIEW` filter is removed.** Import stays on the system picker; the
  launcher remains the only exported entry point.
- **Cleartext is scoped to loopback.** A network security config permits
  cleartext only for `127.0.0.1` and `localhost`; `usesCleartextTraffic` is
  false for everything else.
- **Telemetry is opt-in.** `DeviceReport` defaults to disabled; the existing
  in-app toggle enables it.
- **Release links are allowlisted.** An announcement URL must be HTTPS on
  `github.com` under `SAINTTaiYi/SpektraLab-Android/` or on
  `update.workshop.skin`; anything else falls back to the GitHub release page.
- **Backup is disabled.** App preferences are not included in Android backup.

## Consequences

- A malicious provider can no longer choose the cache path.
- "Open with" no longer appears for image files until the app actually handles
  the incoming intent; the picker is the supported route.
- The Termux debug sink still works on loopback; non-loopback HTTP fails.
- Existing installs that already enabled telemetry keep it enabled.

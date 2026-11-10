# Test matrix

The project has no instrumentation suite. Pure checks are plain `main` classes
under `app/src/test/java/app/spektralab/`, plus three Python checks. Activity
wiring is covered by JUnit 4 + Robolectric (`PanelRowsTest`). Run them with the
Gradle commands in `AGENTS.md`; the manual matrix is
`docs/testing/manual-device-checklist.md`.

| Type | Covered by | What is pinned |
|---|---|---|
| Normal | `ConsoleLayoutCheck`, `FilmBrandCheck`, `GateScaleCheck`, `OpticsDeltaCheck`, `PaletteCheck`, `ScanSampleCheck`, `SpectrumCurveCheck`, `SpectrumSamplesCheck`, `ThemeModeCheck`, `LivePreviewGateCheck`, `LookPresetCheck`, `MeteringCheck`, `HistogramCheck`, `HueCorrectionCheck`, `M3ComponentCheck`, `GlassStyleCheck`, `ScanProfileCheck`, `BuiltInLookCheck`, `PresetAmountCheck`, `SessionDocumentCheck`, `ExportDigestCheck` | Width decision, brand shelf, gate/scale pairing, slider deltas, palette contrast, scan samples, spectra, theme round-trip, live-frame coalescing, preset round-trip, metering table, histogram bins, hue clamping, Material-only interactive widgets, glass tokens, SP-3000 preset switches, sparse built-in looks, preset strength blending, current-frame session round-trip, export length/digest |
| UI wiring | `PanelRowsTest` (JUnit 4 + Robolectric, SDK 35) | Date back corner/style rows, the Optics readout, the open Edge page across a theme switch, the Prism shell rows, the scan sheet preset plus both correction switches, undo visibility after a scan edit with no film, the built-in looks listed in the preset sheet, and the built-in strength draft |
| Boundary | `ConsoleLayoutCheck` (320/360/390/480/599/600/800 dp), `OpticsDeltaCheck` (min/max), `ScanSampleCheck` (clamp, 0/1), `SafeFileNameCheck` (`.`, `..`, empty, NUL), `GateScaleCheck` (frame aspect) | Edges do not spill into the next state or crash |
| Exception | `SpectrumCurveCheck` (null, NaN, gaps, ragged rows), `SpectrumSamplesCheck`, `ScanSampleCheck` (null/short/zero width), `SafeFileNameCheck`, `SpkErrorCheck`, `ReleaseUrlCheck` | Missing or hostile input degrades to an explicit unavailable/fallback state |
| Permission | `app/build/check-manifest.py` (30 checks), `app/build/check-apk.py`, manual checklist | Only `READ_EXTERNAL_STORAGE` (maxSdk 32) + `INTERNET`; one exported launcher; cleartext loopback only; backup off; Gradle and manifest SDK agree; merged APK target 36 |
| Data | `app/build/test-gen-stock-catalog.py`, `SpectrumCurveCheck`, `SpectrumSamplesCheck`, `FilmBrandCheck` | Profile generator keeps wavelengths/series, gaps stay gaps, brand counts match ids |
| Regression | `SafeFileNameCheck` (path traversal), `ReleaseUrlCheck` (update URL allowlist), `check-manifest.py` (dead VIEW, cleartext, backup), `check-apk.py` (merged target/backup/cleartext), `GateScaleCheck` (legacy scale pairs), `ChoiceRowCoverageCheck` (every choice list keeps an `edgeChoiceRow`), `LookRestatementCheck` (positive direct scan suppresses scanner correction), `SessionDocumentCheck` (malformed/future-version document rejected), `ExportDigestCheck` (one-byte mismatch rejected), `PanelRowsTest` (dismissed strength sheet leaves the Look) | The QA fixes, earlier gate bugs, panel-row omissions, the positive direct-scan E-9000, and the session/export failure paths stay fixed |

## Not covered automatically

- `MainActivity` layout, gestures, and Frame switching: no instrumentation
  runner; Robolectric covers panel-row construction only, and the manual
  checklist covers the rest.
- Native decode/render/export: owned by the out-of-tree engine; device checks
  cover the user-visible path.
- Runtime permission grants: the app uses the system picker and asks for no
  runtime permission; the manifest policy is the enforceable layer.

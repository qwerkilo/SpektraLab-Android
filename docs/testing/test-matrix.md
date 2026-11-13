# Test matrix

The project has no instrumentation suite. Pure checks are plain `main` classes
under `app/src/test/java/app/spektralab/`, plus three Python checks. Activity
wiring is covered by JUnit 4 + Robolectric (`PanelRowsTest`). Run them with the
Gradle commands in `AGENTS.md`; the manual matrix is
`docs/testing/manual-device-checklist.md`.

| Type | Covered by | What is pinned |
|---|---|---|
| Normal | `ConsoleLayoutCheck`, `FilmBrandCheck`, `GateScaleCheck`, `OpticsDeltaCheck`, `PaletteCheck`, `ScanSampleCheck`, `SpectrumCurveCheck`, `SpectrumSamplesCheck`, `ThemeModeCheck`, `LivePreviewGateCheck`, `LookPresetCheck`, `MeteringCheck`, `HistogramCheck`, `HueCorrectionCheck`, `M3ComponentCheck`, `GlassStyleCheck`, `ScanProfileCheck`, `BuiltInLookCheck`, `PresetAmountCheck`, `SessionDocumentCheck`, `ExportDigestCheck`, `GalleryExportCheck`, `FormatCardLayoutCheck`, `ReopenDecisionCheck`, `RenderEpochCheck`, `ExportMatchCheck`, `ExportSheetCheck`, `BatchPlanCheck` | Width decision, brand shelf, gate/scale pairing, slider deltas, palette contrast, scan samples, spectra, theme round-trip, live-frame coalescing, preset round-trip, metering table, histogram bins, hue clamping, Material-only interactive widgets, glass tokens, SP-3000 preset switches, sparse built-in looks, preset strength blending, current-frame session round-trip, export length/digest, verified gallery publish, Scale card growth with font scale, per-frame 原图 reopen, stale render epoch, export/canvas correlation, export sheet order/routing and note, batch frame order and progress |
| UI wiring | `PanelRowsTest`, `ImportPickerTest` (JUnit 4 + Robolectric, SDK 35) | Date back corner/style rows, the Optics readout, the open Edge page across a theme switch, the Prism shell rows, the scan sheet preset plus both correction switches, undo visibility after a scan edit with no film, the built-in looks listed in the preset sheet, the built-in strength draft, the spectrum cell toggling the stage histogram, Scale card growth at 2.0× font, and the import chains (gallery/Documents order, persistable grant, fallback, no-picker) |
| Boundary | `ConsoleLayoutCheck` (320/360/390/480/599/600/800 dp), `OpticsDeltaCheck` (min/max), `ScanSampleCheck` (clamp, 0/1), `SafeFileNameCheck` (`.`, `..`, empty, NUL), `GateScaleCheck` (frame aspect), `FormatCardLayoutCheck` (0.85/1.3/2.0 font scale) | Edges do not spill into the next state or crash; Scale cards never shrink below their base |
| Exception | `SpectrumCurveCheck` (null, NaN, gaps, ragged rows), `SpectrumSamplesCheck`, `ScanSampleCheck` (null/short/zero width), `SafeFileNameCheck`, `SpkErrorCheck`, `ReleaseUrlCheck`, `GalleryExportCheck` (copy, read-back and publish failures; failing discard) | Missing or hostile input degrades to an explicit unavailable/fallback state; a failed export never leaves a published or pending row |
| Permission | `app/build/check-manifest.py` (30 checks), `app/build/check-apk.py`, manual checklist | Only `READ_EXTERNAL_STORAGE` (maxSdk 32) + `INTERNET`; one exported launcher; cleartext loopback only; backup off; Gradle and manifest SDK agree; merged APK target 36 |
| Data | `app/build/test-gen-stock-catalog.py`, `SpectrumCurveCheck`, `SpectrumSamplesCheck`, `FilmBrandCheck` | Profile generator keeps wavelengths/series, gaps stay gaps, brand counts match ids |
| Regression | `SafeFileNameCheck` (path traversal), `ReleaseUrlCheck` (update URL allowlist), `check-manifest.py` (dead VIEW, cleartext, backup), `check-apk.py` (merged target/backup/cleartext), `GateScaleCheck` (legacy scale pairs), `ChoiceRowCoverageCheck` (every choice list keeps an `edgeChoiceRow`), `LookRestatementCheck` (positive direct scan suppresses scanner correction), `PanelRowsTest` (dismissed strength sheet leaves the Look) | The QA fixes, earlier gate bugs, panel-row omissions, the positive direct-scan E-9000, and the session/export failure paths stay fixed |

## Not covered automatically

- `MainActivity` layout, gestures, and Frame switching: no instrumentation
  runner; Robolectric covers panel-row construction only, and the manual
  checklist covers the rest.
- Native decode/render/export: owned by the out-of-tree engine; device checks
  cover the user-visible path.
- Runtime permission grants: the app uses the system picker and asks for no
  runtime permission; the manifest policy is the enforceable layer.

# Import pipeline map

Spec: `docs/specs/import-pipeline.md`
Base: `master` at `4e85643`

1. **01 `importer.StillDecoder`** — decode, EXIF, scale. No engine.
2. **02 `importer.RawDecoder`** — LibRaw session open + preview, blocked by 01.
3. **03 `importer.ImportController`** — packing, preview publish, session,
   failure text; blocked by 01/02.

Target: `MainActivity` loses the decode/EXIF helpers; `ImportBudget` stays the
one size rule.

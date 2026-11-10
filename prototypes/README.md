# UI prototypes (throwaway)

## Selected: Prism Table

Question: **which Prism HUD refinement should become the M3 console's visual direction?**

Run:

```bash
python prototypes/serve.py
```

Open `http://127.0.0.1:8788/ui-prism.html?variant=C`.

| Key | Refinement | Structure |
|---|---|---|
| `A` | Prism Air | Lighter chrome, segmented bottom dock, shorter airy sheet |
| `B` | Prism Beam | Right-side vertical rail, narrow sheet, more image |
| `C` | Prism Table | **Selected** — 2×2 readout table, slim text dock, quiet glass |

Theme: `?theme=light|dark`, the switcher's theme button, or `T`.
The state line under the switcher shows the direction, theme, film, exposure, grain and scan fraction.

`ui-liquid-glass.html` keeps the earlier three-language round (Lumen Deck / Refraction Rail / Prism HUD).
`ui-prototype.html` keeps the first round (Darkroom / Contact Sheet / Instrument).
`ui-instrument.html` keeps the Instrument round (Console / Ledger / Scan).

The winning direction is implemented in the real M3 console via
`docs/specs/prism-table-console.md`; this folder stays out of production.

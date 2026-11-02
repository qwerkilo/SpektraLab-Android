# UI prototypes (throwaway)

## Current: Instrument round

Question: **which refinement of the Instrument direction should ship, and does it survive light mode?**

Run:

```bash
python prototypes/serve.py
```

Open `http://127.0.0.1:8788/ui-instrument.html?variant=A`.

| Key | Variant | Structure |
|---|---|---|
| `A` | Console | C tightened: readout strip, density curve, larger type |
| `B` | Ledger | Vertical index rail, measurement table, Δ column |
| `C` | Scan | Draggable scan line, RGB / λ readout, spectrum + 2×2 matrix |

Theme: the pill in each phone header toggles light/dark. `?theme=light` or `?theme=dark` sets it from the URL; `T` toggles from the keyboard.

The state line under the switcher shows the active theme, tab, film, slider values, edge toggles, and scan position.

## Earlier: three visual languages

`ui-prototype.html` keeps the first round: A Darkroom, B Contact Sheet, C Instrument. It is superseded by the Instrument round.

The winning direction gets rewritten into `MainActivity.java`; this folder stays out of production.

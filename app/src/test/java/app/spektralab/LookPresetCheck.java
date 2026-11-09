package app.spektralab;

import org.json.JSONObject;

/** Round-trip and rejection rules for the shared Look preset payload. */
public final class LookPresetCheck {

    private static int checks;

    private LookPresetCheck() {
    }

    public static void main(String[] args) {
        final Look look = new Look();
        look.filmStock = "kodak_portra_400";
        look.printStock = Look.DEFAULT_PRINT_STOCK;
        look.printExposure = 1.75f;
        look.overscanOn = true;
        look.overscanFormat = "135";
        look.geometryTurns = 1;

        final LookPreset preset = new LookPreset("  Portra 135  ", look);
        ok("name is trimmed", "Portra 135".equals(preset.name()));
        ok("film survives", "kodak_portra_400".equals(preset.look().filmStock));
        ok("exposure survives", Math.abs(preset.look().printExposure - 1.75f) < 0.001f);
        ok("gate survives", "135".equals(preset.look().overscanFormat));
        ok("rotation survives", preset.look().geometryTurns == 1);

        final JSONObject json = preset.toJson();
        ok("version is written", json.optInt("version") == 1);
        final LookPreset back = LookPreset.fromJson(json);
        ok("round-trip reads a preset", back != null);
        ok("round-trip keeps the name", "Portra 135".equals(back.name()));
        ok("round-trip keeps the film", "kodak_portra_400".equals(back.look().filmStock));
        ok("round-trip keeps the gate", back.look().overscanOn
                && "135".equals(back.look().overscanFormat));

        ok("a payload without look is rejected", LookPreset.fromJson(new JSONObject()) == null);
        ok("null is rejected", LookPreset.fromJson(null) == null);

        final JSONObject emptyName = new JSONObject();
        final JSONObject emptyLook = new JSONObject();
        try {
            emptyName.put("name", "");
            emptyName.put("look", emptyLook);
        } catch (org.json.JSONException ignored) { }
        final LookPreset unnamed = LookPreset.fromJson(emptyName);
        ok("an unnamed preset still parses", unnamed != null);
        ok("an empty Look has no film", unnamed.look().filmStock == null);

        System.out.println("LookPresetCheck: " + checks + " checks passed");
    }

    private static void ok(String what, boolean value) {
        ++checks;
        if (!value) {
            throw new AssertionError(what);
        }
    }
}

package app.spektralab;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The shipped one-tap film looks.
 *
 * <p>A built-in look is a sparse overlay on the current Look: it names a film
 * and a paper, then authors only the fields that make the look recognisable.
 * Applying one therefore keeps the user's composition, crop, date and view
 * settings instead of replacing the whole Look. This is deliberately different
 * from a {@link LookPreset}, which is a complete Look snapshot.
 */
final class BuiltInLook {

    private static final String DEFINITIONS = """
            [
              {
                "id": "portra_400",
                "name": "Portra 400",
                "film": "kodak_portra_400",
                "print": "kodak_portra_endura",
                "overlay": {
                  "grain": 0.95,
                  "halation": 1.0,
                  "mtr": "center_weighted"
                }
              },
              {
                "id": "gold_200",
                "name": "Gold 200",
                "film": "kodak_gold_200",
                "print": "kodak_ektacolor_edge",
                "overlay": {
                  "grain": 1.1,
                  "halation": 1.0
                }
              },
              {
                "id": "ektar_100",
                "name": "Ektar 100",
                "film": "kodak_ektar_100",
                "print": "kodak_supra_endura",
                "overlay": {
                  "grain": 0.7,
                  "halation": 0.7
                }
              },
              {
                "id": "tri_x_400",
                "name": "Tri-X 400",
                "film": "kodak_tri_x_400",
                "print": "ilford_multigrade_iv_rc",
                "overlay": {
                  "grain": 1.15,
                  "halation": 0.9
                }
              },
              {
                "id": "kodachrome_64",
                "name": "Kodachrome 64",
                "film": "kodak_kodachrome_64",
                "print": "kodak_portra_endura",
                "overlay": {
                  "grain": 0.9,
                  "halation": 0.6
                }
              },
              {
                "id": "cinestill_800t",
                "name": "CineStill 800T",
                "film": "cinestill_800t",
                "print": "kodak_2383",
                "overlay": {
                  "grain": 1.25,
                  "halation": 2.2,
                  "boost": 1.0,
                  "mtr": "center_weighted"
                }
              }
            ]
            """;

    private static final List<BuiltInLook> ALL = parseAll();

    private final String id;
    private final String name;
    private final String filmId;
    private final String printId;
    private final JSONObject overlay;

    private BuiltInLook(String id, String name, String filmId, String printId,
                        JSONObject overlay) {
        this.id = id;
        this.name = name;
        this.filmId = filmId;
        this.printId = printId;
        this.overlay = overlay;
    }

    /** Every shipped look, in the order the picker shows them. */
    static List<BuiltInLook> all() {
        return ALL;
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    String filmId() {
        return filmId;
    }

    String printId() {
        return printId;
    }

    /**
     * The full Look this built-in produces from {@code base}. Omitted fields
     * keep the base value, so a look never resets the user's composition.
     */
    Look apply(Look base) {
        final JSONObject merged = (base == null ? new Look() : base.copy()).toJson();
        merge(merged, overlay);
        try {
            merged.put("film", filmId);
            merged.put("print", printId);
        } catch (org.json.JSONException impossible) {
            throw new IllegalStateException("built-in look has no film", impossible);
        }
        return Look.fromJson(merged);
    }

    private static void merge(JSONObject into, JSONObject overlay) {
        final java.util.Iterator<String> keys = overlay.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            try {
                into.put(key, overlay.get(key));
            } catch (org.json.JSONException impossible) {
                throw new IllegalStateException("bad built-in overlay key " + key, impossible);
            }
        }
    }

    private static List<BuiltInLook> parseAll() {
        try {
            final JSONArray array = new JSONArray(DEFINITIONS);
            final List<BuiltInLook> looks = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); ++i) {
                final JSONObject o = array.getJSONObject(i);
                looks.add(new BuiltInLook(
                        o.getString("id"),
                        o.getString("name"),
                        o.getString("film"),
                        o.getString("print"),
                        o.getJSONObject("overlay")));
            }
            return Collections.unmodifiableList(looks);
        } catch (org.json.JSONException e) {
            throw new IllegalStateException("built-in look table is malformed", e);
        }
    }
}

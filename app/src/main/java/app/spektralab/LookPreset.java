package app.spektralab;

import org.json.JSONObject;

/** A named Look payload for save, share and import. */
final class LookPreset {
    private static final int VERSION = 1;

    private final String name;
    private final Look look;

    LookPreset(String name, Look look) {
        this.name = name == null ? "" : name.trim();
        this.look = look == null ? new Look() : look.copy();
    }

    String name() {
        return name;
    }

    Look look() {
        return look;
    }

    JSONObject toJson() {
        final JSONObject o = new JSONObject();
        try {
            o.put("version", VERSION);
            o.put("name", name);
            o.put("look", look.toJson());
        } catch (org.json.JSONException ignored) { }
        return o;
    }

    /** Read one preset; null when the payload has no Look object. */
    static LookPreset fromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        final JSONObject lookJson = o.optJSONObject("look");
        if (lookJson == null) {
            return null;
        }
        return new LookPreset(o.optString("name", ""), Look.fromJson(lookJson));
    }
}

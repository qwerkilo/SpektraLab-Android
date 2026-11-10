package app.spektralab;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/**
 * Lightroom-style preset strength: cross-fade a base Look toward a full Look.
 *
 * <p>Continuous leaves (numbers and numeric arrays) are interpolated.
 * Categorical leaves (booleans, strings, and the integer seeds/turns) cannot be
 * half-applied, so they switch at 50 %. The walk is over the base object's
 * keys, which is always a complete Look, so a sparse target simply keeps the
 * base value for every field it does not author.
 */
final class PresetAmount {

    /** Numeric leaves that identify a state rather than measure one. */
    private static final Set<String> CATEGORICAL_NUMERIC_KEYS = Set.of(
            "version", "cseed", "fseed", "gturn");

    private PresetAmount() {
    }

    /** A detached Look at {@code amount} in [0, 1]. */
    static Look blend(Look base, Look full, float amount) {
        final float t = Math.max(0f, Math.min(1f, amount));
        final JSONObject from = (base == null ? new Look() : base.copy()).toJson();
        final JSONObject to = (full == null ? new Look() : full.copy()).toJson();
        return Look.fromJson(blendObject(from, to, t));
    }

    private static JSONObject blendObject(JSONObject base, JSONObject full, float t) {
        final JSONObject out = new JSONObject();
        final java.util.Iterator<String> keys = base.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            try {
                if (!full.has(key)) {
                    out.put(key, base.get(key));
                } else {
                    out.put(key, blendValue(key, base.get(key), full.get(key), t));
                }
            } catch (org.json.JSONException impossible) {
                throw new IllegalStateException("bad Look key " + key, impossible);
            }
        }
        return out;
    }

    private static Object blendValue(String key, Object base, Object full, float t) {
        if (base instanceof JSONObject && full instanceof JSONObject) {
            return blendObject((JSONObject) base, (JSONObject) full, t);
        }
        if (base instanceof JSONArray && full instanceof JSONArray) {
            return blendArray(key, (JSONArray) base, (JSONArray) full, t);
        }
        if (base instanceof Number && full instanceof Number) {
            if (CATEGORICAL_NUMERIC_KEYS.contains(key)) {
                return t < 0.5f ? base : full;
            }
            final double a = ((Number) base).doubleValue();
            final double b = ((Number) full).doubleValue();
            return a + (b - a) * t;
        }
        return t < 0.5f ? base : full;
    }

    private static JSONArray blendArray(String key, JSONArray base, JSONArray full, float t) {
        if (base.length() != full.length()) {
            return t < 0.5f ? base : full;
        }
        final JSONArray out = new JSONArray();
        for (int i = 0; i < base.length(); ++i) {
            try {
                out.put(blendValue(key, base.get(i), full.get(i), t));
            } catch (org.json.JSONException impossible) {
                throw new IllegalStateException("bad Look array " + key, impossible);
            }
        }
        return out;
    }
}

package app.spektralab;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The print-view adjustment layer: white balance, tone, colour, and four-zone
 * grading. It is part of a Frame's Look, not a second global setting, so a
 * Frame switch and an undo restore the same values the preview showed.
 *
 * <p>{@link #toNative()} is the only place that knows the engine's array
 * order. The JNI setter takes that array; preview and export must pass the
 * same one.
 */
public final class ViewAdjust {
    static final float VIGNETTE_MID = 50f;

    public enum Group {
        WHITE_BALANCE("白平衡", "WHITE BALANCE"),
        TONE("亮度", "TONE"),
        COLOUR("颜色", "COLOUR"),
        GRADING("分级", "GRADING");

        public final String label;
        public final String english;

        Group(String label, String english) {
            this.label = label;
            this.english = english;
        }
    }

    public enum Field {
        TEMPERATURE(Group.WHITE_BALANCE, "色温", "K", -100f, 100f, 0f),
        TINT(Group.WHITE_BALANCE, "色调", "", -100f, 100f, 0f),

        EXPOSURE(Group.TONE, "曝光", "EV", -3f, 3f, 0f),
        CONTRAST(Group.TONE, "对比度", "", -100f, 100f, 0f),
        BRIGHTNESS(Group.TONE, "亮度", "", -100f, 100f, 0f),
        HIGHLIGHTS(Group.TONE, "高光", "", -100f, 100f, 0f),
        SHADOWS(Group.TONE, "阴影", "", -100f, 100f, 0f),
        BLACK(Group.TONE, "黑场", "", 0f, 50f, 0f),
        WHITE(Group.TONE, "白场", "", 0f, 50f, 0f),

        SATURATION(Group.COLOUR, "饱和度", "", -100f, 100f, 0f),
        VIBRANCE(Group.COLOUR, "自然饱和度", "", -100f, 100f, 0f),

        SHADOW_HUE(Group.GRADING, "阴影 色相", "°", 0f, 360f, 0f),
        SHADOW_SAT(Group.GRADING, "阴影 饱和", "", 0f, 100f, 0f),
        SHADOW_LUM(Group.GRADING, "阴影 明度", "", -100f, 100f, 0f),
        MID_HUE(Group.GRADING, "中间调 色相", "°", 0f, 360f, 0f),
        MID_SAT(Group.GRADING, "中间调 饱和", "", 0f, 100f, 0f),
        MID_LUM(Group.GRADING, "中间调 明度", "", -100f, 100f, 0f),
        HIGH_HUE(Group.GRADING, "高光 色相", "°", 0f, 360f, 0f),
        HIGH_SAT(Group.GRADING, "高光 饱和", "", 0f, 100f, 0f),
        HIGH_LUM(Group.GRADING, "高光 明度", "", -100f, 100f, 0f),
        GLOBAL_HUE(Group.GRADING, "全局 色相", "°", 0f, 360f, 0f),
        GLOBAL_SAT(Group.GRADING, "全局 饱和", "", 0f, 100f, 0f),
        GLOBAL_LUM(Group.GRADING, "全局 明度", "", -100f, 100f, 0f);

        public final Group group;
        public final String label;
        public final String unit;
        public final float min, max, defaultValue;

        Field(Group group, String label, String unit, float min, float max, float defaultValue) {
            this.group = group;
            this.label = label;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
        }

        float get(ViewAdjust a) {
            switch (this) {
                case TEMPERATURE: return a.temperature;
                case TINT: return a.tint;
                case EXPOSURE: return a.exposure;
                case CONTRAST: return a.contrast;
                case BRIGHTNESS: return a.brightness;
                case HIGHLIGHTS: return a.highlights;
                case SHADOWS: return a.shadows;
                case BLACK: return a.black;
                case WHITE: return a.white;
                case SATURATION: return a.saturation;
                case VIBRANCE: return a.vibrance;
                case SHADOW_HUE: return a.cbHue[0];
                case SHADOW_SAT: return a.cbSat[0];
                case SHADOW_LUM: return a.cbLum[0];
                case MID_HUE: return a.cbHue[1];
                case MID_SAT: return a.cbSat[1];
                case MID_LUM: return a.cbLum[1];
                case HIGH_HUE: return a.cbHue[2];
                case HIGH_SAT: return a.cbSat[2];
                case HIGH_LUM: return a.cbLum[2];
                case GLOBAL_HUE: return a.cbHue[3];
                case GLOBAL_SAT: return a.cbSat[3];
                case GLOBAL_LUM: return a.cbLum[3];
                default: throw new IllegalArgumentException(name());
            }
        }

        void set(ViewAdjust a, float value) {
            final float v = Math.max(min, Math.min(max, value));
            switch (this) {
                case TEMPERATURE: a.temperature = v; break;
                case TINT: a.tint = v; break;
                case EXPOSURE: a.exposure = v; break;
                case CONTRAST: a.contrast = v; break;
                case BRIGHTNESS: a.brightness = v; break;
                case HIGHLIGHTS: a.highlights = v; break;
                case SHADOWS: a.shadows = v; break;
                case BLACK: a.black = v; break;
                case WHITE: a.white = v; break;
                case SATURATION: a.saturation = v; break;
                case VIBRANCE: a.vibrance = v; break;
                case SHADOW_HUE: a.cbHue[0] = v; break;
                case SHADOW_SAT: a.cbSat[0] = v; break;
                case SHADOW_LUM: a.cbLum[0] = v; break;
                case MID_HUE: a.cbHue[1] = v; break;
                case MID_SAT: a.cbSat[1] = v; break;
                case MID_LUM: a.cbLum[1] = v; break;
                case HIGH_HUE: a.cbHue[2] = v; break;
                case HIGH_SAT: a.cbSat[2] = v; break;
                case HIGH_LUM: a.cbLum[2] = v; break;
                case GLOBAL_HUE: a.cbHue[3] = v; break;
                case GLOBAL_SAT: a.cbSat[3] = v; break;
                case GLOBAL_LUM: a.cbLum[3] = v; break;
                default: throw new IllegalArgumentException(name());
            }
        }
    }

    float temperature, tint, exposure, contrast, brightness, highlights, shadows, black, white;
    float saturation, vibrance;
    final float[] cbHue = new float[4];
    final float[] cbSat = new float[4];
    final float[] cbLum = new float[4];

    ViewAdjust copy() {
        final ViewAdjust out = new ViewAdjust();
        out.load(toJson());
        return out;
    }

    float get(Field field) {
        return field.get(this);
    }

    void set(Field field, float value) {
        field.set(this, value);
    }

    void reset(Group group) {
        for (Field field : Field.values()) {
            if (field.group == group) field.set(this, field.defaultValue);
        }
    }

    boolean isDefault() {
        for (Field field : Field.values()) {
            if (Math.abs(field.get(this) - field.defaultValue) > 0.0001f) return false;
        }
        return true;
    }

    /** The exact array order the JNI setter reads. */
    double[] toNative() {
        return new double[]{
                temperature, tint, exposure, contrast, brightness, saturation,
                highlights, shadows, black, white, 0f, VIGNETTE_MID,
                cbHue[0], cbSat[0], cbLum[0], cbHue[1], cbSat[1], cbLum[1],
                cbHue[2], cbSat[2], cbLum[2], cbHue[3], cbSat[3], cbLum[3],
                vibrance
        };
    }

    JSONObject toJson() {
        final JSONObject out = new JSONObject();
        try {
            for (Field field : Field.values()) out.put(field.name().toLowerCase(java.util.Locale.ROOT), field.get(this));
            final JSONArray hue = new JSONArray(), sat = new JSONArray(), lum = new JSONArray();
            for (int i = 0; i < 4; ++i) {
                hue.put(cbHue[i]);
                sat.put(cbSat[i]);
                lum.put(cbLum[i]);
            }
            out.put("cb_hue", hue);
            out.put("cb_sat", sat);
            out.put("cb_lum", lum);
        } catch (org.json.JSONException ignored) {
        }
        return out;
    }

    void load(JSONObject o) {
        if (o == null) return;
        for (Field field : Field.values()) {
            if (o.has(field.name().toLowerCase(java.util.Locale.ROOT))) {
                field.set(this, (float) o.optDouble(field.name().toLowerCase(java.util.Locale.ROOT),
                        field.defaultValue));
            }
        }
        readArray(o.optJSONArray("cb_hue"), cbHue);
        readArray(o.optJSONArray("cb_sat"), cbSat);
        readArray(o.optJSONArray("cb_lum"), cbLum);
    }

    private static void readArray(JSONArray a, float[] out) {
        if (a == null) return;
        for (int i = 0; i < out.length; ++i) out[i] = (float) a.optDouble(i, 0f);
    }

    static String valueText(float value, String unit) {
        return unit.isEmpty() ? String.format(java.util.Locale.US, "%.1f", value)
                : String.format(java.util.Locale.US, "%.0f %s", value, unit);
    }
}

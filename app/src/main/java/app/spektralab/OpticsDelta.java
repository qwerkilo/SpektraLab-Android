package app.spektralab;

/**
 * The display-only Δ on an optics row, plus the contract every optics control
 * reads: how far the current value sits from that row's panel default.
 *
 * <p>This is a formatting seam, not a second state. It holds the parameter's
 * name, unit, range, step, and default, and turns a value/default pair into
 * the signed two-decimal string the row shows. Nothing here is sent to the
 * engine by itself, saved into a Look, or kept beside the value it describes:
 * the Look owns the value, and the wire name here is the JSON key it uses.
 */
public final class OpticsDelta {
    /** The calibrated exposure multiplier. */
    public static final float EXPOSURE_DEFAULT = 1.0f;
    /** The calibrated grain amount. */
    public static final float GRAIN_DEFAULT = 1.0f;
    /** The calibrated halation amount. */
    public static final float HALATION_DEFAULT = 1.0f;
    /** The calibrated glare percentage. */
    public static final float GLARE_DEFAULT = 3.0f;

    private static final float DEFAULT_EPSILON = 0.0005f;

    private OpticsDelta() { }

    /**
     * One calibrated optics parameter. The slider reads its range and step
     * from here, the Looks page reads its label and unit, the reset asks
     * {@link #isDefault(float)}, and the Look JSON uses {@link #wire}.
     * Naming all of it once keeps the panel, the save format, and the checks
     * on the same numbers.
     *
     * <p>The order is the strip order and the engine's own knob order.
     */
    public enum Param {
        EXPOSURE("exp", "\u653e\u5927\u673a\u66dd\u5149", "E", "EV", 0.25f, 4.0f, EXPOSURE_DEFAULT, 0.01f),
        HIGHLIGHT_ROOM("highlight", "\u9ad8\u5149\u4fdd\u62a4", "H", "EV", 0.0f, 3.0f, 0.0f, 0.01f),
        SHADOW_ROOM("shadow", "\u6697\u90e8\u4fdd\u62a4", "S", "EV", 0.0f, 3.0f, 0.0f, 0.01f),
        GRAIN("grain", "\u9897\u7c92", "G", "\u00d7", 0.0f, 2.0f, GRAIN_DEFAULT, 0.01f),
        HALATION("halation", "\u5149\u6655", "\u25c9", "\u00d7", 0.0f, 4.0f, HALATION_DEFAULT, 0.01f),
        HALATION_BOOST("boost", "\u5149\u6655\u589e\u5f3a", "+", "EV", 0.0f, 8.0f, 0.0f, 0.01f),
        HALATION_SCATTER("scatter", "\u4e73\u5242\u6563\u5c04", "~", "\u00d7", 0.0f, 1.0f, 1.0f, 0.01f),
        GLARE("glare", "\u7729\u5149", "\u2727", "\u00d7", 0.0f, 12.0f, GLARE_DEFAULT, 0.01f),
        LENS_BLUR("lens", "\u955c\u5934\u6a21\u7cca", "\u25cc", "\u00b5m", 0.0f, 200.0f, 0.0f, 1.0f),
        Y_FILTER("yf", "\u9ec4\u6ee4\u955c", "Y", "CC", -1.0f, 1.0f, 0.0f, 0.01f),
        M_FILTER("mf", "\u54c1\u7ea2\u6ee4\u955c", "M", "CC", -1.0f, 1.0f, 0.0f, 0.01f),
        FILTER_SCALE("fscale", "\u6ee4\u955c\u5f3a\u5ea6", "C", "CC", 1.0f, 60.0f, 1.0f, 0.1f),
        PREFLASH("preflash", "\u9884\u95ea", "\u26a1", "\u00d7", 0.0f, 1.0f, 0.0f, 0.01f),
        SHADOW_ROLLOFF("rolloff", "\u66f2\u7ebf\u67d4\u5ea6", "\u223f", "\u00d7", 1.0f, 4.0f, 2.0f, 0.01f),
        MAX_LIFT("lift", "\u63d0\u5347\u4e0a\u9650", "L", "EV", 0.25f, 12.0f, 4.0f, 0.01f),
        HALATION_ON("halon", "\u5149\u6655\u5f00\u5173", "\u25c9", "\u00d7", 0.0f, 1.0f, 1.0f, 1.0f),
        ANTIHALATION_REMOVED("ahrem", "\u53bb\u6297\u5149\u6655", "\u2298", "\u00d7", 0.0f, 1.0f, 0.0f, 1.0f),
        GRAIN_ON("grainon", "\u9897\u7c92\u5f00\u5173", "g", "\u00d7", 0.0f, 1.0f, 1.0f, 1.0f),
        COUPLERS_ON("coupon", "\u5c42\u95f4\u6291\u5236", "\u21c4", "\u00d7", 0.0f, 1.0f, 1.0f, 1.0f),
        SCANNER_WHITE("scw", "\u626b\u63cf\u767d", "W", "\u00d7", 0.0f, 1.0f, 0.0f, 1.0f),
        SCANNER_BLACK("scb", "\u626b\u63cf\u9ed1", "B", "\u00d7", 0.0f, 1.0f, 0.0f, 1.0f),
        EDR_ON("edr", "\u52a8\u6001\u8303\u56f4", "HDR", "\u00d7", 0.0f, 1.0f, 0.0f, 1.0f);

        /** Stable wire name: the Look JSON key the engine's parameters use. */
        public final String wire;
        /** The visible name TalkBack announces. */
        public final String label;
        /** The short mark on the parameter chip. */
        public final String glyph;
        /** The visible unit TalkBack also announces. */
        public final String unit;
        /** The real float range the slider exposes to accessibility. */
        public final float min, max;
        /** The value this row calls neutral. */
        public final float defaultValue;
        /** The smallest change a drag needs to make. */
        public final float step;

        Param(String wire, String label, String glyph, String unit,
              float min, float max, float defaultValue, float step) {
            this.wire = wire;
            this.label = label;
            this.glyph = glyph;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.step = step;
        }

        /** True when the row already reads as its default. */
        public boolean isDefault(float value) {
            return OpticsDelta.isDefault(value, defaultValue);
        }

        /** Keep a restored or dragged value inside this row's real range. */
        public float clamp(float value) {
            return Math.max(min, Math.min(max, value));
        }
    }

    /** The Wratten filters the engine carries, plus the taking lens unfiltered. */
    public enum CameraFilter {
        NONE("", "\u65e0"),
        W8("w8", "W8 \u9ec4"),
        W11("w11", "W11 \u9ec4\u7eff"),
        W12("w12", "W12 \u6df1\u9ec4"),
        W15("w15", "W15 \u6df1\u9ec4"),
        W21("w21", "W21 \u6a59"),
        W25("w25", "W25 \u7ea2"),
        W47("w47", "W47 \u84dd"),
        W58("w58", "W58 \u7eff");

        /** The engine's filter id; an empty id means no filter. */
        public final String wire;
        /** The visible name TalkBack announces. */
        public final String label;

        CameraFilter(String wire, String label) {
            this.wire = wire;
            this.label = label;
        }

        /** The stored id, or {@link #NONE} when an older Look names another. */
        public static CameraFilter fromWire(String wire) {
            if (wire != null) {
                for (CameraFilter filter : values()) {
                    if (filter.wire.equals(wire)) return filter;
                }
            }
            return NONE;
        }
    }

    /** The scene measurement highlight/shadow protection is calculated on. */
    public enum ProtectionNorm {
        POWER("power", "\u5e42\u5f8b power"),
        LUMINANCE("y", "\u4eae\u5ea6 y"),
        PEAK("max", "\u5cf0\u503c max");

        /** The engine's norm id. */
        public final String wire;
        /** The visible name TalkBack announces. */
        public final String label;

        ProtectionNorm(String wire, String label) {
            this.wire = wire;
            this.label = label;
        }

        /** The stored id, or {@link #POWER} when an older Look names another. */
        public static ProtectionNorm fromWire(String wire) {
            if (wire != null) {
                for (ProtectionNorm norm : values()) {
                    if (norm.wire.equals(wire)) return norm;
                }
            }
            return POWER;
        }
    }

    /**
     * A signed two-decimal difference, or {@code 0.00} at the default.
     * Locale.US keeps the decimal point stable under a Chinese UI locale.
     */
    public static String text(float value, float defaultValue) {
        final String signed = String.format(java.util.Locale.US, "%+.2f", value - defaultValue);
        // Formatter prints -0.00 for a small negative difference; the row has
        // no meaning for a signed zero, and the default must read as plain
        // 0.00 rather than +0.00.
        if ("+0.00".equals(signed) || "-0.00".equals(signed)) return "0.00";
        return signed;
    }

    /** The two-decimal number the row shows; Locale.US keeps its point. */
    public static String valueText(float value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    /**
     * True when the value already reads as the default. The row's reset is a
     * no-op here: the Δ text is already 0.00, so there is nothing to redraw.
     */
    public static boolean isDefault(float value, float defaultValue) {
        return Math.abs(value - defaultValue) <= DEFAULT_EPSILON;
    }
}

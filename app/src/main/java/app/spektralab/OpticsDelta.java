package app.spektralab;

/**
 * The display-only Δ on an optics row: how far the current value sits from
 * that row's panel default.
 *
 * <p>This is a formatting seam, not a second state. It holds the four panel
 * parameters (name, unit, range, default) and turns a value/default pair into
 * the signed two-decimal string the row shows. Nothing here is sent to the
 * engine, saved into a Look, or kept beside the value it describes.
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
     * One calibrated optics parameter. The slider reads its range from here,
     * and the per-row reset asks {@link #isDefault(float)} before sending a
     * render. Naming the ranges once keeps the panel and its checks on the
     * same numbers.
     */
    public enum Param {
        EXPOSURE("EV", 0.25f, 4.0f, EXPOSURE_DEFAULT),
        GRAIN("\u00d7", 0.0f, 2.0f, GRAIN_DEFAULT),
        HALATION("\u00d7", 0.0f, 4.0f, HALATION_DEFAULT),
        GLARE("\u00d7", 0.0f, 12.0f, GLARE_DEFAULT);

        /** The visible unit TalkBack also announces. */
        public final String unit;
        /** The real float range the slider exposes to accessibility. */
        public final float min, max;
        /** The value this row calls neutral. */
        public final float defaultValue;

        Param(String unit, float min, float max, float defaultValue) {
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
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

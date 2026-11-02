package app.spektralab;

/**
 * The display-only Δ on an optics row: how far the current value sits from
 * that row's panel default.
 *
 * <p>This is a formatting seam, not a second state. It holds the four panel
 * defaults and turns a value/default pair into the signed two-decimal string
 * the row shows. Nothing here is sent to the engine, saved into a Look, or
 * kept beside the value it describes.
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

    private OpticsDelta() { }

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
}

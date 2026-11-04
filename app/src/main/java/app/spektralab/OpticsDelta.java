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

    /** Reads one optics value from a Look. */
    public interface Getter {
        float get(Look look);
    }

    /** Writes one optics value into a Look. */
    public interface Setter {
        void set(Look look, float value);
    }

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
        EXPOSURE("exp", "print_exposure", "\u653e\u5927\u673a\u66dd\u5149", "E", "EV", 0.25f, 4.0f, EXPOSURE_DEFAULT, 0.01f, (l) -> l.printExposure, (l, v) -> l.printExposure = v),
        HIGHLIGHT_ROOM("highlight", "scene_latitude_highlight_room", "\u9ad8\u5149\u4fdd\u62a4", "H", "EV", 0.0f, 3.0f, 0.0f, 0.01f, (l) -> l.highlightRoom, (l, v) -> l.highlightRoom = v),
        SHADOW_ROOM("shadow", "scene_latitude_shadow_room", "\u6697\u90e8\u4fdd\u62a4", "S", "EV", 0.0f, 3.0f, 0.0f, 0.01f, (l) -> l.shadowRoom, (l, v) -> l.shadowRoom = v),
        GRAIN("grain", "grain_amount", "\u9897\u7c92", "G", "\u00d7", 0.0f, 2.0f, GRAIN_DEFAULT, 0.01f, (l) -> l.grainAmount, (l, v) -> l.grainAmount = v),
        HALATION("halation", "halation_amount", "\u5149\u6655", "\u25c9", "\u00d7", 0.0f, 4.0f, HALATION_DEFAULT, 0.01f, (l) -> l.halationAmount, (l, v) -> l.halationAmount = v),
        HALATION_BOOST("boost", "halation_boost_ev", "\u5149\u6655\u589e\u5f3a", "+", "EV", 0.0f, 8.0f, 0.0f, 0.01f, (l) -> l.halationBoost, (l, v) -> l.halationBoost = v),
        HALATION_SCATTER("scatter", "halation_scatter_amount", "\u4e73\u5242\u6563\u5c04", "~", "\u00d7", 0.0f, 1.0f, 1.0f, 0.01f, (l) -> l.halationScatter, (l, v) -> l.halationScatter = v),
        GLARE("glare", "glare_amount", "\u7729\u5149", "\u2727", "\u00d7", 0.0f, 12.0f, GLARE_DEFAULT, 0.01f, (l) -> l.glareAmount, (l, v) -> l.glareAmount = v),
        LENS_BLUR("lens", "lens_blur_um", "\u955c\u5934\u6a21\u7cca", "\u25cc", "\u00b5m", 0.0f, 200.0f, 0.0f, 1.0f, (l) -> l.lensBlur, (l, v) -> l.lensBlur = v),
        Y_FILTER("yf", "y_filter_shift", "\u9ec4\u6ee4\u955c", "Y", "CC", -1.0f, 1.0f, 0.0f, 0.01f, (l) -> l.yFilter, (l, v) -> l.yFilter = v),
        M_FILTER("mf", "m_filter_shift", "\u54c1\u7ea2\u6ee4\u955c", "M", "CC", -1.0f, 1.0f, 0.0f, 0.01f, (l) -> l.mFilter, (l, v) -> l.mFilter = v),
        FILTER_SCALE("fscale", "filter_shift_scale", "\u6ee4\u955c\u5f3a\u5ea6", "C", "CC", 1.0f, 60.0f, 1.0f, 0.1f, (l) -> l.filterScale, (l, v) -> l.filterScale = v),
        PREFLASH("preflash", "preflash_exposure", "\u9884\u95ea", "\u26a1", "\u00d7", 0.0f, 1.0f, 0.0f, 0.01f, (l) -> l.preflash, (l, v) -> l.preflash = v),
        SHADOW_ROLLOFF("rolloff", "scene_latitude_rolloff", "\u66f2\u7ebf\u67d4\u5ea6", "\u223f", "\u00d7", 1.0f, 4.0f, 2.0f, 0.01f, (l) -> l.shadowRolloff, (l, v) -> l.shadowRolloff = v),
        MAX_LIFT("lift", "scene_latitude_max_lift", "\u63d0\u5347\u4e0a\u9650", "L", "EV", 0.25f, 12.0f, 4.0f, 0.01f, (l) -> l.maxLift, (l, v) -> l.maxLift = v),
        HALATION_ON("halon", "halation_active", "\u5149\u6655\u5f00\u5173", "\u25c9", "\u00d7", 0.0f, 1.0f, 1.0f, 1.0f, (l) -> l.halationOn, (l, v) -> l.halationOn = v),
        ANTIHALATION_REMOVED("ahrem", "antihalation_removed", "\u53bb\u6297\u5149\u6655", "\u2298", "\u00d7", 0.0f, 1.0f, 0.0f, 1.0f, (l) -> l.antihalationRemoved, (l, v) -> l.antihalationRemoved = v),
        GRAIN_ON("grainon", "grain_active", "\u9897\u7c92\u5f00\u5173", "g", "\u00d7", 0.0f, 1.0f, 1.0f, 1.0f, (l) -> l.grainOn, (l, v) -> l.grainOn = v),
        COUPLERS_ON("coupon", "dir_couplers_active", "\u5c42\u95f4\u6291\u5236", "\u21c4", "\u00d7", 0.0f, 1.0f, 1.0f, 1.0f, (l) -> l.couplersOn, (l, v) -> l.couplersOn = v),
        SCANNER_WHITE("scw", "scanner_white_correction", "\u626b\u63cf\u767d", "W", "\u00d7", 0.0f, 1.0f, 0.0f, 1.0f, (l) -> l.scannerWhite, (l, v) -> l.scannerWhite = v),
        SCANNER_BLACK("scb", "scanner_black_correction", "\u626b\u63cf\u9ed1", "B", "\u00d7", 0.0f, 1.0f, 0.0f, 1.0f, (l) -> l.scannerBlack, (l, v) -> l.scannerBlack = v),
        EDR_ON("edr", "extended_dynamic_range", "\u52a8\u6001\u8303\u56f4", "HDR", "\u00d7", 0.0f, 1.0f, 0.0f, 1.0f, (l) -> l.edrOn, (l, v) -> l.edrOn = v);

        /** Stable wire name: the Look JSON key the engine's parameters use. */
        public final String wire;
        /** The engine's own parameter key for the same value. */
        public final String engineKey;
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
        /** Native Look field reader for this parameter. */
        public final Getter getter;
        /** Native Look field writer for this parameter. */
        public final Setter setter;

        Param(String wire, String engineKey, String label, String glyph, String unit,
              float min, float max, float defaultValue, float step,
              Getter getter, Setter setter) {
            this.wire = wire;
            this.engineKey = engineKey;
            this.label = label;
            this.glyph = glyph;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.step = step;
            this.getter = getter;
            this.setter = setter;
        }

        /** True when the row already reads as its default. */
        public boolean isDefault(float value) {
            return OpticsDelta.isDefault(value, defaultValue);
        }

        /** Keep a restored or dragged value inside this row's real range. */
        public float clamp(float value) {
            return Math.max(min, Math.min(max, value));
        }

        /**
         * The nearest value the slider may legally hold: clamped to the range
         * and snapped to the step. A Look value outside the range or between
         * steps is still shown as it is, but the thumb has to land on a tick or
         * Material Slider refuses to lay out.
         */
        public float snap(float value) {
            final float clamped = clamp(value);
            final float ticks = Math.round((clamped - min) / step);
            return Math.max(min, Math.min(max, min + ticks * step));
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

    /** The parameter at a strip index; an out-of-range tap lands on an end. */
    public static Param paramAt(int index) {
        final Param[] all = Param.values();
        return all[Math.max(0, Math.min(all.length - 1, index))];
    }

    /** The strip index of a parameter, in the contract's own order. */
    public static int indexOf(Param param) {
        return param == null ? 0 : param.ordinal();
    }

    /**
     * The engine's `scene_latitude_highlight_room` for a protection slider
     * value. The slider says "how much protection", the engine field says "how
     * many stops of headroom are left"; bigger room is less compression, so the
     * two run opposite ways. Zero turns the stage off; 3 is the tightest room
     * (0.5 stop) the curvature floor permits.
     */
    public static float highlightRoomWire(float protection) {
        if (protection <= 0.01f) return 0f;
        final float clamped = Math.max(0f, Math.min(3f, protection));
        return 3.0f - (clamped / 3.0f) * 2.5f;
    }

    /** True when either protection side asks the engine for the stage at all. */
    public static boolean sceneLatitudeActive(float highlightProtection,
                                              float shadowProtection) {
        return highlightProtection > 0.01f || shadowProtection > 0.01f;
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

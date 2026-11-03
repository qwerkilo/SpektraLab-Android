package app.spektralab;

import java.util.Locale;

/**
 * Look re-statement: the complete Look as the engine's parameter delta.
 *
 * <p>Preview and export call this one builder. The engine keeps state between
 * renders, so a delta that names only what changed since the last picture would
 * let an export inherit the preview's stock or a parameter a slider moved after
 * the last draw. Stating the whole Look makes that impossible: the Film stock,
 * the Print stock, every optical parameter, the camera filter, the protection
 * norm, and every Rebate and Date back field go out together, every time.
 *
 * <p>Android-free on purpose. The two context facts that do not belong to a
 * Look -- whether the source is a RAW, and what the current file is -- are
 * passed in by the caller, so the JVM check can exercise the exact string an
 * export sends.
 */
public final class LookRestatement {
    /**
     * The facts a Look does not carry: the source kind, the render's long
     * edge, whether the selected Film stock is a Reversal stock, the edge
     * print's text for that stock, and the Date back's *default* imprint text
     * (the Frame's EXIF date). A typed text lives on the Look itself and wins
     * over this fallback.
     */
    public static final class Context {
        public final boolean raw;
        public final int longEdge;
        public final boolean positiveFilm;
        public final String edgePrint;
        public final String defaultDateText;

        public Context(boolean raw, int longEdge, boolean positiveFilm,
                       String edgePrint, String defaultDateText) {
            this.raw = raw;
            this.longEdge = longEdge;
            this.positiveFilm = positiveFilm;
            this.edgePrint = edgePrint == null ? "" : edgePrint;
            this.defaultDateText = defaultDateText == null ? "" : defaultDateText;
        }
    }

    private LookRestatement() { }

    /**
     * The engine delta for one Look. Throws when the Look names no Film stock:
     * the engine's own default would render a different picture, and an export
     * with no film is a UI bug, not a look.
     */
    public static String json(Look look, Context context) {
        if (look == null) throw new IllegalArgumentException("look is null");
        if (look.filmStock == null) {
            throw new IllegalArgumentException("a Look with no Film stock has nothing to re-state");
        }
        if (context == null) throw new IllegalArgumentException("context is null");

        final String edgePrint = look.edgeTextOn ? context.edgePrint : "";
        // A typed Date back text is part of the Frame's Look; only an empty one
        // asks for the file's own EXIF date.
        final String dateText = look.dateOn
                ? orDefault(look.dateText, context.defaultDateText) : "";
        final boolean monochrome = isMonochromeStock(look.filmStock);
        final float highlight = look.get(OpticsDelta.Param.HIGHLIGHT_ROOM);
        final float shadow = look.get(OpticsDelta.Param.SHADOW_ROOM);

        return String.format(Locale.US,
            "{\"film_stock\":\"%s\",\"print_stock\":\"%s\",\"scan_film\":%s,"
            + "\"input_color_space\":\"%s\",\"input_cctf_decoding\":%s,\"auto_exposure\":%s,"
            + "\"output_color_space\":\"sRGB\",\"preview_long_edge\":%d,"
            + "\"print_exposure\":%.2f,\"grain_amount\":%.3f,\"grain_sublayers_active\":%s,"
            + "\"halation_amount\":%.3f,\"halation_boost_ev\":%.2f,"
            + "\"halation_scatter_amount\":%.3f,"
            + "\"halation_active\":%s,\"antihalation_removed\":%s,\"grain_active\":%s,"
            + "\"dir_couplers_active\":%s,\"scanner_white_correction\":%s,"
            + "\"scanner_black_correction\":%s,\"extended_dynamic_range\":%s,"
            + "\"camera_filter\":\"%s\","
            + "\"scene_latitude_active\":%s,\"scene_latitude_norm\":\"%s\","
            + "\"scene_latitude_highlight_knee\":1.0,"
            + "\"scene_latitude_highlight_room\":%.2f,\"scene_latitude_shadow_knee\":-2.0,"
            + "\"scene_latitude_shadow_room\":%.2f,"
            + "\"scene_latitude_rolloff\":%.2f,\"scene_latitude_max_lift\":%.2f,"
            + "\"glare_amount\":%.3f,\"film_format_mm\":%.1f,"
            + "\"lens_blur_um\":%.1f,"
            + "\"m_filter_shift\":%.3f,\"y_filter_shift\":%.3f,\"filter_shift_scale\":%.1f,"
            + "\"preflash_exposure\":%.4f,"
            + "\"overscan_active\":%s,\"overscan_format\":\"%s\",\"overscan_edge_text\":\"%s\","
            + "\"overscan_fog\":%.2f,\"overscan_leaks\":%.2f,\"overscan_frame_number\":%.0f,"
            + "\"overscan_mode\":\"%s\",\"overscan_gate\":\"%s\",\"overscan_holes\":\"%s\","
            + "\"overscan_carrier\":\"%s\",\"overscan_f_number\":%.0f,"
            + "\"overscan_camera_seed\":%d,\"overscan_frame_seed\":%d,"
            + "\"date_imprint_active\":%s,\"date_imprint_text\":\"%s\",\"date_imprint_style\":\"%s\","
            + "\"date_imprint_text_b\":\"%s\",\"date_imprint_data_text\":\"%s\","
            + "\"date_imprint_placement\":\"%s\",\"date_imprint_corner\":\"%s\","
            + "\"date_imprint_inset_x\":%.2f,\"date_imprint_inset_y\":%.2f,"
            + "\"date_imprint_size\":%.2f,\"date_imprint_ev\":%.1f}",
            escape(look.filmStock),
            escape(orDefault(look.printStock, Look.DEFAULT_PRINT_STOCK)),
            context.positiveFilm ? "true" : "false",
            context.raw ? "ProPhoto RGB" : "sRGB",
            context.raw ? "false" : "true",
            context.raw ? "true" : "false",
            context.longEdge,
            look.printExposure, grainAmount(look),
            monochrome ? "false" : "true",
            look.halationAmount,
            look.halationBoost,
            look.halationScatter,
            look.halationOn > 0.5f ? "true" : "false",
            look.antihalationRemoved > 0.5f ? "true" : "false",
            look.grainOn > 0.5f ? "true" : "false",
            look.couplersOn > 0.5f ? "true" : "false",
            look.scannerWhite > 0.5f ? "true" : "false",
            look.scannerBlack > 0.5f ? "true" : "false",
            look.edrOn > 0.5f ? "true" : "false",
            OpticsDelta.CameraFilter.fromWire(look.cameraFilter).wire,
            OpticsDelta.sceneLatitudeActive(highlight, shadow) ? "true" : "false",
            OpticsDelta.ProtectionNorm.fromWire(look.sceneNorm).wire,
            OpticsDelta.highlightRoomWire(highlight),
            shadow,
            look.shadowRolloff,
            look.maxLift,
            look.glareAmount, filmFormatMm(look),
            look.lensBlur,
            look.mFilter,
            look.yFilter,
            look.filterScale,
            look.preflash,
            look.overscanOn ? "true" : "false",
            orDefault(look.overscanFormat, Look.DEFAULT_OVERSCAN_FORMAT),
            escape(edgePrint),
            look.edgeFog, look.edgeLeaks, look.frameNumber,
            orDefault(look.edgeMode, "strip"),
            orDefault(look.edgeGate, "auto"),
            orDefault(look.edgeHoles, "white"),
            orDefault(look.edgeCarrier, "none"),
            look.edgeFNumber,
            look.cameraSeed, look.frameSeed,
            look.dateOn ? "true" : "false", escape(dateText),
            orDefault(look.dateStyle, "lcd"),
            escape(look.dateTextB),
            escape(look.dataText),
            orDefault(look.datePlacement, "frame"),
            orDefault(look.dateCorner, "br"),
            look.dateInsetX, look.dateInsetY, look.dateSize, look.dateEv
        );
    }

    /**
     * The engine field one optical parameter is re-stated as. The Look uses
     * the parameter's own wire name; the engine names the same value after the
     * stage it drives. One table keeps the builder from silently dropping a
     * parameter that has no matching key.
     */
    public static String engineKey(OpticsDelta.Param param) {
        switch (param) {
            case EXPOSURE: return "print_exposure";
            case HIGHLIGHT_ROOM: return "scene_latitude_highlight_room";
            case SHADOW_ROOM: return "scene_latitude_shadow_room";
            case GRAIN: return "grain_amount";
            case HALATION: return "halation_amount";
            case HALATION_BOOST: return "halation_boost_ev";
            case HALATION_SCATTER: return "halation_scatter_amount";
            case GLARE: return "glare_amount";
            case LENS_BLUR: return "lens_blur_um";
            case Y_FILTER: return "y_filter_shift";
            case M_FILTER: return "m_filter_shift";
            case FILTER_SCALE: return "filter_shift_scale";
            case PREFLASH: return "preflash_exposure";
            case SHADOW_ROLLOFF: return "scene_latitude_rolloff";
            case MAX_LIFT: return "scene_latitude_max_lift";
            case HALATION_ON: return "halation_active";
            case ANTIHALATION_REMOVED: return "antihalation_removed";
            case GRAIN_ON: return "grain_active";
            case COUPLERS_ON: return "dir_couplers_active";
            case SCANNER_WHITE: return "scanner_white_correction";
            case SCANNER_BLACK: return "scanner_black_correction";
            case EDR_ON: return "extended_dynamic_range";
            default: throw new IllegalArgumentException("unknown parameter " + param);
        }
    }

    /**
     * The frame length the rebate layout must see. With a rebate on, the Gate
     * decides: the Scale describes grain size, the Gate describes the film the
     * edge is cut from, and the engine lays out the canvas by the latter.
     */
    public static float filmFormatMm(Look look) {
        if (look == null) return 36f;
        if (!look.overscanOn) return look.filmFormatMm;
        final GateScale.Gate gate = GateScale.byId(look.overscanFormat);
        return gate != null ? gate.longEdgeMm : look.filmFormatMm;
    }

    /** The wire's `grain_amount`: the slider times the stock's own amount. */
    public static double grainAmount(Look look) {
        final float slider = look == null ? 1f : look.get(OpticsDelta.Param.GRAIN);
        return Math.max(0.0, Math.min(2.0, slider * ownGrain(look == null ? null : look.filmStock)));
    }

    /**
     * What the Grain slider's 1 means on a black-and-white stock: the
     * `grain_amount` at which the engine renders the diffuse RMS granularity
     * the maker publishes. A colour film's is 1.
     */
    public static double ownGrain(String stock) {
        if (stock == null) return 1.0;
        switch (stock) {
            case "kodak_tri_x_400": return 1.70;
            case "ilford_hp5_plus_400": return 1.49;
            case "kodak_tmax_100": return 0.74;
            case "fujifilm_neopan_acros_100_ii": return 0.64;
            default: return 1.0;
        }
    }

    /**
     * The four black-and-white stocks the catalogue carries. The engine's
     * grain has a colour and a monochrome form; the silver sheet gets the
     * single-layer one.
     */
    public static boolean isMonochromeStock(String stock) {
        if (stock == null) return false;
        switch (stock) {
            case "kodak_tri_x_400":
            case "ilford_hp5_plus_400":
            case "kodak_tmax_100":
            case "fujifilm_neopan_acros_100_ii":
                return true;
            default:
                return false;
        }
    }

    /** Quote a user-editable string for the engine's JSON delta. */
    public static String escape(String value) {
        if (value == null) return "";
        final StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); ++i) {
            final char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format(Locale.US, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}

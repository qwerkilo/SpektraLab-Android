package app.spektralab;

/**
 * Pure JVM contract for the Look re-statement: the one parameter builder
 * preview and export share.
 *
 * <p>The export may not inherit whatever an earlier render left in the engine,
 * so the builder has to state the whole Look every time: the Film stock, the
 * Print stock, all 22 optical parameters, the camera filter, the protection
 * norm, and every Rebate and Date back field. These checks change one value at
 * a time and look for it in the wire JSON, which is the seam an export reads.
 */
public final class LookRestatementCheck {
    private static int checks;

    public static void main(String[] args) {
        everyFieldIsStated();
        everyParameterReachesTheWire();
        choicesReachTheWire();
        rawAndScanContext();
        rebateAndDateReachTheWire();
        dateOffClearsTheText();
        formatFollowsTheGate();
        grainFollowsTheStock();
        escapingKeepsJsonValid();
        emptyTypedDateUsesTheFrameDefault();
        noFilmHasNothingToState();

        System.out.println("LookRestatementCheck: " + checks + " checks passed");
    }

    /** A full Look: no key is allowed to fall back to an engine default. */
    private static void everyFieldIsStated() {
        final Look look = fullLook();
        final org.json.JSONObject json = parse(
                LookRestatement.json(look, context(false, false, "EDGE", "")));

        eq("kodak_portra_400", json.optString("film_stock"));
        eq("kodak_portra_endura", json.optString("print_stock"));
        eq("2048", json.optString("preview_long_edge"));
        eq("sRGB", json.optString("output_color_space"));

        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            final String key = LookRestatement.engineKey(param);
            ok("missing engine key " + key + " for " + param.wire, json.has(key));
        }
        for (String key : new String[] {
                "scan_film", "input_color_space", "input_cctf_decoding", "auto_exposure",
                "print_exposure", "grain_amount", "grain_sublayers_active",
                "halation_amount", "halation_boost_ev", "halation_scatter_amount",
                "halation_active", "antihalation_removed", "grain_active",
                "dir_couplers_active", "scanner_white_correction",
                "scanner_black_correction", "extended_dynamic_range", "camera_filter",
                "hue_correction",
                "scene_latitude_active", "scene_latitude_norm",
                "scene_latitude_highlight_knee", "scene_latitude_highlight_room",
                "scene_latitude_shadow_knee", "scene_latitude_shadow_room",
                "scene_latitude_rolloff", "scene_latitude_max_lift",
                "glare_amount", "film_format_mm", "lens_blur_um", "m_filter_shift",
                "y_filter_shift", "filter_shift_scale", "preflash_exposure",
                "overscan_active", "overscan_format", "overscan_edge_text",
                "overscan_fog", "overscan_leaks", "overscan_frame_number",
                "overscan_mode", "overscan_gate", "overscan_holes", "overscan_carrier",
                "overscan_f_number", "overscan_camera_seed", "overscan_frame_seed",
                "date_imprint_active", "date_imprint_text", "date_imprint_style",
                "date_imprint_text_b", "date_imprint_data_text",
                "date_imprint_placement", "date_imprint_corner",
                "date_imprint_inset_x", "date_imprint_inset_y",
                "date_imprint_size", "date_imprint_ev"}) {
            ok("missing wire key " + key, json.has(key));
        }
    }

    /**
     * The export-parity seam: changing one parameter changes exactly its wire
     * key, and the value written is the value the Look holds. A slider that
     * moves the preview but not the export fails here.
     */
    private static void everyParameterReachesTheWire() {
        final org.json.JSONObject baseline = parse(
                LookRestatement.json(fullLook(), context(false, false, "EDGE", "")));
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            final Look changed = fullLook();
            // A different endpoint: no parameter's default is its maximum, and
            // the ones whose default is the minimum move to the maximum.
            final float value = param.defaultValue == param.min ? param.max : param.min;
            changed.set(param, value);

            final org.json.JSONObject json = parse(
                    LookRestatement.json(changed, context(false, false, "EDGE", "")));
            final String key = LookRestatement.engineKey(param);
            if (isSwitch(param)) {
                eq(value > 0.5f ? "true" : "false", json.optString(key));
                ok(param.wire + " did not change with the parameter",
                        !json.optString(key).equals(baseline.optString(key)));
            } else {
                near(key, engineValue(param, changed), json.optDouble(key));
                ok(param.wire + " did not change with the parameter",
                        json.optDouble(key) != baseline.optDouble(key));
            }
        }
    }

    /** The six parameters the engine reads as on/off switches. */
    private static boolean isSwitch(OpticsDelta.Param param) {
        switch (param) {
            case HALATION_ON:
            case ANTIHALATION_REMOVED:
            case GRAIN_ON:
            case COUPLERS_ON:
            case SCANNER_WHITE:
            case SCANNER_BLACK:
            case EDR_ON:
                return true;
            default:
                return false;
        }
    }

    /**
     * What the engine field should read for a Look, from the contract only:
     * the protection slider is inverted, the grain slider is scaled by the
     * stock's own granularity, and everything else is the value itself.
     */
    private static double engineValue(OpticsDelta.Param param, Look look) {
        final float value = look.get(param);
        switch (param) {
            case HIGHLIGHT_ROOM: return OpticsDelta.highlightRoomWire(value);
            case GRAIN: return value * LookRestatement.ownGrain(look.filmStock);
            default: return value;
        }
    }

    private static void choicesReachTheWire() {
        final Look look = fullLook();
        look.cameraFilter = OpticsDelta.CameraFilter.W25.wire;
        look.sceneNorm = OpticsDelta.ProtectionNorm.PEAK.wire;
        look.set(OpticsDelta.Param.HIGHLIGHT_ROOM, 1.5f);
        look.set(OpticsDelta.Param.SHADOW_ROOM, 2.0f);
        final org.json.JSONObject json = parse(
                LookRestatement.json(look, context(false, false, "EDGE", "")));

        eq("w25", json.optString("camera_filter"));
        near("hue_correction", 1.0, json.optDouble("hue_correction"));
        eq("max", json.optString("scene_latitude_norm"));
        // The slider says protection; the engine wants the room left over.
        near("scene_latitude_highlight_room", OpticsDelta.highlightRoomWire(1.5f),
                json.optDouble("scene_latitude_highlight_room"));
        near("scene_latitude_shadow_room", 2.0, json.optDouble("scene_latitude_shadow_room"));
        eq("true", json.optString("scene_latitude_active"));

        // However the Look spells a choice the app no longer offers, the
        // engine gets a documented id rather than an unknown string.
        final Look unknown = fullLook();
        unknown.cameraFilter = "w99";
        unknown.sceneNorm = "spot";
        final org.json.JSONObject fallback = parse(
                LookRestatement.json(unknown, context(false, false, "EDGE", "")));
        eq(OpticsDelta.CameraFilter.NONE.wire, fallback.optString("camera_filter"));
        eq(OpticsDelta.ProtectionNorm.POWER.wire, fallback.optString("scene_latitude_norm"));
        eq("false", fallback.optString("scene_latitude_active"));
    }

    private static void rawAndScanContext() {
        final Look look = fullLook();

        final org.json.JSONObject raw = parse(LookRestatement.json(
                look, new LookRestatement.Context(true, 2048, false, "EDGE", "")));
        eq("ProPhoto RGB", raw.optString("input_color_space"));
        eq("false", raw.optString("input_cctf_decoding"));
        eq("true", raw.optString("auto_exposure"));
        eq("balanced", raw.optString("auto_exposure_method"));
        eq("false", raw.optString("scan_film"));

        final org.json.JSONObject jpeg = parse(LookRestatement.json(
                look, new LookRestatement.Context(false, 1024, false, "EDGE", "")));
        eq("sRGB", jpeg.optString("input_color_space"));
        eq("true", jpeg.optString("input_cctf_decoding"));
        eq("false", jpeg.optString("auto_exposure"));
        eq("1024", jpeg.optString("preview_long_edge"));

        look.meterMethod = "protect_highlights";
        final org.json.JSONObject meteredJpeg = parse(LookRestatement.json(
                look, new LookRestatement.Context(false, 1024, false, "EDGE", "")));
        eq("true", meteredJpeg.optString("auto_exposure"));
        eq("protect_highlights", meteredJpeg.optString("auto_exposure_method"));

        final org.json.JSONObject p3 = parse(LookRestatement.json(
                look, new LookRestatement.Context(false, 1024, false, "EDGE", "", "Display P3")));
        eq("Display P3", p3.optString("output_color_space"));

        // A directly scanned positive bypasses the negative scan correction;
        // sending it makes the native engine reject the whole render.
        look.scannerWhite = 1f;
        look.scannerBlack = 1f;
        final org.json.JSONObject negative = parse(LookRestatement.json(
                look, new LookRestatement.Context(false, 2048, false, "EDGE", "")));
        eq("true", negative.optString("scanner_white_correction"));
        eq("true", negative.optString("scanner_black_correction"));

        final org.json.JSONObject slide = parse(LookRestatement.json(
                look, new LookRestatement.Context(false, 2048, true, "EDGE", "")));
        eq("true", slide.optString("scan_film"));
        eq("false", slide.optString("scanner_white_correction"));
        eq("false", slide.optString("scanner_black_correction"));
    }

    private static void rebateAndDateReachTheWire() {
        final Look look = fullLook();
        look.overscanOn = true;
        look.overscanFormat = "120_6x7";
        look.edgeMode = "filed";
        look.edgeGate = "shouldered";
        look.edgeHoles = "black";
        look.edgeCarrier = "open";
        look.edgeTextOn = true;
        look.edgeFog = 2.25f;
        look.edgeLeaks = 0.75f;
        look.frameNumber = 12f;
        look.edgeFNumber = 5.6f;
        look.cameraSeed = 7;
        look.frameSeed = 19;
        look.dateOn = true;
        look.datePlacement = "rebate";
        look.dateCorner = "tl";
        look.dateStyle = "data";
        look.dateInsetX = 4.5f;
        look.dateInsetY = 1.25f;
        look.dateSize = 1.8f;
        look.dateEv = 2.2f;
        look.dateTextB = "2026 11 03";
        look.dataText = "1/125 f/8";

        final org.json.JSONObject json = parse(LookRestatement.json(
                look, context(false, false, "KODAK PORTRA 400", "2026 11 07")));
        eq("true", json.optString("overscan_active"));
        eq("120_6x7", json.optString("overscan_format"));
        eq("KODAK PORTRA 400", json.optString("overscan_edge_text"));
        eq("filed", json.optString("overscan_mode"));
        eq("shouldered", json.optString("overscan_gate"));
        eq("black", json.optString("overscan_holes"));
        eq("open", json.optString("overscan_carrier"));
        near("overscan_fog", 2.25, json.optDouble("overscan_fog"));
        near("overscan_leaks", 0.75, json.optDouble("overscan_leaks"));
        near("overscan_frame_number", 12.0, json.optDouble("overscan_frame_number"));
        // The wire publishes whole f-numbers: the existing delta format
        // rounds 5.6 to 6, and this sync must not change an image.
        near("overscan_f_number", 6.0, json.optDouble("overscan_f_number"));
        eq("7", json.optString("overscan_camera_seed"));
        eq("19", json.optString("overscan_frame_seed"));

        eq("true", json.optString("date_imprint_active"));
        eq("2026 11 07", json.optString("date_imprint_text"));
        eq("data", json.optString("date_imprint_style"));
        eq("2026 11 03", json.optString("date_imprint_text_b"));
        eq("1/125 f/8", json.optString("date_imprint_data_text"));
        eq("rebate", json.optString("date_imprint_placement"));
        eq("tl", json.optString("date_imprint_corner"));
        near("date_imprint_inset_x", 4.5, json.optDouble("date_imprint_inset_x"));
        near("date_imprint_inset_y", 1.25, json.optDouble("date_imprint_inset_y"));
        near("date_imprint_size", 1.8, json.optDouble("date_imprint_size"));
        near("date_imprint_ev", 2.2, json.optDouble("date_imprint_ev"));

        // The edge print has its own switch: off means an empty print, not a
        // stale one left in the engine.
        look.edgeTextOn = false;
        eq("", parse(LookRestatement.json(
                look, context(false, false, "KODAK PORTRA 400", "2026 11 07")))
                .optString("overscan_edge_text"));
    }

    private static void dateOffClearsTheText() {
        final Look look = fullLook();
        look.dateOn = false;
        final org.json.JSONObject json = parse(LookRestatement.json(
                look, context(false, false, "EDGE", "2026 11 07")));
        eq("false", json.optString("date_imprint_active"));
        eq("", json.optString("date_imprint_text"));
    }

    private static void formatFollowsTheGate() {
        final Look look = fullLook();
        look.filmFormatMm = 36f;
        look.overscanFormat = "120_6x7";

        look.overscanOn = false;
        near("film_format_mm without a rebate", 36.0,
                parse(LookRestatement.json(look, context(false, false, "EDGE", "")))
                        .optDouble("film_format_mm"));

        look.overscanOn = true;
        near("film_format_mm with a rebate", 69.5,
                parse(LookRestatement.json(look, context(false, false, "EDGE", "")))
                        .optDouble("film_format_mm"));

        // An unknown Gate falls back to the Scale, never to a made-up edge.
        look.overscanFormat = "120_6x17_future";
        look.filmFormatMm = 56f;
        near("film_format_mm with an unknown Gate", 56.0,
                parse(LookRestatement.json(look, context(false, false, "EDGE", "")))
                        .optDouble("film_format_mm"));
    }

    private static void grainFollowsTheStock() {
        final Look look = fullLook();
        look.filmStock = "kodak_portra_400";
        look.set(OpticsDelta.Param.GRAIN, 1.0f);
        near("colour grain is the slider", 1.0,
                parse(LookRestatement.json(look, context(false, false, "EDGE", "")))
                        .optDouble("grain_amount"));
        eq("true", parse(LookRestatement.json(look, context(false, false, "EDGE", "")))
                .optString("grain_sublayers_active"));

        look.filmStock = "kodak_tri_x_400";
        near("Tri-X grain is the slider times its own amount", 1.70,
                parse(LookRestatement.json(look, context(false, false, "EDGE", "")))
                        .optDouble("grain_amount"));
        eq("false", parse(LookRestatement.json(look, context(false, false, "EDGE", "")))
                .optString("grain_sublayers_active"));

        look.set(OpticsDelta.Param.GRAIN, 1.5f);
        near("the wire grain is clamped to 2.0", 2.0,
                parse(LookRestatement.json(look, context(false, false, "EDGE", "")))
                        .optDouble("grain_amount"));
    }

    private static void escapingKeepsJsonValid() {
        final Look look = fullLook();
        look.dateOn = true;
        look.dataText = "100 ISO \"sun\\rain\"" + (char) 10 + "second line";
        look.dateTextB = "'26 11 03";
        look.dateText = "\"typed\" date";
        final org.json.JSONObject json = parse(LookRestatement.json(
                look, context(false, false, "EDGE", "2026 11 07")));
        eq("100 ISO \"sun\\rain\"" + (char) 10 + "second line",
                json.optString("date_imprint_data_text"));
        eq("'26 11 03", json.optString("date_imprint_text_b"));
        // The Look's typed date wins over the Frame's EXIF default.
        eq("\"typed\" date", json.optString("date_imprint_text"));

        // Catalog and edge-print names reach the wire through the same escape:
        // a quote in any of them may not break the delta.
        final Look quoted = fullLook();
        quoted.filmStock = "kodak\"portra";
        quoted.printStock = "kodak\\endura";
        quoted.edgeTextOn = true;
        final org.json.JSONObject edge = parse(LookRestatement.json(quoted,
                context(false, false, "KODAK \"400\"", "")));
        eq("kodak\"portra", edge.optString("film_stock"));
        eq("kodak\\endura", edge.optString("print_stock"));
        eq("KODAK \"400\"", edge.optString("overscan_edge_text"));
    }

    /** An empty typed text follows the file's EXIF date, not a stale one. */
    private static void emptyTypedDateUsesTheFrameDefault() {
        final Look look = fullLook();
        look.dateOn = true;
        look.dateText = "";
        eq("2026 11 07", parse(LookRestatement.json(
                look, context(false, false, "EDGE", "2026 11 07")))
                .optString("date_imprint_text"));
    }

    private static void noFilmHasNothingToState() {
        final Look look = fullLook();
        look.filmStock = null;
        boolean threw = false;
        try {
            LookRestatement.json(look, context(false, false, "EDGE", ""));
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        ok("a Look without a Film stock is not re-stated", threw);
    }

    // ---------------------------------------------------------------------

    private static Look fullLook() {
        final Look look = new Look();
        look.filmStock = "kodak_portra_400";
        look.printStock = "kodak_portra_endura";
        look.filmFormatMm = 36f;
        look.overscanOn = false;
        look.overscanFormat = Look.DEFAULT_OVERSCAN_FORMAT;
        look.dateOn = false;
        look.edgeMode = "strip";
        look.edgeGate = "auto";
        look.edgeHoles = "white";
        look.edgeCarrier = "none";
        look.edgeFog = 1f;
        look.edgeLeaks = 0f;
        look.frameNumber = 0f;
        look.edgeFNumber = 0f;
        look.cameraSeed = 1;
        look.frameSeed = 1;
        look.datePlacement = "frame";
        look.dateCorner = "br";
        look.dateStyle = "lcd";
        look.dateInsetX = 3f;
        look.dateInsetY = 2.4f;
        look.dateSize = 1f;
        look.dateEv = 3.5f;
        look.dataText = "";
        look.dateTextB = "";
        look.dateText = "";
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            look.set(param, param.defaultValue);
        }
        look.cameraFilter = OpticsDelta.CameraFilter.NONE.wire;
        look.sceneNorm = OpticsDelta.ProtectionNorm.POWER.wire;
        return look;
    }

    private static LookRestatement.Context context(boolean raw, boolean positive,
                                                   String edgePrint, String dateText) {
        return new LookRestatement.Context(raw, 2048, positive, edgePrint, dateText);
    }

    private static org.json.JSONObject parse(String json) {
        try {
            return new org.json.JSONObject(json);
        } catch (org.json.JSONException e) {
            throw new AssertionError("the re-statement is not valid JSON: " + e
                    + "\n" + json);
        }
    }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void near(String what, double expected, double actual) {
        checks++;
        if (Math.abs(expected - actual) > 0.0005) {
            throw new AssertionError(what + ": expected " + expected + ", got " + actual);
        }
    }

    private static void ok(String what, boolean value) {
        checks++;
        if (!value) throw new AssertionError(what);
    }

    private static void ok(boolean value) {
        ok("expected true", value);
    }
}

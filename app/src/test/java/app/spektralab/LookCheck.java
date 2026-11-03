package app.spektralab;

/**
 * Pure JVM contract for a Frame's Look.
 *
 * <p>Three things must keep working as the parameter set grows: an older Look
 * JSON without a field reads with that field's documented default; every value
 * the older Look did carry survives untouched; and a full Look round-trips
 * through its wire names without drift.
 */
public final class LookCheck {
    private static int checks;

    public static void main(String[] args) {
        defaults();
        oldJson();
        roundTrip();
        disabledRebateKeepsItsValues();
        wireNames();

        System.out.println("LookCheck: " + checks + " checks passed");
    }

    private static void defaults() {
        final Look look = new Look();
        eq(null, look.filmStock);
        eq(Look.DEFAULT_PRINT_STOCK, look.printStock);
        eq(Look.DEFAULT_OVERSCAN_FORMAT, look.overscanFormat);
        eq(36f, look.filmFormatMm);
        ok(!look.overscanOn && !look.dateOn);

        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            eq(param.defaultValue, look.get(param));
        }
        eq(OpticsDelta.CameraFilter.NONE.wire, look.cameraFilter);
        eq(OpticsDelta.ProtectionNorm.POWER.wire, look.sceneNorm);

        // The rebate and date-back fields the engine expects even when the
        // feature is off: the controls start from these, not from zero.
        eq("strip", look.edgeMode);
        eq("auto", look.edgeGate);
        eq("white", look.edgeHoles);
        eq("none", look.edgeCarrier);
        eq(1f, look.edgeFog);
        eq(0f, look.edgeLeaks);
        eq(0f, look.frameNumber);
        eq(0f, look.edgeFNumber);
        eq(1, look.cameraSeed);
        eq(1, look.frameSeed);
        ok(look.edgeTextOn);

        eq("frame", look.datePlacement);
        eq("br", look.dateCorner);
        eq("lcd", look.dateStyle);
        eq(3f, look.dateInsetX);
        eq(2.4f, look.dateInsetY);
        eq(1f, look.dateSize);
        eq(3.5f, look.dateEv);
        eq("", look.dataText);
        eq("", look.dateTextB);
    }

    /** An 1.3.x Look has none of the new fields; none of its values may move. */
    private static void oldJson() {
        final org.json.JSONObject old = new org.json.JSONObject();
        put(old, "film", "kodak_portra_400");
        put(old, "print", "kodak_portra_endura");
        put(old, "exp", 1.4);
        put(old, "grain", 0.6);
        put(old, "halation", 2.2);
        put(old, "glare", 5.5);
        put(old, "mm", 56.0);
        put(old, "overscan", true);
        put(old, "format", "120_6x7");
        put(old, "date", true);

        final Look look = Look.fromJson(old);
        eq("kodak_portra_400", look.filmStock);
        eq("kodak_portra_endura", look.printStock);
        eq(1.4f, look.printExposure);
        eq(0.6f, look.grainAmount);
        eq(2.2f, look.halationAmount);
        eq(5.5f, look.glareAmount);
        eq(56f, look.filmFormatMm);
        ok(look.overscanOn);
        eq("120_6x7", look.overscanFormat);
        ok(look.dateOn);

        // New parameters and choices take their documented defaults. The four
        // the old Look did carry keep the values asserted above.
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            if ("exp".equals(param.wire) || "grain".equals(param.wire)
                    || "halation".equals(param.wire) || "glare".equals(param.wire)) {
                continue;
            }
            eq(param.defaultValue, look.get(param));
        }
        eq("", look.cameraFilter);
        eq("power", look.sceneNorm);
        eq("strip", look.edgeMode);
        eq("auto", look.edgeGate);
        eq("white", look.edgeHoles);
        eq("none", look.edgeCarrier);
        eq(1f, look.edgeFog);
        eq(1f, look.dateSize);
        ok(look.edgeTextOn);

        // A missing paper is the default paper, and a missing film is no film.
        final Look bare = Look.fromJson(new org.json.JSONObject());
        eq(null, bare.filmStock);
        eq(Look.DEFAULT_PRINT_STOCK, bare.printStock);
        eq(OpticsDelta.Param.GLARE.defaultValue, bare.get(OpticsDelta.Param.GLARE));

        // A value the app does not know yet is preserved, not rewritten.
        final org.json.JSONObject future = new org.json.JSONObject();
        put(future, "format", "120_6x17_future");
        eq("120_6x17_future", Look.fromJson(future).overscanFormat);

        // The engine's glare range reaches 30 even though the M3 slider shows
        // 0..12; an older Look above the slider range keeps its value.
        final org.json.JSONObject wide = new org.json.JSONObject();
        put(wide, "glare", 20.0);
        eq(20f, Look.fromJson(wide).glareAmount);
    }

    private static void roundTrip() {
        final Look look = new Look();
        look.filmStock = "kodak_vision3_250d";
        look.printStock = "kodak_2383";
        look.filmFormatMm = 69.5f;
        look.overscanOn = true;
        look.overscanFormat = "120_6x7";
        look.dateOn = true;
        look.cameraFilter = OpticsDelta.CameraFilter.W25.wire;
        look.sceneNorm = OpticsDelta.ProtectionNorm.PEAK.wire;
        look.edgeMode = "filed";
        look.edgeGate = "rounded";
        look.edgeHoles = "black";
        look.edgeCarrier = "open";
        look.edgeFog = 2.25f;
        look.edgeLeaks = 0.75f;
        look.frameNumber = 12f;
        look.edgeFNumber = 5.6f;
        look.edgeTextOn = false;
        look.cameraSeed = 7;
        look.frameSeed = 19;
        look.datePlacement = "rebate";
        look.dateCorner = "tl";
        look.dateStyle = "data";
        look.dateInsetX = 4.5f;
        look.dateInsetY = 1.25f;
        look.dateSize = 1.8f;
        look.dateEv = 2.2f;
        look.dataText = "100 ISO \"sun\\rain\"\nsecond line";
        look.dateTextB = "2026 11 03";
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            final float value = param.defaultValue == param.max
                    ? param.min
                    : Math.min(param.max, param.defaultValue + 3f * param.step);
            look.set(param, value);
        }

        final Look copy = Look.fromJson(look.toJson());
        eq(look.filmStock, copy.filmStock);
        eq(look.printStock, copy.printStock);
        eq(look.filmFormatMm, copy.filmFormatMm);
        eq(look.overscanOn, copy.overscanOn);
        eq(look.overscanFormat, copy.overscanFormat);
        eq(look.dateOn, copy.dateOn);
        eq(look.cameraFilter, copy.cameraFilter);
        eq(look.sceneNorm, copy.sceneNorm);
        eq(look.edgeMode, copy.edgeMode);
        eq(look.edgeGate, copy.edgeGate);
        eq(look.edgeHoles, copy.edgeHoles);
        eq(look.edgeCarrier, copy.edgeCarrier);
        eq(look.edgeFog, copy.edgeFog);
        eq(look.edgeLeaks, copy.edgeLeaks);
        eq(look.frameNumber, copy.frameNumber);
        eq(look.edgeFNumber, copy.edgeFNumber);
        eq(look.edgeTextOn, copy.edgeTextOn);
        eq(look.cameraSeed, copy.cameraSeed);
        eq(look.frameSeed, copy.frameSeed);
        eq(look.datePlacement, copy.datePlacement);
        eq(look.dateCorner, copy.dateCorner);
        eq(look.dateStyle, copy.dateStyle);
        eq(look.dateInsetX, copy.dateInsetX);
        eq(look.dateInsetY, copy.dateInsetY);
        eq(look.dateSize, copy.dateSize);
        eq(look.dateEv, copy.dateEv);
        eq(look.dataText, copy.dataText);
        eq(look.dateTextB, copy.dateTextB);
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            eq(look.get(param), copy.get(param));
        }

        // A second pass must not drift: the wire form is stable.
        eq(look.toJson().toString(), copy.toJson().toString());
    }

    /**
     * The Rebate page dims its controls while the switch is off; dimming is
     * not clearing. A disabled rebate keeps every value it was set to, so
     * switching Frame or re-enabling the switch finds the same edge.
     */
    private static void disabledRebateKeepsItsValues() {
        final Look look = new Look();
        look.overscanOn = false;
        look.overscanFormat = "120_6x7";
        look.filmFormatMm = 69.5f;
        look.edgeMode = "filed";
        look.edgeGate = "shouldered";
        look.edgeHoles = "black";
        look.edgeCarrier = "open";
        look.edgeTextOn = false;
        look.edgeFog = 3.25f;
        look.edgeLeaks = 1.75f;
        look.frameNumber = 27f;
        look.edgeFNumber = 8f;
        look.cameraSeed = 91;
        look.frameSeed = 92;
        look.dateOn = false;
        look.datePlacement = "rebate";
        look.dateCorner = "tl";
        look.dateStyle = "dots";
        look.dateInsetX = 6f;
        look.dateInsetY = 5f;
        look.dateSize = 1.4f;
        look.dateEv = 1.5f;
        look.dataText = "1/125 f/8";
        look.dateTextB = "'26 11 03";

        final Look copy = Look.fromJson(look.toJson());
        ok(!copy.overscanOn);
        ok(!copy.dateOn);
        eq("120_6x7", copy.overscanFormat);
        eq(69.5f, copy.filmFormatMm);
        eq("filed", copy.edgeMode);
        eq("shouldered", copy.edgeGate);
        eq("black", copy.edgeHoles);
        eq("open", copy.edgeCarrier);
        ok(!copy.edgeTextOn);
        eq(3.25f, copy.edgeFog);
        eq(1.75f, copy.edgeLeaks);
        eq(27f, copy.frameNumber);
        eq(8f, copy.edgeFNumber);
        eq(91, copy.cameraSeed);
        eq(92, copy.frameSeed);
        eq("rebate", copy.datePlacement);
        eq("tl", copy.dateCorner);
        eq("dots", copy.dateStyle);
        eq(6f, copy.dateInsetX);
        eq(5f, copy.dateInsetY);
        eq(1.4f, copy.dateSize);
        eq(1.5f, copy.dateEv);
        eq("1/125 f/8", copy.dataText);
        eq("'26 11 03", copy.dateTextB);
    }

    private static void wireNames() {
        final org.json.JSONObject json = new Look().toJson();
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            ok(json.has(param.wire));
        }
        for (String key : new String[] {"film", "print", "mm", "overscan", "format", "date",
                "cfilter", "snorm", "emode", "egate", "eholes", "ecarrier", "fog", "leaks",
                "frameno", "etext", "efnum", "dplace", "dcorn", "dstyle", "dinsx", "dinsy",
                "dsize", "dev", "dtext", "dtextb", "cseed", "fseed"}) {
            ok(json.has(key));
        }
        eq(OpticsDelta.Param.GLARE.defaultValue, (float) json.optDouble("glare"));
        eq(OpticsDelta.CameraFilter.NONE.wire, json.optString("cfilter"));
        eq(OpticsDelta.ProtectionNorm.POWER.wire, json.optString("snorm"));
    }

    private static void put(org.json.JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (org.json.JSONException e) {
            throw new AssertionError("cannot put " + key + ": " + e);
        }
    }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void ok(boolean value) {
        checks++;
        if (!value) throw new AssertionError("expected true");
    }
}

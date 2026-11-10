package app.spektralab;

import app.spektralab.core.ScanProfile;

/** Pure JVM contract for the SP-3000 scan preset and its two correction switches. */
public final class ScanProfileCheck {
    private static int checks;

    public static void main(String[] args) throws Exception {
        choices();
        lookDefaultsToOff();
        applyingSp3000TurnsBothSwitchesOn();
        applyingOffClearsBothSwitches();
        oneSwitchOnIsStillOff();
        olderLookIsOff();
        presetReachesTheWire();
        copyPreservesThePreset();

        System.out.println("ScanProfileCheck: " + checks + " checks passed");
    }

    private static void choices() {
        eq("off", ScanProfile.OFF.wire);
        eq("sp3000", ScanProfile.SP_3000.wire);
        eq("SP-3000（模型）", ScanProfile.SP_3000.label);
        eq(ScanProfile.OFF, ScanProfile.fromWire("unknown"));
        eq(ScanProfile.SP_3000, ScanProfile.fromWire("sp3000"));
        eq(ScanProfile.SP_3000, ScanProfile.of(true, true));
        eq(ScanProfile.OFF, ScanProfile.of(true, false));
        eq(ScanProfile.OFF, ScanProfile.of(false, true));
    }

    private static void lookDefaultsToOff() {
        eq(ScanProfile.OFF, new Look().scanProfile());
    }

    private static void applyingSp3000TurnsBothSwitchesOn() {
        final Look look = new Look();
        look.applyScanProfile(ScanProfile.SP_3000);
        eq(1f, look.get(OpticsDelta.Param.SCANNER_WHITE));
        eq(1f, look.get(OpticsDelta.Param.SCANNER_BLACK));
        eq(ScanProfile.SP_3000, look.scanProfile());
    }

    private static void applyingOffClearsBothSwitches() {
        final Look look = new Look();
        look.applyScanProfile(ScanProfile.SP_3000);
        look.applyScanProfile(ScanProfile.OFF);
        eq(0f, look.get(OpticsDelta.Param.SCANNER_WHITE));
        eq(0f, look.get(OpticsDelta.Param.SCANNER_BLACK));
        eq(ScanProfile.OFF, look.scanProfile());
    }

    private static void oneSwitchOnIsStillOff() {
        final Look look = new Look();
        look.set(OpticsDelta.Param.SCANNER_WHITE, 1f);
        eq(ScanProfile.OFF, look.scanProfile());
        look.set(OpticsDelta.Param.SCANNER_BLACK, 1f);
        eq(ScanProfile.SP_3000, look.scanProfile());
    }

    private static void olderLookIsOff() {
        final org.json.JSONObject old = new org.json.JSONObject();
        old.put("film", "kodak_portra_400");
        eq(ScanProfile.OFF, Look.fromJson(old).scanProfile());

        final org.json.JSONObject on = new org.json.JSONObject();
        on.put("scw", 1.0);
        on.put("scb", 1.0);
        eq(ScanProfile.SP_3000, Look.fromJson(on).scanProfile());
    }

    private static void presetReachesTheWire() throws Exception {
        final Look look = new Look();
        look.filmStock = "kodak_portra_400";
        look.applyScanProfile(ScanProfile.SP_3000);
        final org.json.JSONObject json = new org.json.JSONObject(LookRestatement.json(look,
                new LookRestatement.Context(false, 2048, false, "", "", "sRGB")));
        ok("white correction not sent", json.optBoolean("scanner_white_correction"));
        ok("black correction not sent", json.optBoolean("scanner_black_correction"));
    }

    private static void copyPreservesThePreset() {
        final Look look = new Look();
        look.applyScanProfile(ScanProfile.SP_3000);
        eq(ScanProfile.SP_3000, look.copy().scanProfile());
    }

    private static void eq(Object expected, Object actual) {
        ++checks;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    private static void ok(String message, boolean condition) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }
}

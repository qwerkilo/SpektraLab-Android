package app.spektralab;

import app.spektralab.core.HueCorrection;

/** Hue-correction clamping and the pinned stocks. */
public final class HueCorrectionCheck {

    private static int checks;

    private HueCorrectionCheck() {
    }

    public static void main(String[] args) {
        ok("a supported stock keeps the preference",
                HueCorrection.wire("kodak_portra_400", 1f) == 1f);
        ok("zero is a valid preference",
                HueCorrection.wire("kodak_portra_400", 0f) == 0f);
        ok("values above one clamp",
                HueCorrection.wire("kodak_portra_400", 4f) == 1f);
        ok("negative values clamp",
                HueCorrection.wire("kodak_portra_400", -2f) == 0f);
        ok("Vision3 is pinned to off",
                HueCorrection.wire("kodak_vision3_500t", 1f) == 0f);
        ok("CineStill 800T is pinned",
                HueCorrection.wire("cinestill_800t", 1f) == 0f);
        ok("Pro 400H is pinned",
                HueCorrection.wire("fujifilm_pro_400h", 1f) == 0f);
        ok("fixed() agrees with the wire value", HueCorrection.fixed("cinestill_800t"));
        ok("a normal stock is not fixed", !HueCorrection.fixed("kodak_portra_400"));
        ok("null is not fixed", !HueCorrection.fixed(null));
        ok("null stock still clamps", HueCorrection.wire(null, 2f) == 1f);

        System.out.println("HueCorrectionCheck: " + checks + " checks passed");
    }

    private static void ok(String what, boolean value) {
        ++checks;
        if (!value) {
            throw new AssertionError(what);
        }
    }
}

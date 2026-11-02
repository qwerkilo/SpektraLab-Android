package app.spektralab;

/**
 * Pure JVM contract for the optics rows' display-only Δ: each row measures
 * against its own panel default, prints a sign and two decimals, and shows a
 * plain zero when the value is at the default.
 */
public final class OpticsDeltaCheck {
    private static int checks = 0;

    public static void main(String[] args) {
        defaults();
        atDefault();
        signedText();
        rounding();
        extrema();

        System.out.println("OpticsDeltaCheck: " + checks + " checks passed");
    }

    private static void defaults() {
        require("exposure defaults to 1.00", OpticsDelta.EXPOSURE_DEFAULT == 1.0f);
        require("grain defaults to 1.00", OpticsDelta.GRAIN_DEFAULT == 1.0f);
        require("halation defaults to 1.00", OpticsDelta.HALATION_DEFAULT == 1.0f);
        require("glare defaults to 3.00", OpticsDelta.GLARE_DEFAULT == 3.0f);
    }

    private static void atDefault() {
        require("exposure at default shows 0.00",
                "0.00".equals(OpticsDelta.text(1.0f, OpticsDelta.EXPOSURE_DEFAULT)));
        require("grain at default shows 0.00",
                "0.00".equals(OpticsDelta.text(1.0f, OpticsDelta.GRAIN_DEFAULT)));
        require("halation at default shows 0.00",
                "0.00".equals(OpticsDelta.text(1.0f, OpticsDelta.HALATION_DEFAULT)));
        require("glare at default shows 0.00",
                "0.00".equals(OpticsDelta.text(3.0f, OpticsDelta.GLARE_DEFAULT)));
    }

    private static void signedText() {
        require("exposure above default is signed",
                "+0.25".equals(OpticsDelta.text(1.25f, OpticsDelta.EXPOSURE_DEFAULT)));
        require("exposure below default is signed",
                "-0.10".equals(OpticsDelta.text(0.90f, OpticsDelta.EXPOSURE_DEFAULT)));
        require("grain above default is signed",
                "+0.50".equals(OpticsDelta.text(1.50f, OpticsDelta.GRAIN_DEFAULT)));
        require("halation below default is signed",
                "-0.60".equals(OpticsDelta.text(0.40f, OpticsDelta.HALATION_DEFAULT)));
        require("glare below default is signed",
                "-1.00".equals(OpticsDelta.text(2.0f, OpticsDelta.GLARE_DEFAULT)));
        require("two decimals stay two decimals",
                "+1.20".equals(OpticsDelta.text(2.20f, OpticsDelta.EXPOSURE_DEFAULT)));
    }

    private static void rounding() {
        require("a change under half a hundredth shows 0.00",
                "0.00".equals(OpticsDelta.text(1.004f, OpticsDelta.EXPOSURE_DEFAULT)));
        require("a change over half a hundredth shows 0.01",
                "+0.01".equals(OpticsDelta.text(1.006f, OpticsDelta.EXPOSURE_DEFAULT)));
        require("near-default never prints minus zero",
                !"-0.00".equals(OpticsDelta.text(0.999f, OpticsDelta.EXPOSURE_DEFAULT)));
    }

    private static void extrema() {
        require("the exposure low end is a full negative delta",
                "-0.75".equals(OpticsDelta.text(0.25f, OpticsDelta.EXPOSURE_DEFAULT)));
        require("the exposure high end is a full positive delta",
                "+3.00".equals(OpticsDelta.text(4.0f, OpticsDelta.EXPOSURE_DEFAULT)));
        require("glare off is a full negative delta",
                "-3.00".equals(OpticsDelta.text(0.0f, OpticsDelta.GLARE_DEFAULT)));
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

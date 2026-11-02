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
        ranges();
        valueText();
        defaultSeam();
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
        require("the exposure row uses EXPOSURE_DEFAULT",
                OpticsDelta.Param.EXPOSURE.defaultValue == OpticsDelta.EXPOSURE_DEFAULT);
        require("the grain row uses GRAIN_DEFAULT",
                OpticsDelta.Param.GRAIN.defaultValue == OpticsDelta.GRAIN_DEFAULT);
        require("the halation row uses HALATION_DEFAULT",
                OpticsDelta.Param.HALATION.defaultValue == OpticsDelta.HALATION_DEFAULT);
        require("the glare row uses GLARE_DEFAULT",
                OpticsDelta.Param.GLARE.defaultValue == OpticsDelta.GLARE_DEFAULT);
    }

    private static void ranges() {
        require("exposure range is 0.25 to 4.00",
                OpticsDelta.Param.EXPOSURE.min == 0.25f
                        && OpticsDelta.Param.EXPOSURE.max == 4.0f);
        require("grain range is 0.00 to 2.00",
                OpticsDelta.Param.GRAIN.min == 0.0f
                        && OpticsDelta.Param.GRAIN.max == 2.0f);
        require("halation range is 0.00 to 4.00",
                OpticsDelta.Param.HALATION.min == 0.0f
                        && OpticsDelta.Param.HALATION.max == 4.0f);
        require("glare range is 0.00 to 12.00",
                OpticsDelta.Param.GLARE.min == 0.0f
                        && OpticsDelta.Param.GLARE.max == 12.0f);
        require("four optics parameters", OpticsDelta.Param.values().length == 4);
        require("exposure announces EV", "EV".equals(OpticsDelta.Param.EXPOSURE.unit));
        require("grain announces the multiplier sign",
                "\u00d7".equals(OpticsDelta.Param.GRAIN.unit));
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            require(param.name() + " default sits inside its range",
                    param.defaultValue >= param.min && param.defaultValue <= param.max);
        }
    }

    private static void valueText() {
        require("a whole value keeps two decimals", "1.00".equals(OpticsDelta.valueText(1.0f)));
        require("the exposure floor keeps two decimals",
                "0.25".equals(OpticsDelta.valueText(0.25f)));
        require("zero keeps two decimals", "0.00".equals(OpticsDelta.valueText(0.0f)));
        require("the glare ceiling keeps two decimals",
                "12.00".equals(OpticsDelta.valueText(12.0f)));
    }

    /** The reset seam: a value that reads as the default is already there. */
    private static void defaultSeam() {
        require("a default value is at the default", OpticsDelta.isDefault(1.0f, 1.0f));
        require("rounding noise stays at the default",
                OpticsDelta.isDefault(1.0004f, 1.0f));
        require("a visible step leaves the default",
                !OpticsDelta.isDefault(1.0006f, 1.0f));
        require("negative rounding noise stays at the default",
                OpticsDelta.isDefault(0.9996f, 1.0f));
        require("a negative visible step leaves the default",
                !OpticsDelta.isDefault(0.9994f, 1.0f));
        require("a parameter answers the same question",
                OpticsDelta.Param.GLARE.isDefault(3.0f));
        require("a moved parameter is not at its default",
                !OpticsDelta.Param.GLARE.isDefault(3.01f));
        require("a parameter clamps its own range",
                OpticsDelta.Param.EXPOSURE.clamp(9.0f) == 4.0f
                        && OpticsDelta.Param.EXPOSURE.clamp(0.0f) == 0.25f);
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

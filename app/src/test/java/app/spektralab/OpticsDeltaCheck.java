package app.spektralab;

/**
 * Pure JVM contract for the optics rows' display-only Δ: each row measures
 * against its own panel default, prints a sign and two decimals, and shows a
 * plain zero when the value is at the default.
 *
 * <p>It also pins the parameter contract the strip, the slider, the Look JSON,
 * and the reset all read: stable wire ids, labels, glyphs, real ranges, units,
 * steps, and defaults for all 22 engine parameters, plus the camera filter and
 * protection norm choices.
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
        parameterContract();
        choices();
        stripSelection();
        wireMapping();

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

    /** Every parameter the engine exposes, in the order the strip presents. */
    private static final String[] WIRES = {
            "exp", "highlight", "shadow", "grain", "halation", "boost", "scatter",
            "glare", "lens", "yf", "mf", "fscale", "preflash", "rolloff", "lift",
            "halon", "ahrem", "grainon", "coupon", "scw", "scb", "edr"};

    private static void parameterContract() {
        final OpticsDelta.Param[] params = OpticsDelta.Param.values();
        require("twenty-two optical parameters", params.length == 22);
        require("the wire ids are the engine's parameter names", params.length == WIRES.length);
        for (int i = 0; i < params.length; ++i) {
            final OpticsDelta.Param p = params[i];
            require("param " + i + " keeps its stable wire id", WIRES[i].equals(p.wire));
            require(p.name() + " has a label", p.label != null && !p.label.isEmpty());
            require(p.name() + " has a glyph", p.glyph != null && !p.glyph.isEmpty());
            require(p.name() + " has a unit", p.unit != null && !p.unit.isEmpty());
            require(p.name() + " has a positive step", p.step > 0f);
            require(p.name() + " has a real range", p.min < p.max);
            require(p.name() + " clamps below", p.clamp(p.min - 1f) == p.min);
            require(p.name() + " clamps above", p.clamp(p.max + 1f) == p.max);
            require(p.name() + " keeps the default", p.clamp(p.defaultValue) == p.defaultValue);
            require(p.name() + " resets to its default",
                    p.isDefault(p.defaultValue) && !p.isDefault(p.max == p.defaultValue
                            ? p.min : p.max));
            require(p.name() + " has a stable engine key",
                    p.engineKey != null && !p.engineKey.isEmpty());
            for (int j = i + 1; j < params.length; ++j) {
                require(p.name() + " wire is unique", !p.wire.equals(params[j].wire));
                require(p.name() + " engine key is unique",
                        !p.engineKey.equals(params[j].engineKey));
            }
        }
        require("highlight room range", OpticsDelta.Param.HIGHLIGHT_ROOM.min == 0f
                && OpticsDelta.Param.HIGHLIGHT_ROOM.max == 3f);
        require("lens blur is in micrometres", "µm".equals(OpticsDelta.Param.LENS_BLUR.unit)
                && OpticsDelta.Param.LENS_BLUR.max == 200f);
        require("filter shifts are signed", OpticsDelta.Param.Y_FILTER.min == -1f
                && OpticsDelta.Param.Y_FILTER.max == 1f);
    }

    private static void choices() {
        require("eight Wratten filters plus none",
                OpticsDelta.CameraFilter.values().length == 9);
        require("the default camera filter is none",
                OpticsDelta.CameraFilter.NONE.wire.isEmpty());
        for (OpticsDelta.CameraFilter filter : OpticsDelta.CameraFilter.values()) {
            require("filter " + filter.name() + " has a label",
                    filter.label != null && !filter.label.isEmpty());
        }
        for (String wire : new String[] {"w8", "w11", "w12", "w15", "w21", "w25", "w47", "w58"}) {
            require("filter " + wire + " is present",
                    OpticsDelta.CameraFilter.fromWire(wire) != OpticsDelta.CameraFilter.NONE);
        }
        require("unknown filter falls back to none",
                OpticsDelta.CameraFilter.fromWire("nope") == OpticsDelta.CameraFilter.NONE);

        require("three protection norms", OpticsDelta.ProtectionNorm.values().length == 3);
        require("the default norm is power",
                OpticsDelta.ProtectionNorm.POWER.wire.equals("power"));
        for (OpticsDelta.ProtectionNorm norm : OpticsDelta.ProtectionNorm.values()) {
            require("norm " + norm.name() + " has a label",
                    norm.label != null && !norm.label.isEmpty());
            require("norm " + norm.name() + " round-trips",
                    OpticsDelta.ProtectionNorm.fromWire(norm.wire) == norm);
        }
        require("unknown norm falls back to power",
                OpticsDelta.ProtectionNorm.fromWire("nope") == OpticsDelta.ProtectionNorm.POWER);
    }

    /**
     * The strip is a stable list of the contract's own parameters: index 0 is
     * exposure, the last index is EDR, and an out-of-range selection lands on an
     * end rather than crashing a chip tap.
     */
    private static void stripSelection() {
        require("the strip starts on exposure",
                OpticsDelta.paramAt(0) == OpticsDelta.Param.EXPOSURE);
        require("a selection below the strip lands on exposure",
                OpticsDelta.paramAt(-4) == OpticsDelta.Param.EXPOSURE);
        require("the strip keeps the contract order at index 7",
                OpticsDelta.paramAt(7) == OpticsDelta.Param.GLARE);
        require("the strip ends on EDR", OpticsDelta.paramAt(21) == OpticsDelta.Param.EDR_ON);
        require("a selection beyond the strip lands on EDR",
                OpticsDelta.paramAt(200) == OpticsDelta.Param.EDR_ON);
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            require(param.name() + " round-trips through its strip index",
                    OpticsDelta.paramAt(OpticsDelta.indexOf(param)) == param);
        }

        // Material Slider rejects a step that does not divide the range; the
        // contract is also what the strip's range announcement means.
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            final double steps = (param.max - param.min) / (double) param.step;
            require(param.name() + " step divides its range",
                    Math.abs(steps - Math.round(steps)) < 0.001);
            require(param.name() + " default lands on a slider tick",
                    param.snap(param.defaultValue) == param.defaultValue);
            require(param.name() + " snaps an out-of-range value to its cap",
                    param.snap(param.max + 50f) == param.max);
            require(param.name() + " snaps a low value into its range",
                    param.snap(param.min - 50f) == param.min);
            final float offGrid = param.min + 3.5f * param.step;
            final float snapped = param.snap(offGrid);
            final double ticks = (snapped - param.min) / (double) param.step;
            require(param.name() + " snaps a between-step value onto a tick",
                    Math.abs(ticks - Math.round(ticks)) < 0.001);
        }
        require("a glare above the M3 cap lands on the cap",
                OpticsDelta.Param.GLARE.snap(20f) == 12f);
        require("a switch between 0 and 1 lands on 1",
                OpticsDelta.Param.HALATION_ON.snap(0.7f) == 1f);
    }

    /** The two wire values whose UI meaning is not the engine's own number. */
    private static void wireMapping() {
        require("no highlight protection leaves the stage off",
                OpticsDelta.highlightRoomWire(0f) == 0f);
        require("the tightest protection is the 0.5-stop room floor",
                Math.abs(OpticsDelta.highlightRoomWire(3f) - 0.5f) < 0.0001f);
        require("half protection is half the room",
                Math.abs(OpticsDelta.highlightRoomWire(1.5f) - 1.75f) < 0.0001f);
        require("protection above the contract clamps to the floor",
                Math.abs(OpticsDelta.highlightRoomWire(9f) - 0.5f) < 0.0001f);
        require("the stage is off with both rooms at zero",
                !OpticsDelta.sceneLatitudeActive(0f, 0f));
        require("rounding noise does not switch the stage on",
                !OpticsDelta.sceneLatitudeActive(0.01f, 0f));
        require("a visible highlight room switches the stage on",
                OpticsDelta.sceneLatitudeActive(0.02f, 0f));
        require("a visible shadow room switches the stage on",
                OpticsDelta.sceneLatitudeActive(0f, 0.02f));
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

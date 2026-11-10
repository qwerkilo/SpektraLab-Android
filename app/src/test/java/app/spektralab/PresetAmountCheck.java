package app.spektralab;

/** Preset strength: continuous values cross-fade, categorical values snap. */
public final class PresetAmountCheck {

    private static int checks;

    public static void main(String[] args) {
        final Look base = new Look();
        base.filmStock = "kodak_portra_400";
        base.printStock = "kodak_portra_endura";
        base.grainAmount = 0.5f;
        base.geometryTurns = 0;
        base.view.temperature = 0f;
        base.view.cbHue[0] = 0f;

        final Look full = new Look();
        full.filmStock = "kodak_ektar_100";
        full.printStock = "kodak_supra_endura";
        full.grainAmount = 1.5f;
        full.geometryTurns = 180;
        full.view.temperature = 40f;
        full.view.cbHue[0] = 100f;

        final Look zero = PresetAmount.blend(base, full, 0f);
        eq("kodak_portra_400", zero.filmStock, "zero keeps film");
        eq(0.5f, zero.grainAmount, "zero keeps grain");
        eq(0, zero.geometryTurns, "zero keeps turns");
        eq(0f, zero.view.temperature, "zero keeps temperature");

        final Look one = PresetAmount.blend(base, full, 1f);
        eq("kodak_ektar_100", one.filmStock, "one takes film");
        eq(1.5f, one.grainAmount, "one takes grain");
        eq(180, one.geometryTurns, "one takes turns");
        eq(40f, one.view.temperature, "one takes temperature");

        final Look half = PresetAmount.blend(base, full, 0.5f);
        eq(1.0f, half.grainAmount, "half interpolates grain");
        eq(20f, half.view.temperature, "half interpolates view");
        eq(50f, half.view.cbHue[0], "half interpolates array");
        eq("kodak_ektar_100", half.filmStock, "half snaps film");
        eq(180, half.geometryTurns, "half snaps turns");

        final Look clamped = PresetAmount.blend(base, full, 3f);
        eq(1.5f, clamped.grainAmount, "amount clamps high");
        final Look below = PresetAmount.blend(base, full, -2f);
        eq(0.5f, below.grainAmount, "amount clamps low");
        eq(0.5f, base.grainAmount, "base not mutated");

        System.out.println("PresetAmountCheck: " + checks + " checks passed");
    }

    private static void eq(Object want, Object got, String label) {
        ++checks;
        if (!java.util.Objects.equals(want, got)) {
            throw new AssertionError(label + ": want " + want + ", got " + got);
        }
    }

    private static void eq(float want, float got, String label) {
        ++checks;
        if (Math.abs(want - got) > 1e-5f) {
            throw new AssertionError(label + ": want " + want + ", got " + got);
        }
    }
}

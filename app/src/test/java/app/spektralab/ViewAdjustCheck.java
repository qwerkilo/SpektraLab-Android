package app.spektralab;

/** Pure JVM contract for the print-view adjustment layer. */
public final class ViewAdjustCheck {
    private static int checks;

    public static void main(String[] args) {
        final ViewAdjust a = new ViewAdjust();
        for (ViewAdjust.Field field : ViewAdjust.Field.values()) {
            near(field.defaultValue, field.get(a));
        }
        ok(a.isDefault());

        ViewAdjust.Field.TEMPERATURE.set(a, 25f);
        ViewAdjust.Field.CONTRAST.set(a, -40f);
        ViewAdjust.Field.SHADOW_HUE.set(a, 210f);
        ViewAdjust.Field.GLOBAL_LUM.set(a, 12f);
        ok(!a.isDefault());

        final double[] nativeValues = a.toNative();
        eq(25, nativeValues.length);
        near(25f, (float) nativeValues[0]);
        near(-40f, (float) nativeValues[3]);
        near(210f, (float) nativeValues[12]);
        near(12f, (float) nativeValues[23]);

        final ViewAdjust copy = a.copy();
        near(25f, copy.temperature);
        near(-40f, copy.contrast);
        near(210f, copy.cbHue[0]);
        near(12f, copy.cbLum[3]);

        final Look look = new Look();
        look.view = copy;
        final Look restored = Look.fromJson(look.toJson());
        near(25f, restored.view.temperature);
        near(-40f, restored.view.contrast);
        near(210f, restored.view.cbHue[0]);
        near(12f, restored.view.cbLum[3]);

        // Old Looks have no view object and must read as zero, not as null.
        final Look old = Look.fromJson(new org.json.JSONObject());
        ok(old.view != null && old.view.isDefault());

        copy.reset(ViewAdjust.Group.TONE);
        near(0f, copy.contrast);
        near(25f, copy.temperature);

        System.out.println("ViewAdjustCheck: " + checks + " checks passed");
    }

    private static void ok(boolean value) {
        ++checks;
        if (!value) throw new AssertionError("check failed");
    }

    private static void eq(int expected, int actual) {
        ++checks;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }

    private static void near(float expected, float actual) {
        ++checks;
        if (Math.abs(expected - actual) > 0.001f) {
            throw new AssertionError(expected + " != " + actual);
        }
    }
}

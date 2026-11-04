package app.spektralab;

/** Pure JVM contract for the composition values and their engine keys. */
public final class CompositionCheck {
    private static int checks;

    public static void main(String[] args) {
        final Look look = new Look();
        look.filmStock = "cinestill_800t";
        look.geometryTurns = 2;
        look.geometryRotation = -12.5f;
        look.geometryCropX = 0.1f;
        look.geometryCropY = 0.2f;
        look.geometryCropW = 0.7f;
        look.geometryCropH = 0.6f;
        look.geometryFlipH = true;
        look.geometryFlipV = false;

        final Look restored = Look.fromJson(look.toJson());
        eq(2, restored.geometryTurns);
        near(-12.5f, restored.geometryRotation);
        near(0.1f, restored.geometryCropX);
        near(0.7f, restored.geometryCropW);
        ok(restored.geometryFlipH);
        ok(!restored.geometryFlipV);

        final String json = LookRestatement.json(look,
                new LookRestatement.Context(false, 2048, false, "CINESTILL 800T", ""));
        ok(json.contains("\"geometry_crop_x\":0.100000"));
        ok(json.contains("\"geometry_crop_y\":0.200000"));
        ok(json.contains("\"geometry_crop_w\":0.700000"));
        ok(json.contains("\"geometry_crop_h\":0.600000"));
        ok(json.contains("\"geometry_rotation_deg\":-12.500"));
        ok(json.contains("\"geometry_quarter_turns\":2"));
        ok(json.contains("\"geometry_flip_h\":true"));
        ok(json.contains("\"geometry_flip_v\":false"));

        final Look fresh = Look.fromJson(new org.json.JSONObject());
        eq(0, fresh.geometryTurns);
        near(0f, fresh.geometryCropX);
        near(1f, fresh.geometryCropW);
        near(1f, fresh.geometryCropH);
        ok(!fresh.geometryFlipH && !fresh.geometryFlipV);

        System.out.println("CompositionCheck: " + checks + " checks passed");
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
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new AssertionError(expected + " != " + actual);
        }
    }
}

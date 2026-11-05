package app.spektralab;

import org.json.JSONObject;

/** Pure JVM contract for the one render snapshot preview/export share. */
public final class RenderSnapshotCheck {
    private static int checks;

    public static void main(String[] args) {
        detachesTheLook();
        carriesTheStockAndContext();
        carriesTheViewValues();
        rejectsMissingInputs();
        System.out.println("RenderSnapshotCheck: " + checks + " checks passed");
    }

    private static void detachesTheLook() {
        final Look original = new Look();
        original.filmStock = "kodak_portra_400";
        original.printExposure = 1.4f;
        final RenderSnapshot snapshot = snapshot(original);
        original.printExposure = 3.0f;
        near(1.4f, snapshot.look.printExposure);
        near(3.0f, original.printExposure);
        ok(!snapshot.params.equals(snapshot(original).params),
                "a later Look edit must not alter an existing snapshot");
    }

    private static void carriesTheStockAndContext() {
        final RenderSnapshot snapshot = snapshot(new Look());
        eq("kodak_portra_400", snapshot.stock);
        eq(2048, snapshot.longEdge);
        final JSONObject json = new JSONObject(snapshot.params);
        eq("kodak_portra_400", json.optString("film_stock"));
        eq("2048", json.optString("preview_long_edge"));
        eq("EDGE", json.optString("overscan_edge_text"));
    }

    private static void carriesTheViewValues() {
        final Look look = new Look();
        look.view.temperature = 42f;
        final RenderSnapshot snapshot = snapshot(look);
        ok(snapshot.viewAdjust.length > 0, "view-adjust values are captured");
        near(42f, (float) snapshot.viewAdjust[0]);
    }

    private static void rejectsMissingInputs() {
        boolean threw = false;
        try {
            RenderSnapshot.of(null, "kodak_portra_400", 2048, context());
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        ok(threw, "a null Look is rejected");
    }

    private static RenderSnapshot snapshot(Look look) {
        return RenderSnapshot.of(look, "kodak_portra_400", 2048, context());
    }

    private static LookRestatement.Context context() {
        return new LookRestatement.Context(false, 2048, false, "EDGE", "");
    }

    private static void ok(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void near(float expected, float actual) {
        checks++;
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }
}

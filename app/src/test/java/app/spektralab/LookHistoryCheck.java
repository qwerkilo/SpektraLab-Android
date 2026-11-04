package app.spektralab;

/** Pure JVM contract for the per-Frame Look undo history. */
public final class LookHistoryCheck {
    private static int checks;

    public static void main(String[] args) {
        final LookHistory history = new LookHistory();
        final Look first = new Look();
        history.reset(first);
        eq(1, history.size());
        noUndo(history);

        final Look second = first.copy();
        second.grainAmount = 1.75f;
        history.record(second);
        ok(history.canUndo());
        eq(2, history.size());

        // A repeated state must not create a step.
        history.record(second.copy());
        eq(2, history.size());

        final Look restored = history.undo();
        ok(restored != null);
        near(OpticsDelta.GRAIN_DEFAULT, restored.grainAmount);
        noUndo(history);

        // A new edit after undo starts a fresh branch from the restored state.
        final Look third = restored.copy();
        third.halationAmount = 2.5f;
        history.record(third);
        ok(history.canUndo());
        final Look thirdRestored = history.undo();
        near(OpticsDelta.GRAIN_DEFAULT, thirdRestored.grainAmount);
        near(OpticsDelta.HALATION_DEFAULT, thirdRestored.halationAmount);

        // LIMIT trims the oldest state, not the newest.
        final LookHistory bounded = new LookHistory();
        final Look base = new Look();
        bounded.reset(base);
        for (int i = 0; i < LookHistory.LIMIT + 20; ++i) {
            final Look next = base.copy();
            next.grainAmount = 0.1f + (i % 15) * 0.01f;
            bounded.record(next);
        }
        eq(LookHistory.LIMIT, bounded.size());

        history.clear();
        noUndo(history);
        eq(0, history.size());

        System.out.println("LookHistoryCheck: " + checks + " checks passed");
    }

    private static void noUndo(LookHistory history) {
        ok(!history.canUndo());
        ok(history.undo() == null);
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

package app.spektralab;

import app.spektralab.core.ImportBudget;

/** Checks the pure import-scale arithmetic used by the budget dialog. */
public final class ImportBudgetCheck {
    private static int checks;

    private ImportBudgetCheck() {
    }

    public static void main(String[] args) {
        near(1.0, ImportBudget.scale(100, 50, 1_000_000, 8000, 0));
        near(Math.sqrt(0.25), ImportBudget.scale(200, 100, 5_000, 8000, 0));
        near(0.5, ImportBudget.scale(1000, 500, 10_000_000, 500, 0));
        near(Math.sqrt(0.5), ImportBudget.scale(1000, 500, 10_000_000, 8000, (long) (500 * 500 * 12 + (128L << 20))));
        near(1.0, ImportBudget.scale(1, 1, 1, 1, 0));
        near(1.0, ImportBudget.scale(0, 10, 100, 8000, 0));
        near(1.0, ImportBudget.scale(10, 10, 0, 8000, 0));
        near(0.25, ImportBudget.scale(400, 400, 10_000, 100, 0));
        System.out.println("ImportBudgetCheck: " + checks + " checks passed");
    }

    private static void near(double expected, double actual) {
        if (Math.abs(expected - actual) > 1e-6) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
        checks++;
    }

    private static void ok(String text) {
        checks++;
        if (text == null) {
            throw new AssertionError("null");
        }
    }
}

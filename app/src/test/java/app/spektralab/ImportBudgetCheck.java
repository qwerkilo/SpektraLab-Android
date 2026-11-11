package app.spektralab;

import app.spektralab.core.ImportBudget;

/** Checks the pure import-size arithmetic shared by the budget dialog and decoder. */
public final class ImportBudgetCheck {
    private static int checks;

    private ImportBudgetCheck() {
    }

    public static void main(String[] args) {
        near(1.0, ImportBudget.scale(100, 50, 1_000_000, 12000, 0));
        near(Math.sqrt(0.25), ImportBudget.scale(200, 100, 5_000, 12000, 0));
        near(0.5, ImportBudget.scale(1000, 500, 10_000_000, 500, 0));
        near(Math.sqrt(0.5), ImportBudget.scale(1000, 500, 10_000_000, 8000, 250_000));
        near(1.0, ImportBudget.scale(1, 1, 1, 1, 0));
        near(1.0, ImportBudget.scale(0, 10, 100, 12000, 0));
        near(1.0, ImportBudget.scale(10, 10, 0, 12000, 0));
        near(0.25, ImportBudget.scale(400, 400, 10_000, 100, 0));

        eq(50_000_000, ImportBudget.areaCap(0, 50_000_000), "memory-only cap");
        eq(20_000_000, ImportBudget.areaCap(20_000_000, 50_000_000), "native cap wins");
        eq(30_000_000, ImportBudget.areaCap(0, 30_000_000), "memory cap wins");
        eq(1_000_000, ImportBudget.areaCap(0, 0), "no memory is the floor");
        eq(1_000_000, ImportBudget.areaCap(0, 1), "floor is one megapixel");
        System.out.println("ImportBudgetCheck: " + checks + " checks passed");
    }

    private static void near(double expected, double actual) {
        if (Math.abs(expected - actual) > 1e-6) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
        checks++;
    }

    private static void eq(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
        checks++;
    }
}

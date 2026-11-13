package app.spektralab;

import app.spektralab.core.BatchPlan;

/**
 * Batch export order and progress labels.
 */
public final class BatchPlanCheck {

    private static int checks;

    public static void main(String[] args) {
        final BatchPlan none = BatchPlan.of(new boolean[]{false, false});
        if (!none.isEmpty() || none.size() != 0 || none.totalFrames() != 2) {
            throw new AssertionError("all-skipped plan must be empty but remember the total");
        }
        checks += 3;

        final BatchPlan mixed = BatchPlan.of(new boolean[]{true, false, true, false, true});
        if (mixed.size() != 3) {
            throw new AssertionError("eligible frames: expected 3, got " + mixed.size());
        }
        checks++;
        eq(0, mixed.frameAt(0), "first frame");
        eq(2, mixed.frameAt(1), "second frame skips the ineligible one");
        eq(4, mixed.frameAt(2), "third frame");
        eq(5, mixed.totalFrames(), "total includes skipped frames");
        eq("批量导出 1/3（跳过 2 帧）", mixed.label(0), "first label names the skipped frames");
        eq("批量导出 3/3（跳过 2 帧）", mixed.label(2), "last label uses the eligible count");

        final BatchPlan all = BatchPlan.of(new boolean[]{true, true});
        eq(2, all.size(), "all eligible");
        eq("批量导出 2/2", all.label(1), "all eligible label has no skip suffix");

        System.out.println("BatchPlanCheck: " + checks + " checks passed");
    }

    private static void eq(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
        checks++;
    }

    private static void eq(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
        checks++;
    }
}

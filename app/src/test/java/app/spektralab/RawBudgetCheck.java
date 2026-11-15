package app.spektralab;

import app.spektralab.core.RawBudgetNotice;

/** The oversized-RAW notice's numbers and wording. */
public final class RawBudgetCheck {

    private static int checks;

    private RawBudgetCheck() {
    }

    public static void main(String[] args) {
        eq("24.0", RawBudgetNotice.megapixels(6000, 4000));
        eq("6.0", RawBudgetNotice.megapixels(3000, 2000));
        eq("0.2", RawBudgetNotice.megapixels(500, 400));

        final String text = RawBudgetNotice.text(6000, 4000, 3000, 2000, 55.2);
        eq("原图 6000×4000（24.0 MP），这台机器一次能渲染约 55.2 MP，已按 3000×2000 导入。"
                + "\n\n按原尺寸导入可能渲染失败（E-1001）。", text);

        final String small = RawBudgetNotice.text(3000, 2000, 3000, 2000, 55.2);
        contains(small, "已按 3000×2000 导入");
        contains(small, "6.0 MP");

        System.out.println("RawBudgetCheck: " + checks + " checks passed");
    }

    private static void eq(String expected, String actual) {
        ++checks;
        if (!expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> got <" + actual + ">");
        }
    }

    private static void contains(String text, String needle) {
        ++checks;
        if (!text.contains(needle)) {
            throw new AssertionError("missing: " + needle);
        }
    }
}

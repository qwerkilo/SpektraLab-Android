package app.spektralab.core;

import java.util.Locale;

/** The oversized-RAW notice: the device's budget, the file's size, the result. */
public final class RawBudgetNotice {

    private RawBudgetNotice() {
    }

    /** One-decimal megapixels, the unit the notice and the budget sheet use. */
    public static String megapixels(int width, int height) {
        return String.format(Locale.US, "%.1f", (double) width * (double) height / 1e6);
    }

    public static String text(int rawWidth, int rawHeight, int width, int height,
                              double budgetMegapixels) {
        return "原图 " + rawWidth + "×" + rawHeight + "（"
                + megapixels(rawWidth, rawHeight)
                + " MP），这台机器一次能渲染约 "
                + String.format(Locale.US, "%.1f", budgetMegapixels)
                + " MP，已按 " + width + "×" + height + " 导入。"
                + "\n\n按原尺寸导入可能渲染失败（E-1001）。";
    }
}

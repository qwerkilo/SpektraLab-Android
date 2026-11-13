package app.spektralab.ui.panel;

import java.util.function.IntConsumer;

/**
 * The export destination sheet: which formats the user can pick, and how each
 * row maps to a writer call.
 *
 * <p>The sheet used to be two menus -- format, then size -- and the first thing
 * that hid was "全尺寸（原图）" behind a tap nobody expected. It is one list now,
 * so the mapping lives here instead of in the composition root.
 *
 * <p>The explanatory note is built here too; the Activity only supplies the two
 * runtime facts (whether the device is import-limited, and the driver line).
 * The rows, routing and note are covered by {@code ExportSheetCheck}.
 */
public final class ExportSheet {

    /** One export row: label, writer format, render tier, and batch marker. */
    public static final class Choice {
        public final String label;
        public final String format;
        public final String tier;
        public final boolean batch;

        Choice(String label, String format, String tier, boolean batch) {
            this.label = label;
            this.format = format;
            this.tier = tier;
            this.batch = batch;
        }
    }

    /** Every row of the single export sheet, in display order. */
    public static final Choice[] CHOICES = {
            new Choice("JPEG · 实时画质", "jpeg", "live", false),
            new Choice("JPEG · 预览画质 · 长边 3400", "jpeg", "preview", false),
            new Choice("JPEG · 全尺寸（原图）", "jpeg", "full", false),
            new Choice("TIFF 16 位 · 实时画质", "tiff", "live", false),
            new Choice("TIFF 16 位 · 预览画质 · 长边 3400", "tiff", "preview", false),
            new Choice("TIFF 16 位 · 全尺寸（原图）", "tiff", "full", false),
            new Choice("DNG · 全尺寸（电脑用 · 手机版打不开）", "dng", "full", false),
            new Choice("批量导出全部帧…", "", "", true)
    };

    /** Batch rows; every one reopens each frame before writing it. */
    public static final Choice[] BATCH_CHOICES = {
            new Choice("JPEG · 实时画质", "jpeg", "live", false),
            new Choice("JPEG · 全尺寸", "jpeg", "full", false),
            new Choice("TIFF 16 位 · 全尺寸", "tiff", "full", false),
            new Choice("DNG · 全尺寸", "dng", "full", false)
    };

    /** What the sheet needs from the Activity. */
    public interface Host {
        /** The explanatory note under the title. */
        String note();

        /** Show a bottom sheet with one row per label. */
        void showSheet(String title, String note, String[] rows, IntConsumer onPick);

        /** Write one frame with the chosen writer. */
        void export(String format, String tier);

        /** Start the batch export for the chosen writer. */
        void exportBatch(String format, String tier);
    }

    private final Host host;

    public ExportSheet(Host host) {
        this.host = host;
    }

    /**
     * The explanatory note: what 16-bit TIFF means on this phone, and that DNG
     * is a desktop hand-off. The Activity supplies the two runtime facts.
     */
    public static String note(boolean limited, String driverLine) {
        return (limited
                ? "TIFF 是 16 位；"
                  + "导出后点导出旁边的「分享」，分享到 Lightroom 即可使用（手机相册多半不显示 TIFF）。JPEG 哪儿都能看。"
                : "TIFF 是 16 位；"
                  + "导出后点导出旁边的「分享」，分享到 Lightroom 即可使用（手机相册多半不显示 TIFF）。JPEG 是 8 位、哪儿都能看")
                + "\n" + driverLine
                + "\nDNG 只给电脑上的 Lightroom / ACR 用：手机版 Lr 打不开是已知问题，"
                  + "遇到报错不必反馈。";
    }

    /** Show the single export sheet. */
    public void show() {
        host.showSheet("导出照片", host.note(), labels(CHOICES), which -> pick(which));
    }

    private void pick(int which) {
        if (which < 0 || which >= CHOICES.length) {
            return;
        }
        final Choice choice = CHOICES[which];
        if (choice.batch) {
            showBatch();
            return;
        }
        host.export(choice.format, choice.tier);
    }

    /** Show the batch format sheet. */
    public void showBatch() {
        host.showSheet("批量导出全部帧",
                "逐帧重开并导出；没有胶片的帧会跳过",
                labels(BATCH_CHOICES), which -> {
                    if (which >= 0 && which < BATCH_CHOICES.length) {
                        host.exportBatch(BATCH_CHOICES[which].format, BATCH_CHOICES[which].tier);
                    }
                });
    }

    static String[] labels(Choice[] choices) {
        final String[] out = new String[choices.length];
        for (int i = 0; i < choices.length; ++i) {
            out[i] = choices[i].label;
        }
        return out;
    }
}

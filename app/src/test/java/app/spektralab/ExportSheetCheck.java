package app.spektralab;

import app.spektralab.ui.panel.ExportSheet;

/**
 * Pins the export sheet's rows and routing. A single mis-ordered row silently
 * writes the wrong container, which the format tests cannot see because they
 * call the writer directly.
 */
public final class ExportSheetCheck {

    private static int checks;

    public static void main(String[] args) {
        require("single sheet has eight rows", ExportSheet.CHOICES.length == 8);
        require("last row is the batch entry",
                ExportSheet.CHOICES[7].batch && ExportSheet.CHOICES[7].label.endsWith("…"));
        require("batch entry has no writer",
                ExportSheet.CHOICES[7].format.isEmpty() && ExportSheet.CHOICES[7].tier.isEmpty());
        for (int i = 0; i < 3; ++i) {
            require("JPEG row " + i + " is jpeg", "jpeg".equals(ExportSheet.CHOICES[i].format));
        }
        require("first row is live", "live".equals(ExportSheet.CHOICES[0].tier));
        require("second row is preview", "preview".equals(ExportSheet.CHOICES[1].tier));
        require("third row is full size", "full".equals(ExportSheet.CHOICES[2].tier));
        for (int i = 3; i < 6; ++i) {
            require("TIFF row " + i + " is tiff", "tiff".equals(ExportSheet.CHOICES[i].format));
        }
        require("TIFF full row exists", "full".equals(ExportSheet.CHOICES[5].tier));
        require("DNG is full size only",
                "dng".equals(ExportSheet.CHOICES[6].format)
                        && "full".equals(ExportSheet.CHOICES[6].tier));
        require("batch sheet has four rows", ExportSheet.BATCH_CHOICES.length == 4);
        require("batch starts with live JPEG",
                "jpeg".equals(ExportSheet.BATCH_CHOICES[0].format)
                        && "live".equals(ExportSheet.BATCH_CHOICES[0].tier));
        require("batch ends with DNG",
                "dng".equals(ExportSheet.BATCH_CHOICES[3].format)
                        && "full".equals(ExportSheet.BATCH_CHOICES[3].tier));
        require("batch rows are not nested batches",
                !ExportSheet.BATCH_CHOICES[0].batch && !ExportSheet.BATCH_CHOICES[3].batch);
        plusOne();
        System.out.println("ExportSheetCheck: " + checks + " checks passed");
    }

    private static void plusOne() {
        ++checks;
    }

    private static void require(String name, boolean ok) {
        plusOne();
        if (!ok) {
            throw new AssertionError(name);
        }
    }

    private ExportSheetCheck() {
    }
}

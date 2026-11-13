package app.spektralab;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The export boundary: one module owns the engine writers and the MediaStore
 * row. A second copy in the Activity was how the export path drifted before.
 */
public final class ExportOwnershipCheck {

    private static int checks;

    private ExportOwnershipCheck() {
    }

    public static void main(String[] args) throws Exception {
        final String activity = read("app/src/main/java/app/spektralab/MainActivity.java");
        final String runner = read("app/src/main/java/app/spektralab/export/ExportRunner.java");

        absent(activity, "target.exportJpeg(");
        absent(activity, "target.exportTiff(");
        absent(activity, "target.exportDng(");
        absent(activity, "EXTERNAL_CONTENT_URI");
        absent(activity, "IS_PENDING");
        absent(activity, "GalleryExport.write(");
        present(activity, "ExportRunner.start(");

        present(runner, "exportJpeg(");
        present(runner, "exportTiff(");
        present(runner, "exportDng(");
        present(runner, "EXTERNAL_CONTENT_URI");
        present(runner, "IS_PENDING");
        present(runner, "GalleryExport.write(");

        System.out.println("ExportOwnershipCheck: " + checks + " checks passed");
    }

    private static String read(String path) throws Exception {
        final String text = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        ++checks;
        if (text.isEmpty()) {
            throw new AssertionError("empty source: " + path);
        }
        return text;
    }

    private static void present(String text, String needle) {
        ++checks;
        if (!text.contains(needle)) {
            throw new AssertionError("missing: " + needle);
        }
    }

    private static void absent(String text, String needle) {
        ++checks;
        if (text.contains(needle)) {
            throw new AssertionError("must not appear in MainActivity: " + needle);
        }
    }
}

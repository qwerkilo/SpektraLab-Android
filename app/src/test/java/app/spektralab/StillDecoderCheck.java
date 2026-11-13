package app.spektralab;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** One owner for still decode, EXIF rotation and exact scaling. */
public final class StillDecoderCheck {

    private static int checks;

    private StillDecoderCheck() {
    }

    public static void main(String[] args) throws Exception {
        final String activity = read("app/src/main/java/app/spektralab/MainActivity.java");
        final String decoder = read("app/src/main/java/app/spektralab/importer/StillDecoder.java");

        absent(activity, "private Bitmap decodeImport(");
        absent(activity, "private Bitmap decodeViaImageDecoder(");
        absent(activity, "private Bitmap applyExifRotation(");
        absent(activity, "private Bitmap rotateForOrientation(");
        absent(activity, "private int readOrientation(");
        present(activity, "StillDecoder.decode(");

        present(decoder, "BitmapFactory.decodeStream(");
        present(decoder, "ImageDecoder.decodeBitmap(");
        present(decoder, "ImportBudget.scale(");
        present(decoder, "ExifTransform.of(");
        present(decoder, "import-scale");

        System.out.println("StillDecoderCheck: " + checks + " checks passed");
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

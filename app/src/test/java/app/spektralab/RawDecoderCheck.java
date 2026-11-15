package app.spektralab;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** The RAW source-preview decode lives in one module. */
public final class RawDecoderCheck {

    private static int checks;

    private RawDecoderCheck() {
    }

    public static void main(String[] args) throws Exception {
        final String activity = read("app/src/main/java/app/spektralab/MainActivity.java");
        final String decoder = read("app/src/main/java/app/spektralab/importer/RawDecoder.java");

        absent(activity, "decodeRawPreview(");
        present(activity, "RawDecoder.preview(");
        present(decoder, "exportSource(\"live\")");
        present(decoder, "copyArgbRange(");
        present(decoder, "raw-view");

        System.out.println("RawDecoderCheck: " + checks + " checks passed");
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

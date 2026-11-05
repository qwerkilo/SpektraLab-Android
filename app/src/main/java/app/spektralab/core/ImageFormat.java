package app.spektralab.core;

/** Filename-based format checks shared by the picker, decoder and export paths. */
public final class ImageFormat {
    private static final String[] RAW_EXTENSIONS = {
            ".rw2", ".raw", ".dng", ".arw", ".nef", ".cr2", ".cr3", ".orf",
            ".raf", ".pef", ".srw", ".x3f", ".rwl", ".3fr", ".mrw", ".erf",
    };
    private static final String[] UNSUPPORTED_RAW_EXTENSIONS = {
            ".heic.raw", ".iiq", ".mos",
    };

    private ImageFormat() {
    }

    /** True when the name ends in a RAW container this app can send to LibRaw. */
    public static boolean isRaw(String name) {
        final String lower = name == null ? "" : name.toLowerCase(java.util.Locale.US);
        for (String extension : RAW_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The known-but-unsupported suffix this name ends with, or null.
     *
     * <p>A RAW container we recognise but cannot open still beats decoding its
     * bytes as a JPEG and reporting a mystery.
     */
    public static String unsupportedRawExtension(String name) {
        final String lower = name == null ? "" : name.toLowerCase(java.util.Locale.US);
        for (String extension : UNSUPPORTED_RAW_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return extension;
            }
        }
        return null;
    }
}

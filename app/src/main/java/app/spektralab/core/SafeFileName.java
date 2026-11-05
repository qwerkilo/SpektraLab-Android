package app.spektralab.core;

/**
 * A file name safe to append to a known directory.
 *
 * <p>A content provider owns {@code DISPLAY_NAME}, so it can return a path
 * such as {@code ../../shared_prefs/app.xml}. The cache copy must stay inside
 * the cache directory: keep only the final path segment, reject the directory
 * markers, and fall back to a generated name when nothing usable remains.
 */
public final class SafeFileName {
    private SafeFileName() { }

    public static String of(String candidate, String fallback) {
        if (candidate == null) {
            return fallback;
        }
        if (candidate.indexOf('\0') >= 0) {
            return fallback;
        }
        final String name = new java.io.File(candidate).getName();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            return fallback;
        }
        return name;
    }
}

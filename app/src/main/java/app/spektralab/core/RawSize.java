package app.spektralab.core;

/** The device cap applied to a RAW decode, as a pure predicate. */
public final class RawSize {

    private RawSize() {
    }

    /** True when the source is larger than the session that was opened. */
    public static boolean isShrunk(int[] source, int width, int height) {
        return source != null && source.length == 2
                && (long) source[0] * source[1] > (long) width * height;
    }
}

package app.spektralab.core;

/**
 * Compares an exported JPEG with the canvas the user saw.
 *
 * <p>The comparison is a 48-column grayscale normalised cross-correlation. A
 * normal export scores about 0.97; a truncated or garbled file scores near 0.
 * TIFF and DNG cannot be decoded by {@code BitmapFactory}, so their callers
 * skip the check.
 */
public final class ExportMatch {
    /** Below this the file is named as suspect and exported once more. */
    public static final double MIN_NCC = 0.55;

    private ExportMatch() {
    }

    /**
     * Gray correlation of two equally sized pixel arrays.
     *
     * @param a first ARGB pixels
     * @param b second ARGB pixels
     * @return correlation in [-1, 1], or -1 when it cannot be computed
     */
    public static double ncc(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return -1;
        }
        double meanA = 0;
        double meanB = 0;
        for (int i = 0; i < a.length; i++) {
            meanA += luma(a[i]);
            meanB += luma(b[i]);
        }
        meanA /= a.length;
        meanB /= a.length;
        double num = 0;
        double da = 0;
        double db = 0;
        for (int i = 0; i < a.length; i++) {
            final double va = luma(a[i]) - meanA;
            final double vb = luma(b[i]) - meanB;
            num += va * vb;
            da += va * va;
            db += vb * vb;
        }
        if (da <= 1e-9 || db <= 1e-9) {
            return -1;
        }
        return num / Math.sqrt(da * db);
    }

    /** Whether a computed score should trigger the one automatic re-export. */
    public static boolean suspect(double ncc) {
        return ncc >= 0 && ncc < MIN_NCC;
    }

    private static double luma(int argb) {
        return ((argb & 0xFF) + ((argb >> 8) & 0xFF) + ((argb >> 16) & 0xFF)) / 3.0;
    }
}

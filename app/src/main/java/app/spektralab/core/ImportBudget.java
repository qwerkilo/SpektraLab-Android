package app.spektralab.core;

/** Pure import-size arithmetic shared by the budget dialog and the decoder. */
public final class ImportBudget {
    /** The memory the engine needs for one 12-byte working pixel. */
    private static final long BYTES_PER_PIXEL = 12L;
    /** Keep this much headroom for the rest of the process. */
    private static final long MEMORY_RESERVE_BYTES = 128L << 20;

    private ImportBudget() {
    }

    /**
     * The scale that keeps a bitmap inside every cap at once.
     *
     * @param width source width in pixels
     * @param height source height in pixels
     * @param capPixels largest area this device may render
     * @param longEdge longest allowed edge, or 0 for no edge cap
     * @param availableBytes free heap, or 0 when unknown
     * @return a scale in (0, 1]
     */
    public static double scale(int width, int height, int capPixels, int longEdge,
                               long availableBytes) {
        if (width <= 0 || height <= 0 || capPixels <= 0) {
            return 1.0;
        }
        final long area = (long) width * (long) height;
        double k = 1.0;
        if (area > capPixels) {
            k = Math.sqrt((double) capPixels / (double) area);
        }
        final int sourceLongEdge = Math.max(width, height);
        if (longEdge > 0 && sourceLongEdge > longEdge) {
            k = Math.min(k, (double) longEdge / sourceLongEdge);
        }
        if (availableBytes > 0) {
            final long budget = Math.max(1L << 20, availableBytes - MEMORY_RESERVE_BYTES);
            final double wantBytes = (double) area * BYTES_PER_PIXEL;
            if (wantBytes > budget) {
                k = Math.min(k, Math.sqrt((double) budget / wantBytes));
            }
        }
        return Math.min(k, 1.0);
    }
}

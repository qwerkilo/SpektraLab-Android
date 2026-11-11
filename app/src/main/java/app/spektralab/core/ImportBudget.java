package app.spektralab.core;

/** Pure import-size arithmetic shared by the budget dialog and the decoder. */
public final class ImportBudget {

    private static final long MIN_PIXELS = 1_000_000L;

    private ImportBudget() {
    }

    /**
     * The area cap: the native frame cap when the renderer has one, and the
     * memory estimate otherwise. A zero native cap means the banded renderer
     * has no binding ceiling; a zero memory cap means there is no room left,
     * so the floor is used rather than reading it as unlimited.
     */
    public static int areaCap(int nativeCapPixels, long memoryCapPixels) {
        long cap = memoryCapPixels > 0 ? memoryCapPixels : MIN_PIXELS;
        if (nativeCapPixels > 0) {
            cap = Math.min(cap, nativeCapPixels);
        }
        return (int) Math.min(Math.max(cap, MIN_PIXELS), Integer.MAX_VALUE);
    }

    /**
     * The scale that keeps a bitmap inside every cap at once.
     *
     * @param width source width in pixels
     * @param height source height in pixels
     * @param capPixels largest area this device may render
     * @param longEdge longest allowed edge, or 0 for no edge cap
     * @param memoryCapPixels the same memory estimate used for the area cap,
     *                        or 0 when unknown
     * @return a scale in (0, 1]
     */
    public static double scale(int width, int height, int capPixels, int longEdge,
                               long memoryCapPixels) {
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
        if (memoryCapPixels > 0 && area > memoryCapPixels) {
            k = Math.min(k, Math.sqrt((double) memoryCapPixels / (double) area));
        }
        return Math.min(k, 1.0);
    }
}

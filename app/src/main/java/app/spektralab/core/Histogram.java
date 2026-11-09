package app.spektralab.core;

/** Channel histograms for the displayed bitmap, with no view dependency. */
public final class Histogram {

    public static final int BINS = 64;

    private Histogram() {
    }

    /** 64 bins per channel; transparent pixels are ignored. */
    public static int[][] of(int[] pixels, int width, int height) {
        final int[][] bins = new int[3][BINS];
        if (pixels == null || width <= 0 || height <= 0) {
            return bins;
        }
        final int count = Math.min(pixels.length, width * height);
        for (int i = 0; i < count; ++i) {
            final int p = pixels[i];
            if (((p >>> 24) & 0xFF) < 8) {
                continue;
            }
            ++bins[0][((p >>> 16) & 0xFF) * BINS / 256];
            ++bins[1][((p >>> 8) & 0xFF) * BINS / 256];
            ++bins[2][(p & 0xFF) * BINS / 256];
        }
        return bins;
    }

    /** The tallest count across every channel; 0 for an empty histogram. */
    public static int max(int[][] bins) {
        if (bins == null) {
            return 0;
        }
        int max = 0;
        for (int[] channel : bins) {
            if (channel == null) {
                continue;
            }
            for (int value : channel) {
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }
}

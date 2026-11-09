package app.spektralab;

import app.spektralab.core.Histogram;

/** Bin edges and the empty cases for the histogram overlay. */
public final class HistogramCheck {

    private static int checks;

    private HistogramCheck() {
    }

    public static void main(String[] args) {
        final int[] pixels = {
                0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF,
                0x00FFFFFF, 0xFF808080};
        final int[][] bins = Histogram.of(pixels, 7, 1);

        ok("red 255 lands in the last bin", bins[0][63] == 2);
        ok("green 255 lands in the last bin", bins[1][63] == 2);
        ok("blue 255 lands in the last bin", bins[2][63] == 2);
        ok("transparent white is ignored", bins[0][63] == 2);
        ok("mid grey is in the middle", bins[0][32] == 1);
        ok("black is in bin 0", bins[0][0] == 3);
        ok("max counts every channel", Histogram.max(bins) == 3);

        ok("null pixels give empty bins",
                Histogram.max(Histogram.of(null, 10, 10)) == 0);
        ok("zero width gives empty bins",
                Histogram.max(Histogram.of(new int[]{0xFFFFFFFF}, 0, 1)) == 0);
        ok("a short array is clamped", Histogram.max(Histogram.of(
                new int[]{0xFFFFFFFF}, 10, 10)) == 1);
        ok("null bins have max 0", Histogram.max(null) == 0);
        ok("three channels", bins.length == 3);

        System.out.println("HistogramCheck: " + checks + " checks passed");
    }

    private static void ok(String what, boolean value) {
        ++checks;
        if (!value) {
            throw new AssertionError(what);
        }
    }
}

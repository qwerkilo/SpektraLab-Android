package app.spektralab;

/**
 * Pure JVM contract for the film panel's curve: the plot is built from the
 * shipped samples only. Gaps stay gaps, nulls are never zero, the value range
 * comes from the measured values, and channel order is the profile's own.
 *
 * <p>No Android, no JUnit: run it with the same javac/java that builds the app.
 */
public final class SpectrumCurveCheck {
    private static int checks = 0;

    public static void main(String[] args) {
        unavailableStates();
        shippedSeries();
        gapsAreGaps();
        rangeAndNormalisation();
        constantSeries();
        channelsAndRaggedRows();
        runPairingPerChannel();
        unpairedWavelengthsAreGaps();
        fractionAxis();

        System.out.println("SpectrumCurveCheck: " + checks + " checks passed");
    }

    private static void unavailableStates() {
        require("null input is unavailable", !SpectrumCurve.of(null, null).available());
        require("null wavelengths are unavailable",
                !SpectrumCurve.of(null, new double[][]{{1.0}}).available());
        require("null series is unavailable",
                !SpectrumCurve.of(new double[]{380}, null).available());
        require("empty arrays are unavailable",
                !SpectrumCurve.of(new double[0], new double[0][]).available());
        require("a single measured point is not a curve",
                !SpectrumCurve.of(new double[]{500, 550},
                        new double[][]{{Double.NaN}, {2.0}}).available());
        require("unavailable has no channels",
                SpectrumCurve.unavailable().channelCount() == 0);
        require("unavailable has no runs",
                SpectrumCurve.unavailable().runCount(0) == 0);
    }

    private static void shippedSeries() {
        final double[] wavelengths = new double[81];
        final double[][] series = new double[81][3];
        for (int i = 0; i < 81; ++i) {
            wavelengths[i] = 380 + i * 5;
            series[i][0] = -3.0 + i * 0.01;
            series[i][1] = -2.0 + i * 0.01;
            series[i][2] = -1.0 + i * 0.01;
        }
        final SpectrumCurve curve = SpectrumCurve.of(wavelengths, series);
        require("81 shipped samples are plottable", curve.available());
        require("sample count is 81", curve.sampleCount() == 81);
        require("range starts at 380 nm", close(curve.minNm(), 380));
        require("range ends at 780 nm", close(curve.maxNm(), 780));
        require("three channels are plotted", curve.channelCount() == 3);
    }

    private static void gapsAreGaps() {
        final SpectrumCurve curve = SpectrumCurve.of(
                new double[]{400, 450, 500, 550},
                new double[][]{{1.0}, null, {Double.NaN}, {4.0}});
        require("two real samples survive", curve.sampleCount() == 2);
        require("a gap splits the line", curve.runCount(0) == 2);
        require("the first run is one point", curve.runLength(0, 0) == 1);
        require("the second run is one point", curve.runLength(0, 1) == 1);
        require("the second run starts at the real sample",
                close(curve.nmAt(0, 1, 0), 550));
        require("no run bridges the gap", curve.runStart(0, 1) == 3);

        final SpectrumCurve joined = SpectrumCurve.of(
                new double[]{400, 450, 500, 550},
                new double[][]{{1.0}, {2.0}, {Double.NaN}, {4.0}});
        require("consecutive samples are one run", joined.runCount(0) == 2);
        require("the first run keeps both samples", joined.runLength(0, 0) == 2);
        require("the run after the gap is separate", joined.runStart(0, 1) == 3);
    }

    private static void rangeAndNormalisation() {
        final SpectrumCurve curve = SpectrumCurve.of(
                new double[]{400, 500, 600},
                new double[][]{{0.0, 1.0}, {10.0, 2.0}, {Double.NaN, 3.0}});
        require("range is measured from real values", close(curve.minValue(), 0.0));
        require("range ends at the largest real value", close(curve.maxValue(), 10.0));
        require("the smallest value plots at the bottom",
                close(curve.normalizedAt(0, 0, 0), 0.0));
        require("the largest value plots at the top",
                close(curve.normalizedAt(0, 0, 1), 1.0));
        require("a middle value keeps its ratio",
                close(curve.normalizedAt(1, 0, 1), 0.2));
    }

    private static void constantSeries() {
        final SpectrumCurve curve = SpectrumCurve.of(
                new double[]{400, 500},
                new double[][]{{5.0}, {5.0}});
        require("a flat series is still available", curve.available());
        require("a flat series draws on the middle line",
                close(curve.normalizedAt(0, 0, 0), 0.5));
        require("a flat series has one run", curve.runCount(0) == 1);
    }

    private static void channelsAndRaggedRows() {
        final SpectrumCurve curve = SpectrumCurve.of(
                new double[]{400, 500},
                new double[][]{{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
        require("the first value is the first channel", close(curve.rawAt(0, 0, 0), 1.0));
        require("the second value is the second channel", close(curve.rawAt(1, 0, 0), 2.0));
        require("the third value is the third channel", close(curve.rawAt(2, 0, 0), 3.0));
        require("the next row maps the same way", close(curve.rawAt(0, 0, 1), 4.0));
        require("channel 0 is red", "R".equals(SpectrumCurve.channelLabel(0)));
        require("channel 1 is green", "G".equals(SpectrumCurve.channelLabel(1)));
        require("channel 2 is blue", "B".equals(SpectrumCurve.channelLabel(2)));

        final SpectrumCurve ragged = SpectrumCurve.of(
                new double[]{400, 500},
                new double[][]{{1.0}, {2.0, 3.0}});
        require("the widest row sets the channel count", ragged.channelCount() == 2);
        require("a channel missing from a row is a gap",
                ragged.runCount(1) == 1 && ragged.runStart(1, 0) == 1);
        require("the present value is plotted", close(ragged.rawAt(1, 0, 0), 3.0));

        final SpectrumCurve noBlue = SpectrumCurve.of(
                new double[]{400, 500},
                new double[][]{{1.0, 2.0, Double.NaN}, {3.0, 4.0, Double.NaN}});
        require("a channel with only gaps draws no line", noBlue.runCount(2) == 0);
        require("the measured channels still draw", noBlue.runCount(0) == 1);
        require("a gap has no value to plot", Double.isNaN(noBlue.rawAt(2, 0, 0)));
    }

    /** Every channel owns its runs: one channel's gap never splits another. */
    private static void runPairingPerChannel() {
        final SpectrumCurve curve = SpectrumCurve.of(
                new double[]{400, 450, 500, 550, 600},
                new double[][]{
                        {1.0, 1.0, 1.0},
                        {2.0, Double.NaN, 2.0},
                        {3.0, 3.0, Double.NaN},
                        {4.0, 4.0, Double.NaN},
                        {5.0, 5.0, 5.0},
                });
        require("a fully sampled channel has one run", curve.runCount(0) == 1);
        require("the full run keeps every sample", curve.runLength(0, 0) == 5);

        require("a gap in the middle splits only its own channel", curve.runCount(1) == 2);
        require("the run before the gap is one point", curve.runLength(1, 0) == 1);
        require("the run after the gap keeps three points", curve.runLength(1, 1) == 3);
        require("the second green run starts at 500 nm", curve.runStart(1, 1) == 2);
        require("the second green run begins at 500 nm", close(curve.nmAt(1, 1, 0), 500));
        require("the second green run ends at 600 nm", close(curve.nmAt(1, 1, 2), 600));

        require("a channel with a gap has two runs", curve.runCount(2) == 2);
        require("the first blue run keeps both samples", curve.runLength(2, 0) == 2);
        require("the last blue run is a single point", curve.runLength(2, 1) == 1);
        require("a one-point run is never dropped", curve.runStart(2, 1) == 4);
        require("a one-point run still carries its wavelength", close(curve.nmAt(2, 1, 0), 600));

        final int[] realSamples = {5, 4, 3};
        for (int channel = 0; channel < curve.channelCount(); ++channel) {
            int plotted = 0;
            for (int run = 0; run < curve.runCount(channel); ++run) {
                require("run " + run + " of channel " + channel + " is non-empty",
                        curve.runLength(channel, run) >= 1);
                plotted += curve.runLength(channel, run);
            }
            require("channel " + channel + " plots only its real samples",
                    plotted == realSamples[channel]);
            require("a point outside the run has no value",
                    Double.isNaN(curve.rawAt(channel, curve.runCount(channel), 0)));
        }
    }

    /** A finite value on a row without a wavelength is not a paired sample. */
    private static void unpairedWavelengthsAreGaps() {
        final SpectrumCurve curve = SpectrumCurve.of(
                new double[]{400, Double.NaN, 500, 550},
                new double[][]{{1.0}, {2.0}, {3.0}, {4.0}});
        require("the unpaired row is not counted", curve.sampleCount() == 3);
        require("the finite row after the bad wavelength starts a new run",
                curve.runCount(0) == 2);
        require("the first run is the real sample before the gap",
                curve.runStart(0, 0) == 0 && curve.runLength(0, 0) == 1);
        require("the second run begins at the next real wavelength",
                curve.runStart(0, 1) == 2);
        require("the value on the unpaired row is never plotted",
                Double.isNaN(curve.rawAt(0, 0, 1)));
    }

    /** The plot and Scan inspect share one 0..1 horizontal axis. */
    private static void fractionAxis() {
        final SpectrumCurve curve = SpectrumCurve.of(
                new double[]{400, 500, 600},
                new double[][]{{1.0}, {2.0}, {3.0}});
        require("the first measured wavelength is the left edge",
                close(curve.fractionAtNm(400), 0.0));
        require("the middle wavelength is the axis midpoint",
                close(curve.fractionAtNm(500), 0.5));
        require("the last measured wavelength is the right edge",
                close(curve.fractionAtNm(600), 1.0));
        require("a wavelength below the range clamps to the left edge",
                close(curve.fractionAtNm(380), 0.0));
        require("a wavelength above the range clamps to the right edge",
                close(curve.fractionAtNm(780), 1.0));
        require("a missing wavelength has no fraction",
                Double.isNaN(curve.fractionAtNm(Double.NaN)));
        require("unavailable has no fraction axis",
                Double.isNaN(SpectrumCurve.unavailable().fractionAtNm(500)));

        final SpectrumCurve flat = SpectrumCurve.of(
                new double[]{500, 500},
                new double[][]{{1.0}, {2.0}});
        require("a zero-width range draws on the middle line",
                close(flat.fractionAtNm(500), 0.5));
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 1e-9;
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

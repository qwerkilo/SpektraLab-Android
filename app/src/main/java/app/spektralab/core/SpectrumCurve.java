package app.spektralab.core;

/**
 * The plottable form of a stock's shipped spectral series.
 *
 * <p>The film panel may only draw what the profile measured: every point keeps
 * its own wavelength, a gap (null, NaN, infinity, a missing channel, or a
 * missing row) ends the current line instead of being filled or plotted as
 * zero, and the value range is measured from the finite samples alone. Channel
 * order is the profile's own: row value 0 is red, 1 is green, 2 is blue.
 *
 * <p>It is deliberately Android-free so the JVM check can compile it without
 * the SDK. Values are normalised across every channel together, so a curve
 * compares the three layers the way the profile stores them.
 */
public final class SpectrumCurve {
    /** The only truthful label when a stock ships no plottable samples. */
    public static final String UNAVAILABLE_LABEL = "SPECTRAL DATA UNAVAILABLE";

    /** The profile's own channel order; a fourth value is not part of the schema. */
    private static final int MAX_CHANNELS = 3;

    private static final SpectrumCurve NONE = new SpectrumCurve(
            new double[0], new double[0][], 0, 0, 0, 0, 0, 0,
            new int[0][], new int[0][]);

    /** One measured wavelength per paired index; NaN when the row has no wavelength. */
    private final double[] wavelengths;
    /** [channel][paired index]; NaN is a gap. */
    private final double[][] values;
    private final int channels;
    private final int samples;
    private final double minNm, maxNm, minValue, maxValue;
    /** [channel][run] first paired index; only the first runCount entries are used. */
    private final int[][] runStarts;
    private final int[][] runLengths;

    private SpectrumCurve(double[] wavelengths, double[][] values, int channels, int samples,
                          double minNm, double maxNm, double minValue, double maxValue,
                          int[][] runStarts, int[][] runLengths) {
        this.wavelengths = wavelengths;
        this.values = values;
        this.channels = channels;
        this.samples = samples;
        this.minNm = minNm;
        this.maxNm = maxNm;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.runStarts = runStarts;
        this.runLengths = runLengths;
    }

    /** A stock whose catalogue entry has no usable spectral arrays. */
    public static SpectrumCurve unavailable() {
        return NONE;
    }

    /**
     * Read the shipped series. {@code series} is one row per wavelength, with
     * one entry per channel. A lack of at least two paired samples is
     * unavailability, not a one-point curve.
     */
    public static SpectrumCurve of(double[] wavelengths, double[][] series) {
        if (wavelengths == null || series == null) {
            return NONE;
        }
        final int paired = Math.min(wavelengths.length, series.length);
        if (paired < 2) {
            return NONE;
        }
        final int channels = channelCount(series, paired);
        if (channels == 0) {
            return NONE;
        }
        final Samples samples = collect(wavelengths, series, channels, paired);
        if (samples.count < 2) {
            return NONE;
        }

        final int[][] starts = new int[channels][samples.count];
        final int[][] lengths = new int[channels][samples.count];
        buildRuns(channels, paired, samples.values, starts, lengths);
        return new SpectrumCurve(samples.nm, samples.values, channels, samples.count,
                samples.loNm, samples.hiNm, samples.loValue, samples.hiValue, starts, lengths);
    }

    private static int channelCount(double[][] series, int paired) {
        int channels = 0;
        for (int i = 0; i < paired; ++i) {
            final double[] row = series[i];
            if (row != null) {
                channels = Math.max(channels, Math.min(MAX_CHANNELS, row.length));
            }
        }
        return channels;
    }

    private static Samples collect(double[] wavelengths, double[][] series,
                                   int channels, int paired) {
        final Samples out = new Samples(paired, channels);
        for (int i = 0; i < paired; ++i) {
            out.nm[i] = finite(wavelengths[i]) ? wavelengths[i] : Double.NaN;
            final double[] row = series[i];
            boolean sampled = false;
            for (int c = 0; c < channels; ++c) {
                final double value = row != null && c < row.length ? row[c] : Double.NaN;
                if (finite(out.nm[i]) && finite(value)) {
                    out.values[c][i] = value;
                    sampled = true;
                    if (value < out.loValue) {
                        out.loValue = value;
                    }
                    if (value > out.hiValue) {
                        out.hiValue = value;
                    }
                } else {
                    out.values[c][i] = Double.NaN;
                }
            }
            if (sampled) {
                out.count++;
                if (out.nm[i] < out.loNm) {
                    out.loNm = out.nm[i];
                }
                if (out.nm[i] > out.hiNm) {
                    out.hiNm = out.nm[i];
                }
            }
        }
        return out;
    }

    private static void buildRuns(int channels, int paired, double[][] values,
                                  int[][] starts, int[][] lengths) {
        for (int c = 0; c < channels; ++c) {
            int run = -1;
            for (int i = 0; i < paired; ++i) {
                if (!finite(values[c][i])) {
                    continue;
                }
                if (run < 0 || i != starts[c][run] + lengths[c][run]) {
                    run++;
                    starts[c][run] = i;
                    lengths[c][run] = 1;
                } else {
                    lengths[c][run]++;
                }
            }
        }
    }

    /** Mutable accumulator for {@link #collect}. */
    private static final class Samples {
        private final double[] nm;
        private final double[][] values;
        private int count;
        private double loNm = Double.POSITIVE_INFINITY;
        private double hiNm = Double.NEGATIVE_INFINITY;
        private double loValue = Double.POSITIVE_INFINITY;
        private double hiValue = Double.NEGATIVE_INFINITY;

        private Samples(int paired, int channels) {
            this.nm = new double[paired];
            this.values = new double[channels][paired];
        }
    }

    public boolean available() {
        return samples >= 2;
    }

    public int sampleCount() {
        return samples;
    }

    public int channelCount() {
        return channels;
    }

    public double minNm() {
        return minNm;
    }

    public double maxNm() {
        return maxNm;
    }

    /** The smallest finite value across every channel, or NaN when unavailable. */
    public double minValue() {
        return available() ? minValue : Double.NaN;
    }

    /** The largest finite value across every channel, or NaN when unavailable. */
    public double maxValue() {
        return available() ? maxValue : Double.NaN;
    }

    /** How many unbroken lines this channel draws. */
    public int runCount(int channel) {
        if (!validChannel(channel)) {
            return 0;
        }
        int count = 0;
        for (int run = 0; run < runStarts[channel].length; ++run) {
            if (runLengths[channel][run] <= 0) {
                break;
            }
            count++;
        }
        return count;
    }

    /** The paired index where a run begins, or -1. */
    public int runStart(int channel, int run) {
        if (!validChannel(channel) || run < 0 || run >= runCount(channel)) {
            return -1;
        }
        return runStarts[channel][run];
    }

    /** The number of consecutive real samples in a run, or 0. */
    public int runLength(int channel, int run) {
        if (!validChannel(channel) || run < 0 || run >= runCount(channel)) {
            return 0;
        }
        return runLengths[channel][run];
    }

    /** The measured wavelength at a point inside a run; NaN when absent. */
    public double nmAt(int channel, int run, int point) {
        final int index = indexAt(channel, run, point);
        return index < 0 ? Double.NaN : wavelengths[index];
    }

    /** The profile's own value at a point inside a run; NaN when absent. */
    public double rawAt(int channel, int run, int point) {
        final int index = indexAt(channel, run, point);
        return index < 0 ? Double.NaN : values[channel][index];
    }

    /**
     * The value mapped onto 0..1 across every channel's measured range. A flat
     * series draws on the middle line rather than dividing by zero.
     */
    public double normalizedAt(int channel, int run, int point) {
        final double value = rawAt(channel, run, point);
        if (!finite(value)) {
            return Double.NaN;
        }
        final double span = maxValue - minValue;
        if (!(span > 0)) {
            return 0.5;
        }
        return (value - minValue) / span;
    }

    /**
     * Where a measured wavelength lands on the plot's 0..1 horizontal axis.
     * The axis spans this curve's own measured range, so Scan inspect and the
     * plotted samples share one fraction. Out-of-range input clamps to an edge;
     * a missing wavelength or an unavailable curve has no fraction. A range of
     * zero width draws on the middle line, exactly as {@link #normalizedAt}
     * does vertically.
     */
    public double fractionAtNm(double nm) {
        if (!available() || !finite(nm)) {
            return Double.NaN;
        }
        final double span = maxNm - minNm;
        if (!(span > 0)) {
            return 0.5;
        }
        final double fraction = (nm - minNm) / span;
        return Math.max(0.0, Math.min(1.0, fraction));
    }

    /**
     * The plot axis fraction for a Scan inspect sample. The mark follows the
     * sampled wavelength, not the picture's horizontal position: a red scan
     * line and a blue one can sit at the same x while measuring different
     * points on the shipped series. A sample with no dominant wavelength (or
     * no sample at all) has no mark on this axis.
     */
    public double fractionForSample(ScanSample sample) {
        if (sample == null || !sample.hasWavelength()) {
            return Double.NaN;
        }
        return fractionAtNm(sample.wavelengthNm());
    }

    /** The profile's channel name, or an empty string for an unknown index. */
    public static String channelLabel(int channel) {
        switch (channel) {
            case 0: return "R";
            case 1: return "G";
            case 2: return "B";
            default: return "";
        }
    }

    private int indexAt(int channel, int run, int point) {
        if (!validChannel(channel) || run < 0 || run >= runCount(channel)) {
            return -1;
        }
        if (point < 0 || point >= runLengths[channel][run]) {
            return -1;
        }
        return runStarts[channel][run] + point;
    }

    private boolean validChannel(int channel) {
        return channel >= 0 && channel < channels;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}

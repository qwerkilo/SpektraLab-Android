package app.spektralab;

/**
 * The spectral samples a shipped stock carries, if any.
 *
 * <p>The catalogue is allowed to omit the arrays, and a stock may carry real
 * gaps. This module only counts and ranges the samples that exist; it never
 * fills a gap, interpolates a curve, or turns a bin index into a measured
 * wavelength. The Readout strip is its only consumer in the Console shell.
 *
 * <p>It is deliberately Android-free so the JVM check can compile it without
 * the SDK.
 */
public final class SpectrumSamples {
    /** The Spectral rail's reference range. It is a scale, not a measurement. */
    public static final int RAIL_MIN_NM = 380;
    public static final int RAIL_MID_NM = 550;
    public static final int RAIL_MAX_NM = 780;

    /** The only truthful value when a stock ships no samples. */
    public static final String UNAVAILABLE_LABEL = "SPECTRUM UNAVAILABLE";

    private static final String DASH = "\u2014";
    private static final SpectrumSamples NONE = new SpectrumSamples(0, 0, 0);

    private final int samples;
    private final int minNm;
    private final int maxNm;

    private SpectrumSamples(int samples, int minNm, int maxNm) {
        this.samples = samples;
        this.minNm = minNm;
        this.maxNm = maxNm;
    }

    /** A stock whose catalogue entry has no usable spectral samples. */
    public static SpectrumSamples unavailable() {
        return NONE;
    }

    /**
     * Read the shipped series. {@code series} is one row per wavelength, with
     * one entry per channel; nulls, NaNs, and infinities are gaps. A lack of at
     * least two paired samples is unavailability, not a one-point curve.
     *
     * <p>The pairing rule lives in {@link SpectrumCurve}; this cell and the
     * film panel must agree on what counts as a sample.
     */
    public static SpectrumSamples of(double[] wavelengths, double[][] series) {
        final SpectrumCurve curve = SpectrumCurve.of(wavelengths, series);
        if (!curve.available()) return NONE;
        return new SpectrumSamples(curve.sampleCount(),
                (int) Math.round(curve.minNm()), (int) Math.round(curve.maxNm()));
    }

    public boolean available() {
        return samples >= 2;
    }

    public int sampleCount() {
        return samples;
    }

    public int minNm() {
        return minNm;
    }

    public int maxNm() {
        return maxNm;
    }

    /** The cell's headline: the sample count, or the em dash for no data. */
    public String valueText() {
        return available() ? String.valueOf(samples) : DASH;
    }

    /** The cell's unit. Samples are not a wavelength measurement. */
    public String unitText() {
        return available() ? "SMP" : "";
    }

    /** The cell's quiet line: the shipped range, or the explicit state. */
    public String detailText() {
        if (!available()) return UNAVAILABLE_LABEL;
        return minNm + "\u2013" + maxNm + " nm";
    }
}

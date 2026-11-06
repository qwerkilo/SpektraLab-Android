package app.spektralab.core;

/**
 * One read-only sample from a preview row, for Scan inspect.
 *
 * <p>The seam is pure: an ARGB row, its width, and a horizontal fraction in
 * 0..1. The fraction clamps at both edges, so a drag that leaves the picture
 * still reports the edge pixel. The result carries the real 8-bit RGB of that
 * pixel and an <em>estimated</em> dominant wavelength.
 *
 * <p>The wavelength is an estimate from the pixel's hue, not a spectrometer
 * reading. It stays inside 380–780 nm and moves monotonically for a row that
 * sweeps the visible spectrum. A pixel with no meaningful dominant wavelength
 * — grayscale, near-gray, or a purple on the line of purples — reports the
 * explicit {@link #NO_WAVELENGTH} instead of a fabricated number.
 *
 * <p>This class never touches a Look, a view, the engine, or a render. It is
 * deliberately Android-free so the JVM check can compile it without the SDK.
 */
public final class ScanSample {
    /** The estimated wavelength's lower bound. */
    public static final int MIN_WAVELENGTH_NM = 380;
    /** The estimated wavelength's upper bound. */
    public static final int MAX_WAVELENGTH_NM = 780;

    /** The explicit "no dominant wavelength" value. Never a fake number. */
    public static final double NO_WAVELENGTH = Double.NaN;

    /** What the readout shows when {@link #hasWavelength()} is false. */
    public static final String UNAVAILABLE_LABEL = "\u2014";
    public static final String WAVELENGTH_UNIT = "nm";

    /** Scan inspect's starting position, the picture's centre. */
    public static final float DEFAULT_FRACTION = 0.5f;

    /** Below this saturation there is no hue to measure. */
    private static final double MIN_SATURATION = 0.10;
    /** Below this 8-bit chroma the hue is quantisation noise. */
    private static final int MIN_CHROMA = 8;

    /**
     * Hue-to-wavelength anchors for the visible spectrum, in degrees and
     * nanometres. Hue increases from red through yellow, green, cyan, and blue
     * to violet; the estimate therefore decreases across a spectrum row. Hues
     * past 280° are the line of purples and have no single spectral wavelength.
     */
    private static final double[] HUE_ANCHORS =
            {0, 30, 60, 120, 180, 240, 260, 280};
    private static final double[] NM_ANCHORS =
            {700, 610, 580, 540, 490, 460, 430, 380};

    private static final ScanSample NONE =
            new ScanSample(false, 0, 0, 0, NO_WAVELENGTH);

    private final boolean available;
    private final int red, green, blue;
    private final double wavelengthNm;

    private ScanSample(boolean available, int red, int green, int blue, double wavelengthNm) {
        this.available = available;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.wavelengthNm = wavelengthNm;
    }

    /** The only truthful result when there is no row to sample. */
    public static ScanSample unavailable() {
        return NONE;
    }

    /**
     * Sample {@code row} at {@code fraction}. A null row, a non-positive
     * width, or a row shorter than its width is unavailable; the fraction is
     * clamped to 0..1 and NaN falls back to the centre.
     */
    public static ScanSample at(int[] row, int width, float fraction) {
        if (row == null || width <= 0 || row.length < width) {
            return NONE;
        }
        float f = fraction;
        if (Float.isNaN(f)) {
            f = DEFAULT_FRACTION;
        }
        if (f < 0f) {
            f = 0f;
        }
        if (f > 1f) {
            f = 1f;
        }
        final int index = Math.round(f * (width - 1));
        final int pixel = row[Math.max(0, Math.min(width - 1, index))];
        final int r = (pixel >> 16) & 0xFF;
        final int g = (pixel >> 8) & 0xFF;
        final int b = pixel & 0xFF;
        return new ScanSample(true, r, g, b, estimateWavelength(r, g, b));
    }

    /** True when a pixel was actually read. Grayscale is still available. */
    public boolean available() {
        return available;
    }

    public int red() {
        return red;
    }

    public int green() {
        return green;
    }

    public int blue() {
        return blue;
    }

    /** True when this colour has a meaningful dominant wavelength estimate. */
    public boolean hasWavelength() {
        return !Double.isNaN(wavelengthNm);
    }

    /**
     * The estimated dominant wavelength in nanometres, or
     * {@link #NO_WAVELENGTH} when the colour has none. Always within
     * {@link #MIN_WAVELENGTH_NM}..{@link #MAX_WAVELENGTH_NM} when present.
     */
    public double wavelengthNm() {
        return wavelengthNm;
    }

    /** The readout's whole nanometre, or the explicit dash. */
    public String wavelengthText() {
        return hasWavelength()
                ? String.valueOf(Math.round(wavelengthNm))
                : UNAVAILABLE_LABEL;
    }

    /** "nm" only when there is a number to carry it. */
    public String wavelengthUnitText() {
        return hasWavelength() ? WAVELENGTH_UNIT : "";
    }

    /** The sampled pixel as one line of real channel values. */
    public String rgbText() {
        if (!available) {
            return UNAVAILABLE_LABEL;
        }
        return "R " + red + "  G " + green + "  B " + blue;
    }

    /** The readout's RGB line, with an explicit dash per channel when unread. */
    public String rgbReadoutText() {
        return available ? rgbText() : "R \u2014  G \u2014  B \u2014";
    }

    /** The readout's lambda line: the estimate and unit, or the honest dash. */
    public String lambdaText() {
        return "\u03bb " + (hasWavelength()
                ? wavelengthText() + " " + WAVELENGTH_UNIT
                : UNAVAILABLE_LABEL);
    }

    /** Readout text for accessibility: numbers, or the honest unavailable state. */
    public String description() {
        if (!available) {
            return "扫描采样不可用";
        }
        if (!hasWavelength()) {
            return "扫描采样 " + rgbText() + "，没有主波长";
        }
        return "扫描采样 " + rgbText() + "，主波长约 " + wavelengthText() + " 纳米";
    }

    private static double estimateWavelength(int r, int g, int b) {
        final int max = Math.max(r, Math.max(g, b));
        final int min = Math.min(r, Math.min(g, b));
        final int chroma = max - min;
        if (max == 0 || chroma < MIN_CHROMA) {
            return NO_WAVELENGTH;
        }
        final double saturation = chroma / (double) max;
        if (saturation < MIN_SATURATION) {
            return NO_WAVELENGTH;
        }

        double hue;
        if (max == r) {
            hue = 60.0 * (((g - b) / (double) chroma) % 6.0);
        } else if (max == g) {
            hue = 60.0 * (((b - r) / (double) chroma) + 2.0);
        } else {
            hue = 60.0 * (((r - g) / (double) chroma) + 4.0);
        }
        if (hue < 0.0) {
            hue += 360.0;
        }
        if (hue > HUE_ANCHORS[HUE_ANCHORS.length - 1]) {
            return NO_WAVELENGTH;
        }

        double nm = NM_ANCHORS[NM_ANCHORS.length - 1];
        for (int i = 1; i < HUE_ANCHORS.length; ++i) {
            if (hue <= HUE_ANCHORS[i]) {
                final double span = HUE_ANCHORS[i] - HUE_ANCHORS[i - 1];
                final double t = span <= 0 ? 0 : (hue - HUE_ANCHORS[i - 1]) / span;
                nm = NM_ANCHORS[i - 1] + (NM_ANCHORS[i] - NM_ANCHORS[i - 1]) * t;
                break;
            }
        }
        return Math.max(MIN_WAVELENGTH_NM, Math.min(MAX_WAVELENGTH_NM, nm));
    }
}

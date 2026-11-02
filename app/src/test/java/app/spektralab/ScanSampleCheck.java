package app.spektralab;

/**
 * Pure JVM contract for Scan inspect's sample: one ARGB row, one horizontal
 * fraction, real RGB, and an estimated dominant wavelength.
 *
 * <p>A uniform row samples the same colour everywhere; both edges clamp; a
 * single coloured pixel is found at its own index; a missing or short row is
 * explicitly unavailable. Grayscale and the line of purples have no dominant
 * wavelength, so the check requires the explicit unavailable readout rather
 * than a number. A row that sweeps the spectrum must produce a monotonic
 * estimate inside 380–780 nm.
 *
 * <p>No Android, no JUnit: run it with the same javac/java as the app.
 */
public final class ScanSampleCheck {
    private static int checks = 0;

    public static void main(String[] args) {
        unavailableStates();
        uniformRow();
        edgesClamp();
        midpoint();
        singleColouredPixel();
        wavelengthAnchors();
        grayscaleHasNoWavelength();
        lineOfPurplesHasNoWavelength();
        spectrumRowIsMonotonic();

        System.out.println("ScanSampleCheck: " + checks + " checks passed");
    }

    private static void unavailableStates() {
        final ScanSample none = ScanSample.at(null, 4, 0.5f);
        require("a null row is unavailable", !none.available());
        require("a null row has no wavelength", !none.hasWavelength());
        require("a null row carries no wavelength number",
                Double.isNaN(none.wavelengthNm()));
        require("a null row shows the explicit dash",
                "\u2014".equals(none.wavelengthText()));
        require("a null row shows the lambda dash line",
                "\u03bb \u2014".equals(none.lambdaText()));
        require("a null row shows the empty RGB line",
                "R \u2014  G \u2014  B \u2014".equals(none.rgbReadoutText()));

        final ScanSample shortRow = ScanSample.at(new int[]{0xFFFF0000}, 4, 0.5f);
        require("a row shorter than its width is unavailable", !shortRow.available());

        final ScanSample zeroWidth = ScanSample.at(new int[]{0xFFFF0000}, 0, 0.5f);
        require("a zero width is unavailable", !zeroWidth.available());

        require("the shared unavailable sample is unavailable",
                !ScanSample.unavailable().available());
        require("the shared unavailable sample has no wavelength",
                !ScanSample.unavailable().hasWavelength());
    }

    private static void uniformRow() {
        final int colour = 0xFF336699;
        final int[] row = {colour, colour, colour, colour};
        for (float fraction : new float[]{-2f, 0f, 0.25f, 0.5f, 0.75f, 1f, 3f}) {
            final ScanSample sample = ScanSample.at(row, row.length, fraction);
            require("a uniform row samples red at " + fraction, sample.red() == 0x33);
            require("a uniform row samples green at " + fraction, sample.green() == 0x66);
            require("a uniform row samples blue at " + fraction, sample.blue() == 0x99);
        }
        final ScanSample sample = ScanSample.at(row, row.length, 0.5f);
        require("the RGB text carries the real channels",
                "R 51  G 102  B 153".equals(sample.rgbText()));
        require("the readout RGB text is the measured channels",
                "R 51  G 102  B 153".equals(sample.rgbReadoutText()));
        require("the lambda line carries the estimate and unit",
                sample.lambdaText().matches("\u03bb \\d{3} nm"));
        require("the alpha byte is ignored",
                ScanSample.at(new int[]{0x00FF0000}, 1, 0.5f).red() == 0xFF);
    }

    private static void edgesClamp() {
        final int[] row = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        require("a fraction below zero clamps to the first pixel",
                ScanSample.at(row, row.length, -0.5f).red() == 0xFF);
        require("a fraction above one clamps to the last pixel",
                ScanSample.at(row, row.length, 2f).blue() == 0xFF);
        require("the first pixel is the left edge",
                ScanSample.at(row, row.length, 0f).green() == 0);
        require("the last pixel is the right edge",
                ScanSample.at(row, row.length, 1f).red() == 0);
    }

    private static void midpoint() {
        final int[] row = {0xFF101010, 0xFF202020, 0xFFCC3300,
                0xFF404040, 0xFF505050};
        final ScanSample sample = ScanSample.at(row, row.length, 0.5f);
        require("the midpoint samples the centre pixel", sample.red() == 0xCC);
        require("the midpoint keeps the centre green", sample.green() == 0x33);
        require("the midpoint keeps the centre blue", sample.blue() == 0x00);
    }

    private static void singleColouredPixel() {
        final int background = 0xFF202020;
        final int[] row = {background, background, 0xFFCC3300, background, background};
        final ScanSample centred = ScanSample.at(row, row.length, 0.5f);
        require("a single coloured pixel is sampled at its own index",
                centred.red() == 0xCC && centred.green() == 0x33 && centred.blue() == 0);
        final ScanSample neighbour = ScanSample.at(row, row.length, 0.25f);
        require("the pixel beside it is the background",
                neighbour.red() == 0x20 && neighbour.green() == 0x20 && neighbour.blue() == 0x20);
        require("the coloured pixel still yields a wavelength", centred.hasWavelength());
        require("the background is grayscale and yields none", !neighbour.hasWavelength());
    }

    private static void wavelengthAnchors() {
        assertAnchor("red", 0xFFFF0000, 620, 780);
        assertAnchor("orange", 0xFFFF8000, 590, 620);
        assertAnchor("yellow", 0xFFFFFF00, 570, 590);
        assertAnchor("green", 0xFF00FF00, 495, 570);
        assertAnchor("cyan", 0xFF00FFFF, 476, 495);
        assertAnchor("blue", 0xFF0000FF, 450, 476);
        assertAnchor("violet", 0xFF8000FF, 380, 450);
    }

    private static void assertAnchor(String name, int argb, int minNm, int maxNm) {
        final ScanSample sample = ScanSample.at(new int[]{argb}, 1, 0.5f);
        require(name + " has a dominant wavelength", sample.hasWavelength());
        final double nm = sample.wavelengthNm();
        require(name + " stays in 380-780 nm",
                nm >= ScanSample.MIN_WAVELENGTH_NM && nm <= ScanSample.MAX_WAVELENGTH_NM);
        require(name + " lands in its physical band",
                nm >= minNm && nm <= maxNm);
        require(name + " has no unit when unavailable is not the case",
                "nm".equals(sample.wavelengthUnitText()));
        require(name + " prints a whole nanometre",
                sample.wavelengthText().matches("\\d{3}"));
    }

    private static void grayscaleHasNoWavelength() {
        final int[][] grays = {
                {0, 0, 0},
                {128, 128, 128},
                {255, 255, 255},
                {17, 18, 17},
                {100, 104, 100},
        };
        for (int[] rgb : grays) {
            final int argb = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
            final ScanSample sample = ScanSample.at(new int[]{argb}, 1, 0.5f);
            require("grayscale " + rgb[0] + "," + rgb[1] + "," + rgb[2] + " is sampled",
                    sample.available());
            require("grayscale has no dominant wavelength", !sample.hasWavelength());
            require("grayscale never invents a number",
                    Double.isNaN(sample.wavelengthNm()));
            require("grayscale shows the explicit dash",
                    "\u2014".equals(sample.wavelengthText()));
            require("grayscale never prints a lambda number",
                    "\u03bb \u2014".equals(sample.lambdaText()));
            require("grayscale has no wavelength unit",
                    "".equals(sample.wavelengthUnitText()));
        }
    }

    private static void lineOfPurplesHasNoWavelength() {
        final int[] purples = {
                0xFFFF00FF,     // magenta, hue 300
                0xFFC000FF,     // purple, hue 285
                0xFFFF0080,     // rose, hue 330
        };
        for (int argb : purples) {
            final ScanSample sample = ScanSample.at(new int[]{argb}, 1, 0.5f);
            require("line-of-purples " + Integer.toHexString(argb) + " is sampled",
                    sample.available());
            require("line-of-purples " + Integer.toHexString(argb)
                    + " has no spectral wavelength", !sample.hasWavelength());
            require("line-of-purples " + Integer.toHexString(argb)
                    + " never invents a number", Double.isNaN(sample.wavelengthNm()));
            require("line-of-purples " + Integer.toHexString(argb)
                    + " shows lambda as a dash", "\u03bb \u2014".equals(sample.lambdaText()));
            require("line-of-purples " + Integer.toHexString(argb)
                    + " has no unit", "".equals(sample.wavelengthUnitText()));
            require("line-of-purples " + Integer.toHexString(argb)
                    + " says there is no dominant wavelength",
                    sample.description().contains("\u6ca1\u6709\u4e3b\u6ce2\u957f"));
        }
    }

    private static void spectrumRowIsMonotonic() {
        final int width = 281;             // hue 0..280, one degree per pixel
        final int[] row = new int[width];
        for (int x = 0; x < width; ++x) {
            row[x] = hsv(x, 0.90, 0.90);
        }

        double previous = Double.NaN;
        double lowest = Double.MAX_VALUE, highest = -Double.MAX_VALUE;
        for (int x = 0; x < width; ++x) {
            final ScanSample sample = ScanSample.at(row, width, x / (float) (width - 1));
            require("every spectrum step has a wavelength at " + x + " nm hue",
                    sample.hasWavelength());
            final double nm = sample.wavelengthNm();
            require("the spectrum estimate stays in 380-780 nm at x=" + x,
                    nm >= 380 && nm <= 780);
            lowest = Math.min(lowest, nm);
            highest = Math.max(highest, nm);
            if (!Double.isNaN(previous)) {
                // 8-bit sRGB quantisation can wobble a fraction of a nanometre
                // between adjacent one-degree steps; the trend may not reverse.
                require("the estimate never increases across the spectrum at x=" + x
                                + " (" + previous + " -> " + nm + ")",
                        nm <= previous + 1.0);
            }
            previous = nm;
        }
        require("the spectrum row reaches the violet end", lowest <= 400);
        require("the spectrum row reaches the red end", highest >= 650);
    }

    /** Independent HSV reference, so the monotonic row is not built by the seam. */
    private static int hsv(double hue, double saturation, double value) {
        final double chroma = value * saturation;
        final double hp = hue / 60.0;
        final double x = chroma * (1 - Math.abs(hp % 2 - 1));
        double r = 0, g = 0, b = 0;
        if (hp < 1) { r = chroma; g = x; }
        else if (hp < 2) { r = x; g = chroma; }
        else if (hp < 3) { g = chroma; b = x; }
        else if (hp < 4) { g = x; b = chroma; }
        else if (hp < 5) { r = x; b = chroma; }
        else { r = chroma; b = x; }
        final double m = value - chroma;
        final int ri = (int) Math.round((r + m) * 255);
        final int gi = (int) Math.round((g + m) * 255);
        final int bi = (int) Math.round((b + m) * 255);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

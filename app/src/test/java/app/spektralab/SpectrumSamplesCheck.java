package app.spektralab;

/**
 * Pure JVM contract for the Readout strip's spectrum cell: the cell reports
 * shipped samples when the catalogue carries them, and says so explicitly when
 * it does not. No picture, no curve, no invented wavelength.
 */
public final class SpectrumSamplesCheck {
    private static int checks = 0;

    public static void main(String[] args) {
        require("the rail references 380 nm", SpectrumSamples.RAIL_MIN_NM == 380);
        require("the rail references 550 nm", SpectrumSamples.RAIL_MID_NM == 550);
        require("the rail references 780 nm", SpectrumSamples.RAIL_MAX_NM == 780);

        unavailableStates();
        shippedSamples();
        gapsAreNotSamples();
        mismatchedArrays();
        oneSampleIsNotASpectrum();
        rangeIsMeasuredFromSamples();

        System.out.println("SpectrumSamplesCheck: " + checks + " checks passed");
    }

    private static void unavailableStates() {
        final SpectrumSamples none = SpectrumSamples.unavailable();
        require("no catalogue arrays are unavailable", !none.available());
        require("unavailable value is an em dash", "\u2014".equals(none.valueText()));
        require("unavailable unit is empty", "".equals(none.unitText()));
        require("unavailable detail says so",
                "SPECTRUM UNAVAILABLE".equals(none.detailText()));
        require("null wavelengths are unavailable",
                !SpectrumSamples.of(null, new double[][]{{1.0}}).available());
        require("null series is unavailable",
                !SpectrumSamples.of(new double[]{380}, null).available());
        require("empty arrays are unavailable",
                !SpectrumSamples.of(new double[0], new double[0][]).available());
    }

    private static void shippedSamples() {
        final double[] wavelengths = new double[81];
        final double[][] series = new double[81][3];
        for (int i = 0; i < 81; ++i) {
            wavelengths[i] = 380 + i * 5;
            series[i][0] = 1.0;
            series[i][1] = 0.5;
            series[i][2] = 0.25;
        }
        final SpectrumSamples s = SpectrumSamples.of(wavelengths, series);
        require("81 shipped samples are available", s.available());
        require("sample count is 81", s.sampleCount() == 81);
        require("value is the sample count", "81".equals(s.valueText()));
        require("unit is SMP", "SMP".equals(s.unitText()));
        require("detail carries the shipped range",
                "380\u2013780 nm".equals(s.detailText()));
    }

    private static void gapsAreNotSamples() {
        final double[] wavelengths = {400, 450, 500, 550};
        final double[][] series = {
                {1.0, 0.5},
                {Double.NaN, Double.NaN},
                null,
                {3.0, Double.POSITIVE_INFINITY},
        };
        final SpectrumSamples s = SpectrumSamples.of(wavelengths, series);
        require("real gaps leave two samples", s.sampleCount() == 2);
        require("two valid samples are still available", s.available());
        require("gaps are not counted as zero", "2".equals(s.valueText()));
        require("detail spans only valid samples",
                "400\u2013550 nm".equals(s.detailText()));
    }

    private static void mismatchedArrays() {
        final double[] wavelengths = {400, 450, 500, 550};
        final double[][] series = {{1.0}, {2.0}};
        final SpectrumSamples s = SpectrumSamples.of(wavelengths, series);
        require("only paired indices count", s.sampleCount() == 2);
        require("paired range ends at the second pair",
                "400\u2013450 nm".equals(s.detailText()));
    }

    private static void oneSampleIsNotASpectrum() {
        final SpectrumSamples s = SpectrumSamples.of(new double[]{500, 550},
                new double[][]{{Double.NaN}, {2.0}});
        require("a single valid sample is unavailable", !s.available());
        require("a single valid sample shows the unavailable value",
                "\u2014".equals(s.valueText()));
    }

    private static void rangeIsMeasuredFromSamples() {
        final SpectrumSamples s = SpectrumSamples.of(
                new double[]{550, 380, 780, Double.NaN, 700},
                new double[][]{{1.0}, {1.0}, {1.0}, {1.0}, {1.0}});
        require("range is the minimum wavelength", s.minNm() == 380);
        require("range is the maximum wavelength", s.maxNm() == 780);
        require("unsorted samples still report one range",
                "380\u2013780 nm".equals(s.detailText()));
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

package app.spektralab.model;

import app.spektralab.core.SpectrumCurve;
import app.spektralab.core.SpectrumSamples;


/** One catalogue film record, in the form the film panel reads. */
public final class FilmStock {
    private final SpectrumSamples samples;
    private final SpectrumCurve curve;
    /** "LOG SENSITIVITY", "DENSITY", or "" when nothing was shipped. */
    private final String seriesLabel;
    private final java.util.Map<String, String> info;
    /** The paper this film declares it prints on; null when it declares none. */
    private final String targetPrint;

    public FilmStock(SpectrumSamples samples, SpectrumCurve curve, String seriesLabel,
                     java.util.Map<String, String> info, String targetPrint) {
        this.samples = samples;
        this.curve = curve;
        this.seriesLabel = seriesLabel;
        this.info = info;
        this.targetPrint = targetPrint;
    }

    public SpectrumSamples samples() {
        return samples;
    }

    public SpectrumCurve curve() {
        return curve;
    }

    public String seriesLabel() {
        return seriesLabel;
    }

    public java.util.Map<String, String> info() {
        return info;
    }

    public String targetPrint() {
        return targetPrint;
    }
}

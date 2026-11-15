package app.spektralab.core;

/** The caps a RAW session opens with: native size disables both. */
public final class RawOpenPlan {

    public final int maxPixels;
    public final int longEdge;

    private RawOpenPlan(int maxPixels, int longEdge) {
        this.maxPixels = maxPixels;
        this.longEdge = longEdge;
    }

    /** @param nativeSize true when the user asked for the file's own size */
    public static RawOpenPlan of(boolean nativeSize, int maxPixels, int longEdge) {
        return nativeSize ? new RawOpenPlan(0, 0) : new RawOpenPlan(maxPixels, longEdge);
    }
}

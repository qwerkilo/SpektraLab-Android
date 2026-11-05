package app.spektralab;

/**
 * One UI-thread snapshot of everything a render needs: the Look, the exact
 * engine JSON it produces, the view-adjust values, and the stock/long-edge the
 * caller asked for. Preview, export, and the RAW import hand the same object to
 * their worker instead of re-reading controls or re-building the JSON.
 */
public final class RenderSnapshot {
    public final Look look;
    public final String stock;
    public final int longEdge;
    public final double[] viewAdjust;
    public final String params;

    private RenderSnapshot(Look look, String stock, int longEdge,
                           double[] viewAdjust, String params) {
        this.look = look;
        this.stock = stock;
        this.longEdge = longEdge;
        this.viewAdjust = viewAdjust;
        this.params = params;
    }

    /**
     * Take the snapshot. The Look is detached before the JSON is built, so a
     * slider or Frame switch after this call cannot move the render.
     */
    public static RenderSnapshot of(Look look, String stock, int longEdge,
                                    LookRestatement.Context context) {
        if (look == null) throw new IllegalArgumentException("look is null");
        if (context == null) throw new IllegalArgumentException("context is null");
        final Look copy = look.copy();
        copy.filmStock = stock;
        return new RenderSnapshot(copy, stock, longEdge,
                copy.view.toNative(), LookRestatement.json(copy, context));
    }
}

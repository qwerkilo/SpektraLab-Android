package app.spektralab.core;

/**
 * The render generation of the current canvas.
 *
 * <p>Any render that comes back after the canvas was replaced belongs to an
 * ended epoch: it must neither paint nor leave the busy plate behind.
 */
public final class RenderEpoch {
    private int value;

    /** Starts a new generation and returns it. */
    public int bump() {
        return ++value;
    }

    /** The generation a render should capture before it starts. */
    public int current() {
        return value;
    }

    /** Whether a captured generation still owns the canvas. */
    public boolean isCurrent(int captured) {
        return captured == value;
    }
}

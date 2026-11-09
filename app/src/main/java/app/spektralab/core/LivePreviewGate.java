package app.spektralab.core;

/**
 * Coalescing and invalidation for live preview frames.
 *
 * <p>A slider emits far more changes than the GPU can draw. The gate keeps one
 * pending request, hands each frame a generation token, and lets a full render
 * or a frame change invalidate every live frame already in flight. It is
 * deliberately free of Android types so the drop rules can be checked on a
 * plain JVM.
 */
public final class LivePreviewGate {

    private int generation;
    private int fullGeneration;
    private boolean pending;

    /** Ask for a frame; any older request is superseded. */
    public synchronized int request() {
        pending = true;
        return ++generation;
    }

    /** Take the pending request, or return false when none is waiting. */
    public synchronized boolean consume() {
        if (!pending) {
            return false;
        }
        pending = false;
        return true;
    }

    /** Drop the pending request and invalidate every frame in flight. */
    public synchronized int cancel() {
        pending = false;
        return ++generation;
    }

    /** A full-size render owns the canvas; live frames must not land after it. */
    public synchronized int fullRenderStarted() {
        pending = false;
        ++generation;
        return ++fullGeneration;
    }

    /** Whether a finished frame still belongs to the current request and render. */
    public synchronized boolean accepts(int frameGeneration, int frameFullGeneration) {
        return frameGeneration == generation && frameFullGeneration == fullGeneration;
    }

    public synchronized int generation() {
        return generation;
    }

    public synchronized int fullGeneration() {
        return fullGeneration;
    }
}

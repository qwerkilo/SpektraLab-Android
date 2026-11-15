package app.spektralab.core;

/**
 * One RAW import at a time.
 *
 * <p>Two imports overlap in practice when the confirmation is tapped twice:
 * each closes the session the other just opened, and the preview worker can
 * end up inside a native call whose session was freed. The engine fix makes
 * that a Java exception rather than SIGSEGV; refusing the second start keeps
 * the user's first choice intact instead of restarting the decode.
 */
public final class ImportGate {

    private boolean running;

    /** True when this call took the slot; false when an import is already running. */
    public synchronized boolean tryStart() {
        if (running) {
            return false;
        }
        running = true;
        return true;
    }

    /** Release the slot; safe to call when nothing was running. */
    public synchronized void finish() {
        running = false;
    }

    public synchronized boolean isRunning() {
        return running;
    }
}

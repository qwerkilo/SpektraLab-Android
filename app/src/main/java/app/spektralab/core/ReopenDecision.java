package app.spektralab.core;

/**
 * Decides what a Frame does when it is reopened from the strip.
 *
 * <p>A Frame left on 原图 must come back on 原图 even when it still carries a
 * Look; a legacy Frame that never had the flag opens on its print.
 */
public final class ReopenDecision {
    private ReopenDecision() {
    }

    /**
     * Whether reopening should develop the Frame.
     *
     * @param leftOnOriginal the Frame's remembered state
     * @param hasFilm        whether its Look still names a film stock
     * @return true when the reopen should render the saved Look
     */
    public static boolean rendersOnReopen(boolean leftOnOriginal, boolean hasFilm) {
        return !leftOnOriginal && hasFilm;
    }
}

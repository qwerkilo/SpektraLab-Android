package app.spektralab;

/**
 * The per-Frame edit session: one live Look plus its undo history.
 *
 * <p>The Look is the only live copy of the Frame's choices. Callers read it
 * through {@link #look()}; user edits go through {@link #edit} so the history
 * records the settled state. A direct mutation followed by {@link #record()}
 * is allowed for a slider drag that must repaint before it commits.
 */
public final class FrameLookSession {
    private Look look;
    private final LookHistory history = new LookHistory();

    public FrameLookSession(Look initial) {
        look = initial == null ? new Look() : initial.copy();
        history.reset(look);
    }

    /** The current live Look. Treat as read-only; mutate through edit/record. */
    public Look look() {
        return look;
    }

    /** Apply one settled edit and record the resulting state. */
    public void edit(java.util.function.Consumer<Look> change) {
        change.accept(look);
        history.record(look);
    }

    /** Record a state already mutated by a live drag. Identical states are free. */
    public void record() {
        history.record(look);
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public int size() {
        return history.size();
    }

    /** Restore the previous state, or null when there is nothing to undo. */
    public Look undo() {
        final Look previous = history.undo();
        if (previous == null) return null;
        look = previous;
        return look;
    }

    /** Start this Frame over from a supplied Look and clear its history. */
    public void reset(Look next) {
        look = next == null ? new Look() : next.copy();
        history.reset(look);
    }
}

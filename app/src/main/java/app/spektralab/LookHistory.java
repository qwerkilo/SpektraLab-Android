package app.spektralab;

import java.util.ArrayDeque;

/**
 * Undo history for one Frame's Look.
 *
 * <p>Snapshots are the Look's own JSON, so a restored snapshot goes through the
 * same defaults and wire names as a saved Look. A state equal to the newest
 * snapshot is not pushed twice: a re-render after a theme rebuild or a Frame
 * restore must not create an undo step for a state the user never changed.
 */
final class LookHistory {
    static final int LIMIT = 100;

    private final ArrayDeque<String> snapshots = new ArrayDeque<>();
    private String newest = null;

    /** Start a Frame: its current Look is the first, non-undoable state. */
    void reset(Look look) {
        snapshots.clear();
        newest = look.toJson().toString();
        snapshots.push(newest);
    }

    void clear() {
        snapshots.clear();
        newest = null;
    }

    /** Record a settled user edit. Identical states are free. */
    void record(Look look) {
        final String now = look.toJson().toString();
        if (now.equals(newest)) return;
        snapshots.push(now);
        newest = now;
        while (snapshots.size() > LIMIT) snapshots.removeLast();
    }

    boolean canUndo() {
        return snapshots.size() > 1;
    }

    /**
     * Drop the current state and return the one before it, or null when there
     * is nothing to undo.
     */
    Look undo() {
        if (!canUndo()) return null;
        snapshots.pop();
        newest = snapshots.peek();
        try {
            return Look.fromJson(new org.json.JSONObject(newest));
        } catch (org.json.JSONException e) {
            return null;
        }
    }

    /** The number of states, for the contract check and the action's label. */
    int size() {
        return snapshots.size();
    }
}

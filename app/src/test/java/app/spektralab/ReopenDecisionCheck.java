package app.spektralab;

import app.spektralab.core.ReopenDecision;

/** Checks the reopen policy for a Frame left on 原图. */
public final class ReopenDecisionCheck {
    private ReopenDecisionCheck() {
    }

    public static void main(String[] args) {
        check(ReopenDecision.rendersOnReopen(false, true), "developed frame re-renders");
        check(!ReopenDecision.rendersOnReopen(true, true), "original frame stays original");
        check(!ReopenDecision.rendersOnReopen(false, false), "no film means no render");
        check(!ReopenDecision.rendersOnReopen(true, false), "original without film stays original");
        System.out.println("ReopenDecisionCheck: 4 checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

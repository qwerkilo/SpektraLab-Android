package app.spektralab;

import app.spektralab.core.RenderEpoch;

/** Checks that a render from an ended canvas cannot claim the new one. */
public final class RenderEpochCheck {
    private RenderEpochCheck() {
    }

    public static void main(String[] args) {
        RenderEpoch epoch = new RenderEpoch();
        int first = epoch.current();
        int second = epoch.bump();
        check(!epoch.isCurrent(first), "previous generation is stale");
        check(epoch.isCurrent(second), "bumped generation owns the canvas");
        int third = epoch.bump();
        check(!epoch.isCurrent(second), "second generation is stale after a bump");
        check(epoch.isCurrent(third), "third generation owns the canvas");
        System.out.println("RenderEpochCheck: 4 checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

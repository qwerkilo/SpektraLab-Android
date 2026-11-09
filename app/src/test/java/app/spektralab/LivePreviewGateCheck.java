package app.spektralab;

import app.spektralab.core.LivePreviewGate;

/** Plain-JVM contract for coalescing and invalidating live preview frames. */
public final class LivePreviewGateCheck {

    private static int checks;

    private LivePreviewGateCheck() {
    }

    public static void main(String[] args) {
        final LivePreviewGate gate = new LivePreviewGate();

        ok("nothing is pending at start", !gate.consume());

        final int first = gate.request();
        ok("a request opens the gate", gate.consume());
        ok("a consumed request is not handed out twice", !gate.consume());

        final int second = gate.request();
        ok("the second request has a newer token", second > first);
        ok("the first frame is stale after a newer request",
                !gate.accepts(first, gate.fullGeneration()));
        ok("the current frame is accepted",
                gate.accepts(second, gate.fullGeneration()));

        final int cancelled = gate.cancel();
        ok("cancel advances the token", cancelled > second);
        ok("a frame from before cancel is dropped",
                !gate.accepts(second, gate.fullGeneration()));

        final int full = gate.request();
        final int fullGeneration = gate.fullRenderStarted();
        ok("a full render advances its own generation", fullGeneration > 0);
        ok("a full render invalidates the live frame",
                !gate.accepts(full, fullGeneration));
        ok("a full render clears the pending request", !gate.consume());

        final int afterFull = gate.request();
        ok("a request after the full render is accepted",
                gate.accepts(afterFull, gate.fullGeneration()));

        final int beforeFrameChange = gate.request();
        gate.cancel();
        ok("a frame change drops the old frame",
                !gate.accepts(beforeFrameChange, gate.fullGeneration()));

        System.out.println("LivePreviewGateCheck: " + checks + " checks passed");
    }

    private static void ok(String what, boolean value) {
        ++checks;
        if (!value) {
            throw new AssertionError(what);
        }
    }
}

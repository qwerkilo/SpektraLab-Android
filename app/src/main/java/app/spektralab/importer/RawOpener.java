package app.spektralab.importer;

import app.spektralab.core.RawOpenPlan;
import app.spektralab.engine.SpektraLabEngine;

/** Open a RAW session with the caps a {@link RawOpenPlan} decided. */
public final class RawOpener {

    /** The engine call, injected so the cap policy is testable. */
    public interface Sessions {
        SpektraLabEngine.Session openRaw(String path, String params, int maxPixels, int longEdge);
    }

    private RawOpener() {
    }

    public static SpektraLabEngine.Session open(Sessions sessions, String path,
                                                String params, RawOpenPlan plan) {
        return sessions.openRaw(path, params, plan.maxPixels, plan.longEdge);
    }
}

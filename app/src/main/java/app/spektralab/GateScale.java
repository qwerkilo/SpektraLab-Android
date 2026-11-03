package app.spektralab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Frame's Scale and the Gate that takes it.
 *
 * <p>A Scale is the Frame's long edge in millimetres; a Gate is the camera
 * aperture that defines the Frame's shape. They are two names for one piece of
 * film geometry, so this module keeps them together: every shipped Gate carries
 * its own measured long edge and aspect, and the UI never offers a Gate and a
 * Scale that disagree.
 *
 * <p>The values are the engine's measured {@code kFormats} from
 * {@code overscan.cpp}; no new Gate, format, or parameter is introduced here.
 * The module is Android-free so the JVM check can compile it without the SDK.
 */
public final class GateScale {
    /** One camera aperture: its id, its catalogue label, and its film geometry. */
    public static final class Gate {
        public final String id;
        public final String label;
        public final float longEdgeMm;
        public final double aspect;

        private Gate(String id, String label, float longEdgeMm, double aspect) {
            this.id = id;
            this.label = label;
            this.longEdgeMm = longEdgeMm;
            this.aspect = aspect;
        }

        /** The panoramic gates carry the wide caption on the Scale page. */
        public boolean panoramic() {
            return aspect >= 1.9;
        }
    }

    /** The engine's own 5 % shape rule: a rebate is cut only for the frame's shape. */
    private static final double ASPECT_TOLERANCE = 0.045;
    /**
     * A Scale and a Gate belong together when their long edges agree. The old
     * Scale page rounded 69.5 to 70, so a legacy Look can still name the same
     * Gate; 112/120 and 168/170 were genuine disagreements and are corrected.
     */
    private static final double SCALE_TOLERANCE_MM = 0.75;

    private static final Gate[] GATES = {
            new Gate("135", "135 全画幅", 36f, 36.25 / 24.3),
            new Gate("135_half", "135 半格", 24f, 24.0 / 18.0),
            new Gate("135_xpan", "135 XPan", 65f, 65.0 / 24.0),
            new Gate("120_645", "645", 56f, 56.0 / 41.5),
            new Gate("120_6x6", "6×6", 56f, 1.0),
            new Gate("120_6x7", "6×7", 69.5f, 69.5 / 56.0),
            new Gate("120_6x8", "6×8", 76f, 76.0 / 56.0),
            new Gate("120_6x9", "6×9", 84f, 84.0 / 56.0),
            new Gate("120_6x12", "6×12", 112f, 112.0 / 56.0),
            new Gate("120_6x17", "6×17", 168f, 168.0 / 56.0),
    };

    private static final List<Gate> ALL = Collections.unmodifiableList(Arrays.asList(GATES));

    /**
     * The Gate a Look actually names. A valid stored pair is kept as-is; an
     * absent or disagreeing Gate falls back to the shipped Gate whose measured
     * long edge is closest to the Look's Scale.
     */
    public static Gate resolve(String gateId, float longEdgeMm) {
        final Gate gate = byId(gateId);
        if (gate != null && Math.abs(gate.longEdgeMm - longEdgeMm) <= SCALE_TOLERANCE_MM) {
            return gate;
        }
        return forLongEdge(longEdgeMm);
    }

    /**
     * The one Gate the Scale and Edge controls send to the engine. A stored
     * Gate and Scale that disagree -- the old 170, 120, and 70 mm pages -- are
     * pulled onto the Gate's measured edge; a missing or unknown Gate falls
     * back to the Gate for the Scale. The Gate carries its own edge, so there
     * is no second pair object to split apart at the call sites.
     */
    public static Gate pair(String gateId, float longEdgeMm) {
        return resolve(gateId, longEdgeMm);
    }

    private GateScale() {
    }

    /** Every Gate the engine's overscan knows, in catalogue order. */
    public static List<Gate> gates() {
        return ALL;
    }

    /** Resolve a Gate by id, or null when the id is not a shipped Gate. */
    public static Gate byId(String id) {
        if (id == null) return null;
        for (Gate gate : GATES) if (gate.id.equals(id)) return gate;
        return null;
    }

    /** The Scale page's default before a Frame exists. */
    public static Gate defaultGate() {
        return GATES[0];
    }

    /**
     * True when this Scale and this Gate describe the same piece of film.
     * A mismatched pair is not a pair.
     */
    public static boolean pairIsValid(String gateId, float longEdgeMm) {
        final Gate gate = byId(gateId);
        return gate != null
                && Math.abs(gate.longEdgeMm - longEdgeMm) <= SCALE_TOLERANCE_MM;
    }

    /** The shipped Gate whose measured long edge is closest to {@code longEdgeMm}. */
    public static Gate forLongEdge(float longEdgeMm) {
        Gate best = GATES[0];
        float bestDistance = Float.MAX_VALUE;
        for (Gate gate : GATES) {
            final float distance = Math.abs(gate.longEdgeMm - longEdgeMm);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = gate;
            }
        }
        return best;
    }

    /**
     * The Gates that fit a Frame's shape, using the engine's own 5 % rule.
     * Before a Frame exists every Gate is available, so the Scale page still
     * presents the complete shipped set.
     */
    public static List<Gate> available(int width, int height) {
        if (width <= 0 || height <= 0) return ALL;
        final double aspect =
                (double) Math.max(width, height) / (double) Math.min(width, height);
        final List<Gate> out = new ArrayList<>();
        for (Gate gate : GATES) {
            if (Math.abs(aspect / gate.aspect - 1.0) <= ASPECT_TOLERANCE) out.add(gate);
        }
        return out;
    }

    /**
     * True when this Frame's shape can carry a Rebate at all. A rebate is cut
     * only for the frame's shape, using the same 5 % rule as {@link #available}:
     * a shape no Gate can hold must not switch the Rebate on. Before a Frame
     * exists every Gate is offered, so the page can be set up in advance.
     */
    public static boolean canHostRebate(int width, int height) {
        return !available(width, height).isEmpty();
    }
}

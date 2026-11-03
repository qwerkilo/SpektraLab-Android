package app.spektralab;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Rebate page's control contract: the mode, gate-shape, holes, and carrier
 * choices it offers, the ranges its sliders clamp to, and the seed bounds.
 *
 * <p>A Look stores ids, not labels, so these lists are the one place the page,
 * the JSON, and the engine wire name agree. A value a newer build stored but
 * this build does not offer is shown by its own id rather than silently
 * replaced by another choice's label.
 *
 * <p>The module is Android-free so the JVM check can exercise it without the
 * SDK.
 */
public final class RebateControls {
    /** One page choice: the Look's stored id and the label the sheet shows. */
    public static final class Choice {
        public final String id;
        public final String label;

        private Choice(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    /** {@code edgeMode}: the whole strip or the filed-down narrow edge. */
    public static final List<Choice> MODES = choices(
            new String[]{"strip", "filed"},
            new String[]{"整条 strip", "窄边 filed"});

    /** {@code edgeGate}: the aperture's corner shape. */
    public static final List<Choice> GATE_STYLES = choices(
            new String[]{"auto", "square", "rounded", "eared", "shouldered", "kicked"},
            new String[]{"自动", "方角", "圆角", "带耳", "带肩", "踢角"});

    /** {@code edgeHoles}: what shows through the perforations. */
    public static final List<Choice> HOLES = choices(
            new String[]{"white", "black"},
            new String[]{"白齿孔（扫描）", "黑齿孔（暗房）"});

    /** {@code edgeCarrier}: what flanks the film outside the gate. */
    public static final List<Choice> CARRIERS = choices(
            new String[]{"none", "black", "open"},
            new String[]{"无", "黑色片夹", "开放"});

    /** Base fog, 0 is clear base and 4 is a dirty strip. */
    public static final float FOG_MIN = 0f, FOG_MAX = 4f;
    /** Light leaks, 0 is none and 4 is a heavy spool leak. */
    public static final float LEAKS_MIN = 0f, LEAKS_MAX = 4f;
    /** The printed frame number; 0 means the engine picks it from the seed. */
    public static final float FRAME_NUMBER_MIN = 0f, FRAME_NUMBER_MAX = 99f;
    /** The recorded aperture; 0 means unknown (the engine assumes f/5.6). */
    public static final float F_NUMBER_MIN = 0f, F_NUMBER_MAX = 64f;
    /** Seeds are non-negative 32-bit integers. */
    public static final int SEED_MIN = 0, SEED_MAX = Integer.MAX_VALUE;

    private RebateControls() {
    }

    private static List<Choice> choices(String[] ids, String[] labels) {
        if (ids.length != labels.length) {
            throw new IllegalArgumentException("choice ids and labels disagree");
        }
        final Choice[] out = new Choice[ids.length];
        for (int i = 0; i < ids.length; ++i) out[i] = new Choice(ids[i], labels[i]);
        return Collections.unmodifiableList(Arrays.asList(out));
    }

    /** True when this list offers the id. */
    public static boolean has(List<Choice> choices, String id) {
        return indexOf(choices, id) >= 0;
    }

    /** The list index for the id, or -1 when this build does not offer it. */
    public static int indexOf(List<Choice> choices, String id) {
        if (choices == null || id == null) return -1;
        for (int i = 0; i < choices.size(); ++i) {
            if (id.equals(choices.get(i).id)) return i;
        }
        return -1;
    }

    /** The id's label, or the id itself when this build does not offer it. */
    public static String labelOf(List<Choice> choices, String id) {
        final int index = indexOf(choices, id);
        return index < 0 ? String.valueOf(id) : choices.get(index).label;
    }

    /** The labels the choice sheet shows, in list order. */
    public static String[] labels(List<Choice> choices) {
        final String[] out = new String[choices.size()];
        for (int i = 0; i < choices.size(); ++i) out[i] = choices.get(i).label;
        return out;
    }

    public static float clampFog(float value) {
        return clamp(value, FOG_MIN, FOG_MAX);
    }

    public static float clampLeaks(float value) {
        return clamp(value, LEAKS_MIN, LEAKS_MAX);
    }

    /** The engine prints a whole frame number, so the slider lands on one. */
    public static float clampFrameNumber(float value) {
        return Math.round(clamp(value, FRAME_NUMBER_MIN, FRAME_NUMBER_MAX));
    }

    public static float clampFNumber(float value) {
        return clamp(value, F_NUMBER_MIN, F_NUMBER_MAX);
    }

    /** A seed is a non-negative int; a larger or negative edit is clamped. */
    public static int clampSeed(long value) {
        return (int) Math.max(SEED_MIN, Math.min((long) SEED_MAX, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

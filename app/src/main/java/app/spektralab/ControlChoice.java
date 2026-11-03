package app.spektralab;

import java.util.List;

/**
 * One choice a secondary Edge page offers: the id stored in the Look and the
 * label the sheet shows. Rebate and Date back both keep their choices as
 * subclasses of this type, so one M3 radio sheet can serve either page while
 * each page still owns its own list.
 *
 * <p>The module is Android-free so the JVM checks can exercise it without the
 * SDK.
 */
public class ControlChoice {
    public final String id;
    public final String label;

    protected ControlChoice(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** True when this list offers the id. */
    public static boolean has(List<? extends ControlChoice> choices, String id) {
        return indexOf(choices, id) >= 0;
    }

    /** The list index for the id, or -1 when this build does not offer it. */
    public static int indexOf(List<? extends ControlChoice> choices, String id) {
        if (choices == null || id == null) return -1;
        for (int i = 0; i < choices.size(); ++i) {
            if (id.equals(choices.get(i).id)) return i;
        }
        return -1;
    }

    /** The id's label, or the id itself when this build does not offer it. */
    public static String labelOf(List<? extends ControlChoice> choices, String id) {
        final int index = indexOf(choices, id);
        return index < 0 ? String.valueOf(id) : choices.get(index).label;
    }

    /** The labels the choice sheet shows, in list order. */
    public static String[] labels(List<? extends ControlChoice> choices) {
        final String[] out = new String[choices.size()];
        for (int i = 0; i < choices.size(); ++i) out[i] = choices.get(i).label;
        return out;
    }
}

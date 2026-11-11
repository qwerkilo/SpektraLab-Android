package app.spektralab.core;

/** The Scale card's size at a system font scale. */
public final class FormatCardLayout {

    private FormatCardLayout() {
    }

    /** The card widens with the font so "36 mm" keeps one line. */
    public static int widthDp(float fontScale) {
        return 84 + Math.round(32f * (scale(fontScale) - 1f));
    }

    /** The card grows with its text so the gate name keeps a line. */
    public static int heightDp(float fontScale) {
        return 105 + Math.round(20f * (scale(fontScale) - 1f));
    }

    /** The card plus its top padding and a little breathing room. */
    public static int stripHeightDp(float fontScale) {
        return heightDp(fontScale) + 22;
    }

    private static float scale(float fontScale) {
        return Math.max(1f, fontScale);
    }
}

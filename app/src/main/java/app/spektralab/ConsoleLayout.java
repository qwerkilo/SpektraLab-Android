package app.spektralab;

/**
 * The Console's width decision, kept out of the Activity so it can be checked
 * without a device.
 *
 * <p>Below {@link #WIDE_BREAKPOINT_DP} the shell keeps its phone column: the
 * stage, readout strip, source strip, and controls stack vertically. At and
 * above the breakpoint the stage and its readout/source strip take one column
 * and the tabs and panel take the other. The control column is capped so a
 * tablet or unfolded foldable gives the Frame the extra width instead of
 * stretching a phone-shaped panel across the window.
 */
public final class ConsoleLayout {
    /** 600 dp is Android's tablet-width breakpoint. */
    public static final int WIDE_BREAKPOINT_DP = 600;
    /** The narrowest control column that still fits the optics rows. */
    public static final int CONTROL_MIN_DP = 300;
    /** Beyond this the panel would read as a phone column with margins. */
    public static final int CONTROL_MAX_DP = 400;
    /** The Frame column never drops below this once the shell is two-column. */
    public static final int STAGE_MIN_DP = 280;
    /** The control column's share of a window that has not reached its cap. */
    private static final float CONTROL_FRACTION = 0.40f;

    private ConsoleLayout() { }

    /** True when the window is wide enough for the two-column Console. */
    public static boolean isWide(int widthDp) {
        return widthDp >= WIDE_BREAKPOINT_DP;
    }

    /**
     * Width of the tabbed control area in either arrangement: the full window
     * in the phone shell, a capped column in the two-column Console.
     */
    public static int controlColumnWidthDp(int widthDp) {
        if (!isWide(widthDp)) return Math.max(0, widthDp);
        final int byFraction = Math.round(widthDp * CONTROL_FRACTION);
        final int clamped = Math.max(CONTROL_MIN_DP,
                Math.min(CONTROL_MAX_DP, byFraction));
        return Math.max(0, Math.min(clamped, widthDp - STAGE_MIN_DP));
    }

    /**
     * Width of the stage column in either arrangement. Together with
     * {@link #controlColumnWidthDp(int)} it always sums to the window width.
     */
    public static int stageColumnWidthDp(int widthDp) {
        if (!isWide(widthDp)) return Math.max(0, widthDp);
        return Math.max(0, widthDp - controlColumnWidthDp(widthDp));
    }
}

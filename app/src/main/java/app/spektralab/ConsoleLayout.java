package app.spektralab;

/**
 * The Console's width decision, kept out of the Activity so it can be checked
 * without a device.
 *
 * <p>Below {@link #WIDE_BREAKPOINT_DP} the shell keeps its phone column: the
 * stage, readout strip, source strip, and controls stack vertically. At and
 * above the breakpoint a Material navigation rail takes the leading edge and
 * the stage/readout/source column and the tabbed control column share what is
 * left. The control column is capped so a tablet or unfolded foldable gives
 * the Frame the extra width instead of stretching a phone-shaped panel across
 * the window.
 */
public final class ConsoleLayout {
    /** 600 dp is Android's tablet-width breakpoint and the rail's minimum. */
    public static final int WIDE_BREAKPOINT_DP = 600;
    /** The M3 navigation rail's container width at and above the breakpoint. */
    public static final int RAIL_WIDTH_DP = 80;
    /** The narrowest control column that still fits the optics rows. */
    public static final int CONTROL_MIN_DP = 300;
    /** Beyond this the panel would read as a phone column with margins. */
    public static final int CONTROL_MAX_DP = 400;
    /**
     * The Frame column never drops below this once the shell has a rail.
     * At the 600 dp breakpoint the arithmetic is fixed: 600 - 80 rail - 300
     * control floor leaves 220 dp for the Frame. The earlier 280 dp stage floor
     * and 300 dp control floor cannot both fit a 600 dp window with the rail;
     * 220 is the documented consequence, not a silent retune.
     */
    public static final int STAGE_MIN_DP = 220;
    /** The control column's share of the content left beside the rail. */
    private static final float CONTROL_FRACTION = 0.40f;

    private ConsoleLayout() { }

    /** True when the window is wide enough for the rail and two-column Console. */
    public static boolean isWide(int widthDp) {
        return widthDp >= WIDE_BREAKPOINT_DP;
    }

    /**
     * Width of the M3 navigation rail: the full 80 dp at and above the
     * breakpoint, zero on a phone where the secondary tabs take its place.
     */
    public static int railWidthDp(int widthDp) {
        return isWide(widthDp) ? RAIL_WIDTH_DP : 0;
    }

    /**
     * Width of the tabbed control area: the full window in the phone shell, a
     * capped column in the two-column Console. The rail is navigation chrome,
     * so it is subtracted before the content columns divide the rest.
     */
    public static int controlColumnWidthDp(int widthDp) {
        if (!isWide(widthDp)) return Math.max(0, widthDp);
        final int content = Math.max(0, widthDp - railWidthDp(widthDp));
        final int byFraction = Math.round(content * CONTROL_FRACTION);
        final int clamped = Math.max(CONTROL_MIN_DP,
                Math.min(CONTROL_MAX_DP, byFraction));
        return Math.max(0, Math.min(clamped, content - STAGE_MIN_DP));
    }

    /**
     * Width of the stage column in either arrangement. Together with the rail
     * and {@link #controlColumnWidthDp(int)} it always sums to the window.
     */
    public static int stageColumnWidthDp(int widthDp) {
        if (!isWide(widthDp)) return Math.max(0, widthDp);
        return Math.max(0, widthDp - railWidthDp(widthDp)
                - controlColumnWidthDp(widthDp));
    }
}

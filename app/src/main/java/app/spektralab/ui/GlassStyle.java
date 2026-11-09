package app.spektralab.ui;

/**
 * The Prism Table glass token set.
 *
 * <p>Android has no backdrop blur for arbitrary in-Activity views, so the glass
 * is a translucent M3 surface with a specular outline. Keep the alpha, radius
 * and elevation values here instead of spreading them through the Activity; a
 * theme rebuild must not change the photograph, only these chrome values.
 */
public final class GlassStyle {
    /** Floating top bar capsule. */
    public static final int TOP_BAR_RADIUS_DP = 22;
    /** The readout table's floating card. */
    public static final int READOUT_RADIUS_DP = 20;
    /** The panel sheet. */
    public static final int SHEET_RADIUS_DP = 26;
    /** The bottom tab dock. */
    public static final int DOCK_RADIUS_DP = 20;

    /** Top of the surface gradient; intentionally translucent. */
    public static final int SURFACE_START_ALPHA = 226;
    /** Bottom of the surface gradient. */
    public static final int SURFACE_END_ALPHA = 186;
    /** The 1 dp specular outline. */
    public static final int OUTLINE_ALPHA = 190;

    /** Lift of the floating bar above the stage. */
    public static final int TOP_BAR_ELEVATION_DP = 10;
    /** Lift of the panel sheet above the page. */
    public static final int SHEET_ELEVATION_DP = 7;
    /** The strip and source strip's floating-card lift. */
    public static final int CARD_ELEVATION_DP = 6;
    /** The folded photograph shelf's card. */
    public static final int SOURCE_RADIUS_DP = 18;

    private GlassStyle() {
    }
}

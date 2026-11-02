package app.spektralab;

/**
 * The Material 3 control tokens the Console asks for instead of carrying its
 * own hex and sp values.
 *
 * <p>This class names the roles, the M3 type scale, the shape scale, and the
 * state-layer opacities. It is Android-free so {@code M3TokenCheck} can guard
 * the roles and the theme resources on the JVM. The colour <em>values</em>
 * live in {@code res/values/colors.xml} and are mapped by {@code AppTheme} and
 * {@code AppTheme.Dark}; on API 31+ the Material library replaces them with the
 * wallpaper scheme. The photographic stage and the spectral R/G/B are not M3
 * roles and are never listed here — they stay measured {@link Palette} values.
 */
public final class M3Tokens {
    private M3Tokens() { }

    /** The M3 color roles the Console controls may ask for. */
    public enum ColorRole {
        PRIMARY("colorPrimary"),
        ON_PRIMARY("colorOnPrimary"),
        PRIMARY_CONTAINER("colorPrimaryContainer"),
        ON_PRIMARY_CONTAINER("colorOnPrimaryContainer"),
        SECONDARY("colorSecondary"),
        ON_SECONDARY("colorOnSecondary"),
        SECONDARY_CONTAINER("colorSecondaryContainer"),
        ON_SECONDARY_CONTAINER("colorOnSecondaryContainer"),
        TERTIARY("colorTertiary"),
        ON_TERTIARY("colorOnTertiary"),
        TERTIARY_CONTAINER("colorTertiaryContainer"),
        ON_TERTIARY_CONTAINER("colorOnTertiaryContainer"),
        ERROR("colorError"),
        ON_ERROR("colorOnError"),
        ERROR_CONTAINER("colorErrorContainer"),
        ON_ERROR_CONTAINER("colorOnErrorContainer"),
        BACKGROUND("android:colorBackground"),
        ON_BACKGROUND("colorOnBackground"),
        SURFACE("colorSurface"),
        ON_SURFACE("colorOnSurface"),
        SURFACE_VARIANT("colorSurfaceVariant"),
        ON_SURFACE_VARIANT("colorOnSurfaceVariant"),
        SURFACE_DIM("colorSurfaceDim"),
        SURFACE_BRIGHT("colorSurfaceBright"),
        SURFACE_CONTAINER_LOWEST("colorSurfaceContainerLowest"),
        SURFACE_CONTAINER_LOW("colorSurfaceContainerLow"),
        SURFACE_CONTAINER("colorSurfaceContainer"),
        SURFACE_CONTAINER_HIGH("colorSurfaceContainerHigh"),
        SURFACE_CONTAINER_HIGHEST("colorSurfaceContainerHighest"),
        SURFACE_INVERSE("colorSurfaceInverse"),
        ON_SURFACE_INVERSE("colorOnSurfaceInverse"),
        PRIMARY_INVERSE("colorPrimaryInverse"),
        OUTLINE("colorOutline"),
        OUTLINE_VARIANT("colorOutlineVariant"),
        SHADOW("colorShadow"),
        SCRIM("colorScrim"),
        SURFACE_TINT("colorSurfaceTint");

        /** The theme attribute a view resolves for this role. */
        public final String attr;

        ColorRole(String attr) {
            this.attr = attr;
        }
    }

    /** Minimum contrast for body text. */
    public static final double BODY_CONTRAST = 4.5;
    /** Minimum contrast for large text and control boundaries. */
    public static final double LARGE_CONTRAST = 3.0;

    /**
     * The M3 type scale. A control asks for a role; the role carries the size,
     * line height, tracking, and weight. The family stays Inter or OPPOSans.
     */
    public enum TypeRole {
        DISPLAY_LARGE(57f, 64f, -0.25f, 400),
        DISPLAY_MEDIUM(45f, 52f, 0f, 400),
        DISPLAY_SMALL(36f, 44f, 0f, 400),
        HEADLINE_LARGE(32f, 40f, 0f, 400),
        HEADLINE_MEDIUM(28f, 36f, 0f, 400),
        HEADLINE_SMALL(24f, 32f, 0f, 400),
        TITLE_LARGE(22f, 28f, 0f, 400),
        TITLE_MEDIUM(16f, 24f, 0.15f, 500),
        TITLE_SMALL(14f, 20f, 0.1f, 500),
        BODY_LARGE(16f, 24f, 0.5f, 400),
        BODY_MEDIUM(14f, 20f, 0.25f, 400),
        BODY_SMALL(12f, 16f, 0.4f, 400),
        LABEL_LARGE(14f, 20f, 0.1f, 500),
        LABEL_MEDIUM(12f, 16f, 0.5f, 500),
        LABEL_SMALL(11f, 16f, 0.5f, 500);

        public final float sizeSp;
        public final float lineHeightSp;
        public final float letterSpacingEm;
        public final int weight;

        TypeRole(float sizeSp, float lineHeightSp, float letterSpacingEm, int weight) {
            this.sizeSp = sizeSp;
            this.lineHeightSp = lineHeightSp;
            this.letterSpacingEm = letterSpacingEm;
            this.weight = weight;
        }
    }

    /** The M3 corner-radius scale, in dp. FULL becomes a pill. */
    public enum ShapeRole {
        EXTRA_SMALL(4),
        SMALL(8),
        MEDIUM(12),
        LARGE(16),
        EXTRA_LARGE(28),
        FULL(999);

        public final int radiusDp;

        ShapeRole(int radiusDp) {
            this.radiusDp = radiusDp;
        }
    }

    /**
     * The M3 state-layer opacities. A state layer is drawn over the role's
     * container colour; it never changes a control's bounds.
     */
    public enum StateLayer {
        HOVER(0.08f),
        FOCUS(0.10f),
        PRESSED(0.10f),
        DRAGGED(0.16f);

        public final float opacity;

        StateLayer(float opacity) {
            this.opacity = opacity;
        }
    }

    /**
     * True when a theme attribute would put the photograph or the spectrum
     * inside the dynamic control scheme. The stage and spectral colours are
     * Palette values, not M3 roles, and must never appear in a theme overlay.
     */
    public static boolean isExcludedColorAttribute(String attr) {
        if (attr == null) return false;
        final String lower = attr.toLowerCase(java.util.Locale.US);
        return lower.contains("stage") || lower.contains("spectrum");
    }

    /** True when the role is a surface a control may sit on. */
    public static boolean isSurface(ColorRole role) {
        return role == ColorRole.SURFACE
                || role == ColorRole.SURFACE_VARIANT
                || role == ColorRole.SURFACE_DIM
                || role == ColorRole.SURFACE_BRIGHT
                || role == ColorRole.SURFACE_CONTAINER_LOWEST
                || role == ColorRole.SURFACE_CONTAINER_LOW
                || role == ColorRole.SURFACE_CONTAINER
                || role == ColorRole.SURFACE_CONTAINER_HIGH
                || role == ColorRole.SURFACE_CONTAINER_HIGHEST
                || role == ColorRole.BACKGROUND;
    }
}

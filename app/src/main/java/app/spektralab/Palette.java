package app.spektralab;

/**
 * The editor's colour system, resolved as named roles for one Theme mode.
 *
 * <p>Views ask this class for a role; they never carry a themed hex value of
 * their own. The module is deliberately Android-free so the JVM check can
 * compile it without the SDK, and so light and dark remain two complete
 * designs rather than one inverted into the other.
 *
 * <p>Roles with alpha (scrims, shadows, guide lines) are authored for the
 * surface they sit on; they are not contrast-tested as text.
 */
public final class Palette {
    public enum Role {
        /** The page behind every control. */
        BACKGROUND,
        /** A card or image slot that sits on the page. */
        SURFACE,
        /** A sheet, pill, or raised card. */
        SURFACE_ELEVATED,
        /** A recessed well, such as the error detail block. */
        SURFACE_SUNKEN,
        /** Primary text. */
        INK,
        /** Secondary text and quiet labels. */
        MUTED_INK,
        /** Borders, rules, slider tracks, and other control boundaries. */
        LINE,
        /** The selected/active colour. */
        ACCENT,
        /** A quiet accent fill for selected backgrounds. */
        ACCENT_SOFT,
        /** The spectral plot's red channel. */
        SPECTRUM_RED,
        /** The spectral plot's green channel. */
        SPECTRUM_GREEN,
        /** The spectral plot's blue channel. */
        SPECTRUM_BLUE,
        /** Text or glyphs on an accent fill. */
        ON_ACCENT,
        /** The photograph's surround. */
        STAGE,
        /** Text and glyphs drawn over the stage. */
        STAGE_INK,
        /** The dimming fill behind a rendering label. */
        STAGE_BUSY,
        /** The header scrim over the photograph, top edge. */
        STAGE_SCRIM_TOP,
        /** The header scrim over the photograph, middle. */
        STAGE_SCRIM_MID,
        /** The header scrim over the photograph, transparent end. */
        STAGE_SCRIM_END,
        /** The before/after divider line. */
        STAGE_DIVIDER,
        /** A shadow cast over the photograph. */
        STAGE_SHADOW,
        /** Frame guide rule. */
        GUIDE_LINE,
        /** Frame guide centre cross. */
        GUIDE_CENTER,
        /** Frame guide corner marks. */
        GUIDE_CORNER,
        /** A message surface that must stand apart from the page. */
        OVERLAY,
        /** Text on the overlay surface. */
        ON_OVERLAY,
        /** An unlit square mark's outline. */
        MARK_OFF,
        /** Status bar behind the stage. */
        SYSTEM_BAR_STAGE,
        /** Navigation bar behind the page. */
        SYSTEM_BAR_BACKGROUND
    }

    private static final Palette LIGHT = buildLight();
    private static final Palette DARK = buildDark();

    private final boolean dark;
    private final int[] colors = new int[Role.values().length];

    private Palette(boolean dark) {
        this.dark = dark;
    }

    private void put(Role role, int color) {
        colors[role.ordinal()] = color;
    }

    /** Resolve a role for this Theme mode. */
    public int color(Role role) {
        if (role == null) throw new IllegalArgumentException("role == null");
        return colors[role.ordinal()];
    }

    public boolean isDark() {
        return dark;
    }

    public static Palette light() {
        return LIGHT;
    }

    public static Palette dark() {
        return DARK;
    }

    private static Palette buildLight() {
        final Palette p = new Palette(false);
        p.put(Role.BACKGROUND, 0xFFF1F1EE);          // warm paper
        p.put(Role.SURFACE, 0xFFE3E3E0);             // ground
        p.put(Role.SURFACE_ELEVATED, 0xFFFAFAF7);
        p.put(Role.SURFACE_SUNKEN, 0xFFE7E8E3);
        p.put(Role.INK, 0xFF171717);
        p.put(Role.MUTED_INK, 0xFF5E5E58);
        p.put(Role.LINE, 0xFF82827A);
        p.put(Role.ACCENT, 0xFF2F5D3A);              // deep safelight green
        p.put(Role.ACCENT_SOFT, 0xFFDDE6DA);
        p.put(Role.SPECTRUM_RED, 0xFFA93226);
        p.put(Role.SPECTRUM_GREEN, 0xFF2E6B3A);
        p.put(Role.SPECTRUM_BLUE, 0xFF24479A);
        p.put(Role.ON_ACCENT, 0xFFF1F1EE);
        p.put(Role.STAGE, 0xFF121614);               // neutral dark surround
        p.put(Role.STAGE_INK, 0xFFF1F1EE);
        p.put(Role.STAGE_BUSY, 0xE6121614);
        p.put(Role.STAGE_SCRIM_TOP, 0xC9050906);
        p.put(Role.STAGE_SCRIM_MID, 0x66050906);
        p.put(Role.STAGE_SCRIM_END, 0x00050906);
        p.put(Role.STAGE_DIVIDER, 0xB3F1F1EE);
        p.put(Role.STAGE_SHADOW, 0x99000000);
        p.put(Role.GUIDE_LINE, 0x26FFFFFF);
        p.put(Role.GUIDE_CENTER, 0x14FFFFFF);
        p.put(Role.GUIDE_CORNER, 0xA6FFFFFF);
        p.put(Role.OVERLAY, 0xFF171717);
        p.put(Role.ON_OVERLAY, 0xFFF1F1EE);
        p.put(Role.MARK_OFF, 0x66171717);
        p.put(Role.SYSTEM_BAR_STAGE, 0xFF121614);
        p.put(Role.SYSTEM_BAR_BACKGROUND, 0xFFF1F1EE);
        return p;
    }

    private static Palette buildDark() {
        final Palette p = new Palette(true);
        p.put(Role.BACKGROUND, 0xFF0E120F);          // graphite
        p.put(Role.SURFACE, 0xFF171C18);
        p.put(Role.SURFACE_ELEVATED, 0xFF1E2420);
        p.put(Role.SURFACE_SUNKEN, 0xFF0B0E0C);
        p.put(Role.INK, 0xFFE8EFE5);
        p.put(Role.MUTED_INK, 0xFFA7B0A4);
        p.put(Role.LINE, 0xFF6A7068);
        p.put(Role.ACCENT, 0xFF9CCB86);              // phosphor green
        p.put(Role.ACCENT_SOFT, 0xFF223A28);
        p.put(Role.SPECTRUM_RED, 0xFFE08A7A);
        p.put(Role.SPECTRUM_GREEN, 0xFF9BD3A4);
        p.put(Role.SPECTRUM_BLUE, 0xFF9BB8EE);
        p.put(Role.ON_ACCENT, 0xFF0E120F);
        p.put(Role.STAGE, 0xFF070A08);
        p.put(Role.STAGE_INK, 0xFFE3ECE0);
        p.put(Role.STAGE_BUSY, 0xE6070A08);
        p.put(Role.STAGE_SCRIM_TOP, 0xD9050906);
        p.put(Role.STAGE_SCRIM_MID, 0x73050906);
        p.put(Role.STAGE_SCRIM_END, 0x00050906);
        p.put(Role.STAGE_DIVIDER, 0xB3E3ECE0);
        p.put(Role.STAGE_SHADOW, 0x99000000);
        p.put(Role.GUIDE_LINE, 0x26FFFFFF);
        p.put(Role.GUIDE_CENTER, 0x14FFFFFF);
        p.put(Role.GUIDE_CORNER, 0xA6FFFFFF);
        p.put(Role.OVERLAY, 0xFF262D27);
        p.put(Role.ON_OVERLAY, 0xFFF2F6EF);
        p.put(Role.MARK_OFF, 0x66E8EFE5);
        p.put(Role.SYSTEM_BAR_STAGE, 0xFF070A08);
        p.put(Role.SYSTEM_BAR_BACKGROUND, 0xFF0E120F);
        return p;
    }

    /**
     * WCAG 2.x contrast ratio between two opaque colours, from 1.0 to 21.0.
     * Alpha is ignored: callers pass the resolved pair they draw.
     */
    public static double contrastRatio(int foreground, int background) {
        final double fg = luminance(foreground);
        final double bg = luminance(background);
        final double lighter = Math.max(fg, bg);
        final double darker = Math.min(fg, bg);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(int color) {
        final double r = channel((color >> 16) & 0xFF);
        final double g = channel((color >> 8) & 0xFF);
        final double b = channel(color & 0xFF);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(int value) {
        final double c = value / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}

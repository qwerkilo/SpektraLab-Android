package app.spektralab;

/**
 * Pure JVM contract for the palette seam: every named role resolves in both
 * Theme modes, body text clears 4.5:1, large text and control boundaries clear
 * 3:1, and the two modes are visibly different systems.
 *
 * <p>No JUnit, no Android: run it with the same javac/java that builds the app.
 */
public final class PaletteCheck {
    private static int checks = 0;

    public static void main(String[] args) {
        knownContrastValues();
        everyRole(Palette.light(), "light");
        everyRole(Palette.dark(), "dark");
        bodyText(Palette.light(), "light");
        bodyText(Palette.dark(), "dark");
        largeTextAndControls(Palette.light(), "light");
        largeTextAndControls(Palette.dark(), "dark");
        spectrumLines(Palette.light(), "light");
        spectrumLines(Palette.dark(), "dark");
        distinctModes(Palette.light(), Palette.dark());
        System.out.println("PaletteCheck: " + checks + " checks passed");
    }

    /** Independent WCAG anchors, not values recomputed from the palette. */
    private static void knownContrastValues() {
        require("black on white is 21:1",
                close(Palette.contrastRatio(0xFF000000, 0xFFFFFFFF), 21.0));
        require("mid grey on white is 4.48:1",
                close(Palette.contrastRatio(0xFF777777, 0xFFFFFFFF), 4.48));
        require("a colour against itself is 1:1",
                close(Palette.contrastRatio(0xFF1C2AA5, 0xFF1C2AA5), 1.0));
    }

    private static void everyRole(Palette palette, String mode) {
        for (Palette.Role role : Palette.Role.values()) {
            require(mode + " palette defines " + role, palette.color(role) != 0);
        }
    }

    private static void bodyText(Palette palette, String mode) {
        Palette.Role[] surfaces = {
                Palette.Role.BACKGROUND,
                Palette.Role.SURFACE,
                Palette.Role.SURFACE_ELEVATED,
                Palette.Role.SURFACE_SUNKEN,
        };
        for (Palette.Role surface : surfaces) {
            atLeast(palette, mode + " ink on " + surface, Palette.Role.INK, surface, 4.5);
            atLeast(palette, mode + " muted ink on " + surface, Palette.Role.MUTED_INK, surface, 4.5);
            atLeast(palette, mode + " accent on " + surface, Palette.Role.ACCENT, surface, 4.5);
        }
        atLeast(palette, mode + " on-accent", Palette.Role.ON_ACCENT, Palette.Role.ACCENT, 4.5);
        atLeast(palette, mode + " stage ink", Palette.Role.STAGE_INK, Palette.Role.STAGE, 4.5);
        atLeast(palette, mode + " on-overlay", Palette.Role.ON_OVERLAY, Palette.Role.OVERLAY, 4.5);
    }

    private static void largeTextAndControls(Palette palette, String mode) {
        Palette.Role[] surfaces = {
                Palette.Role.BACKGROUND,
                Palette.Role.SURFACE,
                Palette.Role.SURFACE_ELEVATED,
                Palette.Role.SURFACE_SUNKEN,
        };
        for (Palette.Role surface : surfaces) {
            atLeast(palette, mode + " line on " + surface, Palette.Role.LINE, surface, 3.0);
        }
        atLeast(palette, mode + " accent as a control", Palette.Role.ACCENT,
                Palette.Role.BACKGROUND, 3.0);
        atLeast(palette, mode + " stage ink as large text", Palette.Role.STAGE_INK,
                Palette.Role.STAGE, 3.0);
    }

    private static void spectrumLines(Palette palette, String mode) {
        Palette.Role[] lines = {
                Palette.Role.SPECTRUM_RED,
                Palette.Role.SPECTRUM_GREEN,
                Palette.Role.SPECTRUM_BLUE,
        };
        for (Palette.Role line : lines) {
            atLeast(palette, mode + " " + line + " on surface", line,
                    Palette.Role.SURFACE_ELEVATED, 3.0);
            atLeast(palette, mode + " " + line + " on page", line,
                    Palette.Role.BACKGROUND, 3.0);
        }
    }

    private static void distinctModes(Palette light, Palette dark) {
        require("light and dark backgrounds differ",
                light.color(Palette.Role.BACKGROUND) != dark.color(Palette.Role.BACKGROUND));
        require("light and dark surfaces differ",
                light.color(Palette.Role.SURFACE) != dark.color(Palette.Role.SURFACE));
        require("light and dark stages differ",
                light.color(Palette.Role.STAGE) != dark.color(Palette.Role.STAGE));
    }

    private static void atLeast(Palette palette, String label, Palette.Role fg,
                                Palette.Role bg, double minimum) {
        final double ratio = Palette.contrastRatio(palette.color(fg), palette.color(bg));
        require(label + " is " + format(ratio) + ":1, needs " + format(minimum) + ":1",
                ratio >= minimum);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 0.01;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

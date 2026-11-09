package app.spektralab;

import app.spektralab.ui.GlassStyle;

/**
 * Guards the Prism Table glass token set. These are visual constants, not
 * runtime state; the check keeps an accidental opaque alpha or a negative
 * radius from shipping unnoticed.
 */
public final class GlassStyleCheck {
    private GlassStyleCheck() {
    }

    public static void main(String[] args) {
        check("top radius", GlassStyle.TOP_BAR_RADIUS_DP, 12, 32);
        check("readout radius", GlassStyle.READOUT_RADIUS_DP, 12, 32);
        check("sheet radius", GlassStyle.SHEET_RADIUS_DP, 16, 40);
        check("dock radius", GlassStyle.DOCK_RADIUS_DP, 12, 32);
        check("source radius", GlassStyle.SOURCE_RADIUS_DP, 8, 28);
        range("start alpha", GlassStyle.SURFACE_START_ALPHA, 160, 255);
        range("end alpha", GlassStyle.SURFACE_END_ALPHA, 120, 240);
        range("outline alpha", GlassStyle.OUTLINE_ALPHA, 80, 255);
        if (GlassStyle.SURFACE_START_ALPHA <= GlassStyle.SURFACE_END_ALPHA) {
            fail("glass gradient must fade toward the bottom");
        }
        range("bar elevation", GlassStyle.TOP_BAR_ELEVATION_DP, 0, 24);
        range("sheet elevation", GlassStyle.SHEET_ELEVATION_DP, 0, 24);
        range("card elevation", GlassStyle.CARD_ELEVATION_DP, 0, 24);
        System.out.println("GlassStyleCheck: all checks passed");
    }

    private static void check(String name, int value, int min, int max) {
        range(name, value, min, max);
    }

    private static void range(String name, int value, int min, int max) {
        if (value < min || value > max) {
            fail(name + " out of range: " + value);
        }
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }
}

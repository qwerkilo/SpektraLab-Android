package app.spektralab;

import app.spektralab.core.ExportMatch;

/** Checks the export self-check's correlation and retry threshold. */
public final class ExportMatchCheck {
    private ExportMatchCheck() {
    }

    public static void main(String[] args) {
        int[] base = new int[48 * 12];
        int[] same = new int[base.length];
        int[] inverted = new int[base.length];
        int[] flat = new int[base.length];
        for (int i = 0; i < base.length; i++) {
            int v = (i * 37) & 0xFF;
            base[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
            same[i] = base[i];
            inverted[i] = 0xFF000000 | ((255 - v) << 16) | ((255 - v) << 8) | (255 - v);
            flat[i] = 0xFF808080;
        }
        check(ExportMatch.ncc(base, same) > 0.999, "identical images correlate");
        check(ExportMatch.ncc(base, inverted) < -0.999, "inverted images anti-correlate");
        check(ExportMatch.ncc(base, flat) < 0, "flat canvas has no correlation");
        check(ExportMatch.ncc(base, new int[base.length]) < 0, "black canvas has no correlation");
        check(!ExportMatch.suspect(0.97), "normal export is accepted");
        check(ExportMatch.suspect(0.2), "noise is suspect");
        check(!ExportMatch.suspect(-1), "unknown score is not a retry");
        System.out.println("ExportMatchCheck: 7 checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

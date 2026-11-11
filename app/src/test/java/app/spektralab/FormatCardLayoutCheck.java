package app.spektralab;

import app.spektralab.core.FormatCardLayout;

/** The Scale card grows with the system font without changing its base size. */
public final class FormatCardLayoutCheck {

    private static int checks;

    public static void main(String[] args) {
        eq(84, FormatCardLayout.widthDp(1f), "base width");
        eq(105, FormatCardLayout.heightDp(1f), "base height");
        eq(127, FormatCardLayout.stripHeightDp(1f), "base strip");
        eq(84, FormatCardLayout.widthDp(0.85f), "small font keeps the base width");
        eq(105, FormatCardLayout.heightDp(0.85f), "small font keeps the base height");

        eq(94, FormatCardLayout.widthDp(1.3f), "1.3x width");
        eq(111, FormatCardLayout.heightDp(1.3f), "1.3x height");
        eq(133, FormatCardLayout.stripHeightDp(1.3f), "1.3x strip");

        eq(116, FormatCardLayout.widthDp(2f), "2.0x width");
        eq(125, FormatCardLayout.heightDp(2f), "2.0x height");
        eq(147, FormatCardLayout.stripHeightDp(2f), "2.0x strip");

        System.out.println("FormatCardLayoutCheck: " + checks + " checks passed");
    }

    private static void eq(int want, int got, String label) {
        ++checks;
        if (want != got) {
            throw new AssertionError(label + ": want " + want + ", got " + got);
        }
    }
}

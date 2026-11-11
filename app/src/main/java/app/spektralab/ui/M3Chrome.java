package app.spektralab.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;

/**
 * Small colour, typeface and text helpers shared by the console chrome.
 *
 * <p>These carry no console state: they exist so a panel can express an M3
 * state layer or a CJK-safe label without re-implementing it.
 */
public final class M3Chrome {
    private M3Chrome() {
    }

    /** A checked/unchecked colour pair for a Material chip or switch. */
    public static ColorStateList checkedColors(int checked, int unchecked) {
        return new ColorStateList(
                new int[][] { { android.R.attr.state_checked }, {} },
                new int[] { checked, unchecked });
    }

    /** Replace a colour's alpha channel; RGB is kept. */
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /** Apply an M3 opacity token to a colour without changing its RGB. */
    public static int withOpacity(int color, float opacity) {
        return withAlpha(color, Math.round(opacity * 255f));
    }

    /** An M3 weight on the brand family; API 24–27 synthesises medium as normal. */
    public static Typeface weighted(Typeface family, int weight) {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            return Typeface.create(family, weight, false);
        }
        return Typeface.create(family, weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
    }

    /** Whether a character belongs to the CJK ranges the brand face covers. */
    public static boolean isCjk(char c) {
        return (c >= 0x2E80 && c <= 0x9FFF)
                || (c >= 0x3000 && c <= 0x303F)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFF00 && c <= 0xFF65);
    }
}

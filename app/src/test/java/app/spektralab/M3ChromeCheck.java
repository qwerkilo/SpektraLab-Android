package app.spektralab;

import app.spektralab.ui.M3Chrome;

/** Checks the shared chrome helpers. */
public final class M3ChromeCheck {
    private M3ChromeCheck() {
    }

    public static void main(String[] args) {
        check(M3Chrome.withAlpha(0x11223344, 0x80) == 0x80223344, "alpha keeps RGB");
        check(M3Chrome.withAlpha(0x11223344, 0xFF) == 0xFF223344, "opaque alpha");
        check(M3Chrome.withOpacity(0x11223344, 0.5f) == 0x80223344, "half opacity");
        check(M3Chrome.isCjk('胶'), "Chinese is CJK");
        check(M3Chrome.isCjk('、'), "ideographic comma is CJK");
        check(M3Chrome.isCjk('Ａ'), "full-width Latin is CJK");
        check(!M3Chrome.isCjk('A'), "ASCII is not CJK");
        check(!M3Chrome.isCjk('é'), "Latin accent is not CJK");
        System.out.println("M3ChromeCheck: 8 checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

package app.spektralab;

import app.spektralab.core.RawImportParams;
import app.spektralab.core.RawSize;

/** The RAW open parameters and the shrink predicate. */
public final class RawImportCheck {

    private static int checks;

    private RawImportCheck() {
    }

    public static void main(String[] args) {
        final String withFilm = RawImportParams.build("cine_still_800t", "kodak_portra_endura", 1.5);
        contains(withFilm, "\"film_stock\":\"cine_still_800t\",");
        contains(withFilm, "\"print_stock\":\"kodak_portra_endura\"");
        contains(withFilm, "\"input_color_space\":\"ProPhoto RGB\"");
        contains(withFilm, "\"input_cctf_decoding\":false");
        contains(withFilm, "\"output_color_space\":\"sRGB\"");
        contains(withFilm, "\"print_exposure\":1.50");
        contains(withFilm, "\"preview_long_edge\":2048");

        final String noFilm = RawImportParams.build(null, "kodak_portra_endura", 0.0);
        absent(noFilm, "film_stock");
        contains(noFilm, "\"print_exposure\":0.00");

        check(RawSize.isShrunk(new int[]{6000, 4000}, 3000, 2000));
        check(!RawSize.isShrunk(new int[]{3000, 2000}, 3000, 2000));
        check(!RawSize.isShrunk(null, 3000, 2000));
        check(!RawSize.isShrunk(new int[]{1}, 3000, 2000));
        check(RawSize.isShrunk(new int[]{2, 1}, 1, 1));

        System.out.println("RawImportCheck: " + checks + " checks passed");
    }

    private static void contains(String text, String needle) {
        ++checks;
        if (!text.contains(needle)) {
            throw new AssertionError("missing: " + needle + " in " + text);
        }
    }

    private static void absent(String text, String needle) {
        ++checks;
        if (text.contains(needle)) {
            throw new AssertionError("must not appear: " + needle + " in " + text);
        }
    }

    private static void check(boolean ok) {
        ++checks;
        if (!ok) {
            throw new AssertionError("unexpected shrink predicate");
        }
    }
}

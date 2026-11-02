package app.spektralab;

import java.util.Arrays;
import java.util.List;

/** Contract for the shelf's brand facet. */
public final class FilmBrandCheck {
    private static int checks;

    public static void main(String[] args) {
        eq("kodak", FilmBrand.of("kodak_portra_400"));
        eq("kodak", FilmBrand.of("kodak_vision3_500t"));
        eq("fujifilm", FilmBrand.of("fujifilm_provia_100f"));
        eq("ilford", FilmBrand.of("ilford_hp5_plus_400"));
        eq("cinestill", FilmBrand.of("cinestill_800t"));
        eq("other", FilmBrand.of("rollei_retro_80s"));
        eq("other", FilmBrand.of(null));
        eq("other", FilmBrand.of(""));

        ok(FilmBrand.matches("kodak_portra_400", FilmBrand.ALL));
        ok(FilmBrand.matches("kodak_portra_400", "kodak"));
        ok(!FilmBrand.matches("kodak_portra_400", "fujifilm"));
        ok(!FilmBrand.matches("kodak_portra_400", "other"));
        ok(FilmBrand.matches("rollei_retro_80s", "other"));

        eq("全部", FilmBrand.label(FilmBrand.ALL));
        eq("KODAK", FilmBrand.label("kodak"));
        eq("FUJIFILM", FilmBrand.label("fujifilm"));
        eq("ILFORD", FilmBrand.label("ilford"));
        eq("CINESTILL", FilmBrand.label("cinestill"));
        eq("OTHER", FilmBrand.label("other"));

        List<String> ids = Arrays.asList(
                "kodak_portra_400", "kodak_ektar_100",
                "fujifilm_c200", "fujifilm_provia_100f",
                "ilford_hp5_plus_400",
                "cinestill_800t");
        eqInt(6, FilmBrand.count(ids, FilmBrand.ALL));
        eqInt(2, FilmBrand.count(ids, "kodak"));
        eqInt(2, FilmBrand.count(ids, "fujifilm"));
        eqInt(1, FilmBrand.count(ids, "ilford"));
        eqInt(1, FilmBrand.count(ids, "cinestill"));
        eqInt(0, FilmBrand.count(ids, "other"));
        eqInt(0, FilmBrand.count(null, FilmBrand.ALL));

        System.out.println("FilmBrandCheck: " + checks + " checks passed");
    }

    private static void eq(String expected, String actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void ok(boolean value) {
        checks++;
        if (!value) throw new AssertionError("expected true");
    }

    private static void eqInt(int expected, int actual) {
        checks++;
        if (expected != actual) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }
}

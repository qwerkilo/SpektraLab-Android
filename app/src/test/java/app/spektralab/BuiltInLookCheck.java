package app.spektralab;

/** The six built-in looks stay sparse and leave the user's frame alone. */
public final class BuiltInLookCheck {

    private static int checks;

    public static void main(String[] args) {
        final java.util.List<BuiltInLook> looks = BuiltInLook.all();
        eq(6, looks.size(), "six built-in looks");
        final java.util.Set<String> ids = new java.util.HashSet<>();
        for (BuiltInLook look : looks) {
            ok(!look.id().isEmpty(), "look id");
            ok(ids.add(look.id()), "unique id " + look.id());
            ok(!look.name().isEmpty(), "look name");
            ok(!look.filmId().isEmpty(), "look film " + look.id());
            ok(!look.printId().isEmpty(), "look print " + look.id());
        }

        final Look base = new Look();
        base.filmStock = "fujifilm_c200";
        base.printStock = "fujifilm_crystal_archive_typeii";
        base.grainAmount = 0.42f;
        base.geometryTurns = 90;
        base.geometryCropW = 0.8f;
        base.view.temperature = 12f;
        base.dateOn = true;
        base.dateText = "26 10 31";

        final Look portra = BuiltInLook.all().get(0).apply(base);
        eq("kodak_portra_400", portra.filmStock, "film switched");
        eq("kodak_portra_endura", portra.printStock, "paper switched");
        eq(0.95f, portra.grainAmount, "grain authored");
        eq("center_weighted", portra.meterMethod, "metering authored");
        eq(90, portra.geometryTurns, "composition preserved");
        eq(0.8f, portra.geometryCropW, "crop preserved");
        eq(12f, portra.view.temperature, "view preserved");
        eq(true, portra.dateOn, "date switch preserved");
        eq("26 10 31", portra.dateText, "date text preserved");
        eq(0.42f, base.grainAmount, "base not mutated");

        System.out.println("BuiltInLookCheck: " + checks + " checks passed");
    }

    private static void ok(boolean condition, String label) {
        ++checks;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void eq(Object want, Object got, String label) {
        ++checks;
        if (!java.util.Objects.equals(want, got)) {
            throw new AssertionError(label + ": want " + want + ", got " + got);
        }
    }

    private static void eq(float want, float got, String label) {
        ++checks;
        if (Math.abs(want - got) > 1e-5f) {
            throw new AssertionError(label + ": want " + want + ", got " + got);
        }
    }
}

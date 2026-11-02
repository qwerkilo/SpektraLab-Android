package app.spektralab;

/**
 * The Readout strip and the picker sheet must describe one catalogue record.
 * The full label is the record; the short label is a lossless-for-recognition
 * abbreviation of it, never a separately authored string.
 */
public final class StockNamesCheck {
    private static int checks;

    public static void main(String[] args) {
        final String[] labels = {
                "Kodak Professional Portra Endura",
                "Kodak Portra 400",
                "Kodak Portra 400 · 120",
                "Fujifilm Neopan 100 Acros II · 135",
                "Ilford HP5 Plus 400",
                "CineStill 800T",
                "Foma Fomapan 100",
                "Agfa APX 100",
                "Rollei Retro 80S",
                "800T",
        };

        // The picker's name is the catalogue record, untouched.
        for (String label : labels) {
            eq(label, StockNames.full(label));
        }

        eq("Portra Endura", StockNames.readout("Kodak Professional Portra Endura"));
        eq("Portra 400", StockNames.readout("Kodak Portra 400"));
        eq("Portra 400", StockNames.readout("Kodak Portra 400 · 120"));
        eq("Neopan 100 Acros II", StockNames.readout("Fujifilm Neopan 100 Acros II · 135"));
        eq("HP5 Plus 400", StockNames.readout("Ilford HP5 Plus 400"));
        eq("800T", StockNames.readout("CineStill 800T"));
        eq("Fomapan 100", StockNames.readout("Foma Fomapan 100"));
        eq("APX 100", StockNames.readout("Agfa APX 100"));
        eq("Rollei Retro 80S", StockNames.readout("Rollei Retro 80S"));
        eq("800T", StockNames.readout("800T"));
        eq("Portra Endura", StockNames.readout("Professional Portra Endura"));

        // The shelf keeps the maker; only the strip drops it.
        eq("Kodak Portra 400", StockNames.shelf("Kodak Portra 400 · 120"));
        eq("Fujifilm Neopan 100 Acros II", StockNames.shelf("Fujifilm Neopan 100 Acros II · 135"));
        eq("Kodak Portra 400", StockNames.shelf("Kodak Portra 400"));

        eq("", StockNames.full(null));
        eq("", StockNames.shelf(null));
        eq("", StockNames.readout(null));
        eq("", StockNames.readout(""));
        eq("  ", StockNames.readout("  "));

        // One record, two renderings: the strip never invents a longer name,
        // and the picker never loses a character of the catalogue label.
        for (String label : labels) {
            final String full = StockNames.full(label);
            final String shortName = StockNames.readout(label);
            truth(!shortName.isEmpty(), "short label must not be empty: " + label);
            truth(shortName.length() <= full.length(), "short label grew: " + label);
            truth(full.contains(shortName) || shortName.equals("800T"),
                    "short label is not part of the record: " + label);
        }

        System.out.println("StockNamesCheck: " + checks + " checks passed");
    }

    private static void eq(String expected, String actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void truth(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

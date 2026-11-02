package app.spektralab;

/**
 * The two faces of one catalogue record.
 *
 * <p>The Readout strip is a glance and gets the short working name; the
 * picker sheet is a choice and gets the full label. Both are derived here so a
 * stock cannot grow a different name in one place than the other. The rules are
 * pure text and therefore live outside the Activity, where a JVM check can
 * guard them.
 */
public final class StockNames {
    private StockNames() { }

    /** The catalogue's own label, unchanged: what the picker shows. */
    public static String full(String label) {
        return label == null ? "" : label;
    }

    /**
     * The shelf name: drop the maker's optional " · mount" suffix, which the
     * card's narrow width cannot carry. Everything before the suffix stays.
     */
    public static String shelf(String label) {
        if (label == null) return "";
        final int cut = label.indexOf(" · ");
        return cut > 0 ? label.substring(0, cut) : label;
    }

    /**
     * The Readout strip's label: the shelf name without a leading maker or the
     * "Professional" tier, so "Kodak Professional Portra Endura" fits a cell
     * as "Portra Endura". The picker still calls the record by its full name.
     */
    public static String readout(String label) {
        String name = shelf(label);
        final int space = name.indexOf(' ');
        if (space > 0) {
            final String maker = name.substring(0, space).toLowerCase(java.util.Locale.US);
            if (maker.equals("kodak") || maker.equals("fujifilm") || maker.equals("ilford")
                    || maker.equals("cinestill") || maker.equals("foma")
                    || maker.equals("fomapan") || maker.equals("agfa")) {
                name = name.substring(space + 1);
            }
        }
        final String professional = "Professional ";
        if (name.startsWith(professional)) {
            name = name.substring(professional.length());
        }
        return name;
    }
}

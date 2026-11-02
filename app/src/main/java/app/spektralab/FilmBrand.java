package app.spektralab;

import java.util.List;

/**
 * The stock catalogue's brand facet. The shelf has more stocks than fit at
 * once, so it is filtered by brand, not by a made-up "series": Kodak,
 * Fujifilm, Ilford, CineStill, and anything the catalogue does not classify.
 *
 * <p>The mapping is from the stock id, because that is what the catalogue
 * ships; labels are presentation only. Ids are stable across releases.
 */
public final class FilmBrand {
    public static final String ALL = "all";
    public static final String OTHER = "other";

    /** Shelf order: the whole catalogue first, then the four shipped brands. */
    public static final String[] ORDER = {ALL, "kodak", "fujifilm", "ilford", "cinestill", OTHER};

    private FilmBrand() { }

    /** The brand a stock id belongs to; unknown or null is {@link #OTHER}. */
    public static String of(String id) {
        if (id == null) return OTHER;
        if (id.startsWith("kodak")) return "kodak";
        if (id.startsWith("fujifilm")) return "fujifilm";
        if (id.startsWith("ilford")) return "ilford";
        if (id.startsWith("cinestill")) return "cinestill";
        return OTHER;
    }

    /** Whether a stock belongs on the shelf for this filter. */
    public static boolean matches(String id, String brand) {
        return ALL.equals(brand) || of(id).equals(brand);
    }

    /** The filter's own label; the count is appended by the row. */
    public static String label(String brand) {
        if ("kodak".equals(brand)) return "KODAK";
        if ("fujifilm".equals(brand)) return "FUJIFILM";
        if ("ilford".equals(brand)) return "ILFORD";
        if ("cinestill".equals(brand)) return "CINESTILL";
        if (OTHER.equals(brand)) return "OTHER";
        return "全部";
    }

    /** How many stocks this filter would show. */
    public static int count(List<String> ids, String brand) {
        if (ids == null) return 0;
        int n = 0;
        for (String id : ids) if (matches(id, brand)) n++;
        return n;
    }
}

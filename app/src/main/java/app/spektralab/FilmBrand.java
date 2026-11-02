package app.spektralab;

import java.util.Arrays;
import java.util.Collections;
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
    /** The filter that shows the whole catalogue. It is not a brand. */
    public static final String ALL = "all";
    public static final String OTHER = "other";

    private static final String[] IDS = {ALL, "kodak", "fujifilm", "ilford", "cinestill", OTHER};
    /** Index 0 has no prefix: it is the filter, not a stock family. */
    private static final String[] PREFIXES = {"", "kodak", "fujifilm", "ilford", "cinestill", ""};
    private static final String[] LABELS = {"全部", "KODAK", "FUJIFILM", "ILFORD", "CINESTILL", "OTHER"};

    private FilmBrand() { }

    /** Shelf order: the whole catalogue first, then the four shipped brands. */
    public static List<String> order() {
        return Collections.unmodifiableList(Arrays.asList(IDS));
    }

    /** The brand a stock id belongs to; unknown or null is {@link #OTHER}. */
    public static String of(String id) {
        if (id != null) {
            for (int i = 1; i < IDS.length - 1; ++i) {
                if (id.startsWith(PREFIXES[i])) return IDS[i];
            }
        }
        return OTHER;
    }

    /** Whether a stock belongs on the shelf for this filter. */
    public static boolean matches(String id, String brand) {
        return ALL.equals(brand) || of(id).equals(brand);
    }

    /** The filter's own label; the count is appended by the row. */
    public static String label(String brand) {
        for (int i = 0; i < IDS.length; ++i) {
            if (IDS[i].equals(brand)) return LABELS[i];
        }
        return LABELS[LABELS.length - 1];
    }

    /** How many stocks this filter would show. */
    public static int count(List<String> ids, String brand) {
        if (ids == null) return 0;
        int n = 0;
        for (String id : ids) if (matches(id, brand)) n++;
        return n;
    }
}

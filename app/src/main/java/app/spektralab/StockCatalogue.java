package app.spektralab;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One read of the stock catalogue: id, display label, and type. */
public final class StockCatalogue {
    public static final class Stock {
        public final String id;
        public final String label;
        public final String type;
        public final String use;

        Stock(String id, String label, String type, String use) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.use = use;
        }
    }

    private StockCatalogue() { }

    /** Read one `films` or `prints` array. Missing ids are skipped, never invented. */
    public static List<Stock> read(JSONArray array) {
        if (array == null) return Collections.emptyList();
        final List<Stock> stocks = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            final JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            final String id = o.optString("id", "");
            if (id.isEmpty()) continue;
            final String name = o.optString("name", id);
            final String type = o.optString("type", "");
            final String use = o.optString("use", "");
            stocks.add(new Stock(id, label(name, type, use), type, use));
        }
        return stocks;
    }

    static String label(String name, String type, String use) {
        final StringBuilder label = new StringBuilder(name);
        if ("positive".equals(type)) label.append(" · 反转");
        else if ("negative".equals(type)) label.append(" · 负片");
        if ("cine".equals(use)) label.append(" · 电影");
        return label.toString();
    }
}

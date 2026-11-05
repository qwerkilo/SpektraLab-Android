package app.spektralab;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** Pure JVM contract for the catalogue read shared by film and print lists. */
public final class StockCatalogueCheck {
    private static int checks;

    public static void main(String[] args) {
        readsFilmLabels();
        readsPrintLabels();
        skipsMissingIds();
        keepsOrder();
        handlesEmptyAndNull();
        System.out.println("StockCatalogueCheck: " + checks + " checks passed");
    }

    private static void readsFilmLabels() {
        final List<StockCatalogue.Stock> films = StockCatalogue.read(array(
                stock("kodak_portra_400", "Kodak Portra 400", "negative", ""),
                stock("kodak_ektachrome_e100", "Kodak Ektachrome E100", "positive", ""),
                stock("cinestill_800t", "CineStill 800T", "negative", "cine")));
        eq(3, films.size());
        eq("Kodak Portra 400 · 负片", films.get(0).label);
        eq("Kodak Ektachrome E100 · 反转", films.get(1).label);
        eq("CineStill 800T · 负片 · 电影", films.get(2).label);
    }

    private static void readsPrintLabels() {
        final List<StockCatalogue.Stock> prints = StockCatalogue.read(array(
                stock("kodak_portra_endura", "Kodak Portra Endura", "print", "")));
        eq("Kodak Portra Endura", prints.get(0).label);
    }

    private static void skipsMissingIds() {
        final List<StockCatalogue.Stock> films = StockCatalogue.read(array(
                stock("", "No id", "negative", ""),
                null,
                stock("ok", "Has id", "negative", "")));
        eq(1, films.size());
        eq("ok", films.get(0).id);
    }

    private static void keepsOrder() {
        final List<StockCatalogue.Stock> films = StockCatalogue.read(array(
                stock("b", "B", "", ""), stock("a", "A", "", "")));
        eq("b", films.get(0).id);
        eq("a", films.get(1).id);
    }

    private static void handlesEmptyAndNull() {
        eq(0, StockCatalogue.read(null).size());
        eq(0, StockCatalogue.read(new JSONArray()).size());
    }

    private static JSONArray array(JSONObject... values) {
        final JSONArray array = new JSONArray();
        for (JSONObject value : values) array.put(value);
        return array;
    }

    private static JSONObject stock(String id, String name, String type, String use) {
        final JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("type", type);
        o.put("use", use);
        return o;
    }

    private static void ok(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }
}

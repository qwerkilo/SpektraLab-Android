package app.spektralab;

/**
 * Pure JVM contract for the Date back page.
 *
 * <p>The page, the Look, and the engine have to agree about which placement,
 * corner, and style ids exist, what the sliders' ranges are, and how a frame's
 * EXIF capture date becomes the default imprint. The default must follow the
 * file, so a missing or malformed EXIF date is proven to take the caller's
 * fallback rather than reading a clock from inside this module.
 */
public final class DateBackCheck {
    private static int checks;

    public static void main(String[] args) {
        choices();
        labelFallback();
        rangeClamps();
        exifDate();
        fallbackDate();

        System.out.println("DateBackCheck: " + checks + " checks passed");
    }

    private static void choices() {
        require("two date placements", DateBack.PLACEMENTS.size() == 2);
        require("frame is a placement", ControlChoice.has(DateBack.PLACEMENTS, "frame"));
        require("rebate is a placement", ControlChoice.has(DateBack.PLACEMENTS, "rebate"));

        require("four date corners", DateBack.CORNERS.size() == 4);
        require("bottom-right is a corner", ControlChoice.has(DateBack.CORNERS, "br"));
        require("top-left is a corner", ControlChoice.has(DateBack.CORNERS, "tl"));

        require("three date styles", DateBack.STYLES.size() == 3);
        require("LCD is a style", ControlChoice.has(DateBack.STYLES, "lcd"));
        require("dots is a style", ControlChoice.has(DateBack.STYLES, "dots"));
        require("data is a style", ControlChoice.has(DateBack.STYLES, "data"));

        // Every default in Look is one of the page's real ids, not a label that
        // only the page happens to understand.
        final Look look = new Look();
        require("default placement is in the placement list",
                ControlChoice.has(DateBack.PLACEMENTS, look.datePlacement));
        require("default corner is in the corner list",
                ControlChoice.has(DateBack.CORNERS, look.dateCorner));
        require("default style is in the style list",
                ControlChoice.has(DateBack.STYLES, look.dateStyle));

        for (String[] ids : new String[][]{
                ids(DateBack.PLACEMENTS), ids(DateBack.CORNERS), ids(DateBack.STYLES)}) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String id : ids) {
                require("id is not empty", id != null && id.length() > 0);
                require("id is unique: " + id, seen.add(id));
            }
        }
    }

    private static void labelFallback() {
        require("frame has its label",
                "画面".equals(ControlChoice.labelOf(DateBack.PLACEMENTS, "frame")));
        require("bottom-right has its label",
                "右下".equals(ControlChoice.labelOf(DateBack.CORNERS, "br")));
        require("dots has its label",
                "点阵 dots".equals(ControlChoice.labelOf(DateBack.STYLES, "dots")));
        // A Look from a newer build may name an id this build does not offer:
        // showing the id beats showing a different choice's label.
        require("an unknown style shows its own id",
                "future_style".equals(ControlChoice.labelOf(DateBack.STYLES, "future_style")));
        require("an unknown style has no index",
                ControlChoice.indexOf(DateBack.STYLES, "future_style") == -1);
        require("a known style has its catalogue index",
                ControlChoice.indexOf(DateBack.STYLES, "dots") == 1);
    }

    private static void rangeClamps() {
        require("left inset below zero clamps", DateBack.clampInset(-0.5f) == 0f);
        require("left inset above thirty clamps", DateBack.clampInset(31f) == 30f);
        require("a real inset survives", DateBack.clampInset(2.4f) == 2.4f);

        require("size below the minimum clamps", DateBack.clampSize(0.1f) == 0.4f);
        require("size above the maximum clamps", DateBack.clampSize(4f) == 3f);
        require("a real size survives", DateBack.clampSize(1.8f) == 1.8f);

        require("exposure below the minimum clamps", DateBack.clampEv(-5f) == -2f);
        require("exposure above the maximum clamps", DateBack.clampEv(10f) == 8f);
        require("a real exposure survives", DateBack.clampEv(3.5f) == 3.5f);
    }

    private static void exifDate() {
        // The camera's own TAG_DATETIME_ORIGINAL shape.
        require("a full EXIF date becomes the date back's format",
                "'26 10 31".equals(DateBack.formatExif("2026:10:31 14:02:00")));
        // Month and day lose their leading zero, exactly as the upstream page
        // printed them: '26 3 5, not '26 03 05.
        require("a single-digit month and day lose their zero",
                "'26 3 5".equals(DateBack.formatExif("2026:03:05 00:00:00")));
        require("dash separators are accepted",
                "'26 10 31".equals(DateBack.formatExif("2026-10-31 14:02:00")));
        require("a two-digit year stays two digits",
                "'05 1 2".equals(DateBack.formatDate(2005, 1, 2)));

        require("no EXIF date has no default", DateBack.formatExif(null) == null);
        require("an empty EXIF date has no default", DateBack.formatExif("") == null);
        require("a malformed EXIF date has no default",
                DateBack.formatExif("not a date") == null);
        require("an impossible month has no default",
                DateBack.formatExif("2026:13:01 00:00:00") == null);
        require("an impossible day has no default",
                DateBack.formatExif("2026:02:31 00:00:00") == null);
    }

    private static void fallbackDate() {
        final String fallback = "'26 11 3";
        require("a frame with EXIF ignores the fallback",
                "'26 10 31".equals(DateBack.imprintText("2026:10:31 14:02:00", fallback)));
        require("a frame without EXIF takes the fallback",
                fallback.equals(DateBack.imprintText(null, fallback)));
        require("a frame with a broken EXIF date takes the fallback",
                fallback.equals(DateBack.imprintText("2026:99:99 00:00:00", fallback)));
    }

    private static String[] ids(java.util.List<DateBack.Choice> choices) {
        String[] out = new String[choices.size()];
        for (int i = 0; i < choices.size(); ++i) out[i] = choices.get(i).id;
        return out;
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

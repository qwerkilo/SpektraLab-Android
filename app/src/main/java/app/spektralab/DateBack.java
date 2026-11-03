package app.spektralab;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The Date back page's control contract: the placement, corner, and style
 * choices it offers, the ranges its sliders clamp to, and the conversion from
 * a Frame's EXIF capture date to the default imprint text.
 *
 * <p>A Look stores ids, not labels, so these lists are the one place the page,
 * the JSON, and the engine wire name agree. A value a newer build stored but
 * this build does not offer is shown by its own id rather than silently
 * replaced by another choice's label.
 *
 * <p>The formatter is Android-free so the JVM check can exercise it without
 * the SDK. It takes the fallback text as an argument: the default follows the
 * Frame's own EXIF date, and only a Frame with no usable date asks the caller
 * for its fallback.
 */
public final class DateBack {
    /** One page choice: the Look's stored id and the label the sheet shows. */
    public static final class Choice extends ControlChoice {
        private Choice(String id, String label) {
            super(id, label);
        }
    }

    /** {@code datePlacement}: inside the picture or on the film edge. */
    public static final List<Choice> PLACEMENTS = choices(
            new String[]{"frame", "rebate"},
            new String[]{"画面", "片边"});

    /** {@code dateCorner}: which corner of the chosen placement. */
    public static final List<Choice> CORNERS = choices(
            new String[]{"br", "bl", "tr", "tl"},
            new String[]{"右下", "左下", "右上", "左上"});

    /** {@code dateStyle}: the LED, dot-matrix, or camera-data face. */
    public static final List<Choice> STYLES = choices(
            new String[]{"lcd", "dots", "data"},
            new String[]{"LCD 七段", "点阵 dots", "拍摄数据 data"});

    /** Distance from the gate's edge to the text, in millimetres. */
    public static final float INSET_MIN = 0f, INSET_MAX = 30f;
    /** Text height relative to the chosen style's own face. */
    public static final float SIZE_MIN = 0.4f, SIZE_MAX = 3f;
    /** Exposure above 18 % grey, in stops. */
    public static final float EV_MIN = -2f, EV_MAX = 8f;

    private DateBack() {
    }

    private static List<Choice> choices(String[] ids, String[] labels) {
        if (ids.length != labels.length) {
            throw new IllegalArgumentException("choice ids and labels disagree");
        }
        final Choice[] out = new Choice[ids.length];
        for (int i = 0; i < ids.length; ++i) out[i] = new Choice(ids[i], labels[i]);
        return Collections.unmodifiableList(Arrays.asList(out));
    }

    public static float clampInset(float value) {
        return Math.max(INSET_MIN, Math.min(INSET_MAX, value));
    }

    public static float clampSize(float value) {
        return Math.max(SIZE_MIN, Math.min(SIZE_MAX, value));
    }

    public static float clampEv(float value) {
        return Math.max(EV_MIN, Math.min(EV_MAX, value));
    }

    /**
     * A camera's {@code TAG_DATETIME_ORIGINAL} ("2026:10:31 14:02:00") as the
     * date back prints it: "'26 10 31". Returns {@code null} when the value is
     * absent or not a real date, so the caller can supply its own fallback.
     */
    public static String formatExif(String value) {
        if (value == null) return null;
        final String text = value.trim();
        if (text.length() < 10) return null;
        // EXIF uses colons; some writers use dashes. Accept both, but nothing
        // else, so a free-text field is not mistaken for a date.
        if (!isSeparator(text.charAt(4)) || !isSeparator(text.charAt(7))) return null;
        final int year = parse(text, 0, 4);
        final int month = parse(text, 5, 7);
        final int day = parse(text, 8, 10);
        if (year < 0 || month < 0 || day < 0) return null;
        return formatDate(year, month, day);
    }

    /**
     * The imprint for a Frame: its EXIF date when that is usable, otherwise the
     * caller's fallback. A clock is deliberately not read here; the caller
     * owns whether "today" is an honest default for a file with no date.
     */
    public static String imprintText(String exif, String fallback) {
        final String formatted = formatExif(exif);
        return formatted == null ? fallback : formatted;
    }

    /** "2026, 10, 31" as "'26 10 31"; validates the calendar day. */
    public static String formatDate(int year, int month, int day) {
        if (year < 0 || year > 9999 || month < 1 || month > 12
                || day < 1 || day > daysInMonth(year, month)) {
            return null;
        }
        return String.format(Locale.US, "'%02d %d %d", year % 100, month, day);
    }

    private static boolean isSeparator(char c) {
        return c == ':' || c == '-';
    }

    /** A run of exactly this many ASCII digits, or -1. */
    private static int parse(String text, int from, int to) {
        int value = 0;
        for (int i = from; i < to; ++i) {
            final char c = text.charAt(i);
            if (c < '0' || c > '9') return -1;
            value = value * 10 + (c - '0');
        }
        return value;
    }

    private static int daysInMonth(int year, int month) {
        switch (month) {
            case 2: return isLeapYear(year) ? 29 : 28;
            case 4: case 6: case 9: case 11: return 30;
            default: return 31;
        }
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }
}

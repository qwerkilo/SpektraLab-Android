package app.spektralab.ui;

import app.spektralab.Palette;

/**
 * The persisted presentation setting. Light and dark are separate Palette
 * designs; this enum only names which one is active.
 *
 * <p>Theme mode is presentation state: it is never written into a Look, a
 * Frame, or the engine.
 */
public enum ThemeMode {
    LIGHT,
    DARK;

    /** SharedPreferences key. The file is chosen by the Activity. */
    public static final String PREF_KEY = "theme_mode";

    /** Unknown or missing values open light, the current shipped default. */
    public static ThemeMode fromStored(String value) {
        return "dark".equals(value) ? DARK : LIGHT;
    }

    public String storedValue() {
        return this == DARK ? "dark" : "light";
    }

    public ThemeMode toggled() {
        return this == DARK ? LIGHT : DARK;
    }

    public Palette palette() {
        return this == DARK ? Palette.dark() : Palette.light();
    }
}

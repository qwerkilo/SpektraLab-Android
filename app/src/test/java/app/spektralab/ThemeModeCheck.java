package app.spektralab;

/**
 * Pure JVM contract for the persisted Theme mode: stored strings round-trip,
 * unknown values fall back to light, and toggling is an involution.
 */
public final class ThemeModeCheck {
    private static int checks = 0;

    public static void main(String[] args) {
        require("missing preference opens light", ThemeMode.fromStored(null) == ThemeMode.LIGHT);
        require("empty preference opens light", ThemeMode.fromStored("") == ThemeMode.LIGHT);
        require("light preference opens light", ThemeMode.fromStored("light") == ThemeMode.LIGHT);
        require("dark preference opens dark", ThemeMode.fromStored("dark") == ThemeMode.DARK);
        require("unknown preference opens light", ThemeMode.fromStored("sepia") == ThemeMode.LIGHT);

        require("light stores as light", "light".equals(ThemeMode.LIGHT.storedValue()));
        require("dark stores as dark", "dark".equals(ThemeMode.DARK.storedValue()));
        require("stored value round-trips",
                ThemeMode.fromStored(ThemeMode.DARK.storedValue()) == ThemeMode.DARK);

        require("light toggles to dark", ThemeMode.LIGHT.toggled() == ThemeMode.DARK);
        require("dark toggles to light", ThemeMode.DARK.toggled() == ThemeMode.LIGHT);
        require("two toggles return to the same mode",
                ThemeMode.LIGHT.toggled().toggled() == ThemeMode.LIGHT);
        require("light resolves a light palette", !ThemeMode.LIGHT.palette().isDark());
        require("dark resolves a dark palette", ThemeMode.DARK.palette().isDark());

        System.out.println("ThemeModeCheck: " + checks + " checks passed");
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

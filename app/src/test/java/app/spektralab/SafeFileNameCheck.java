package app.spektralab;

/** A provider-supplied display name must never select the cache directory. */
public final class SafeFileNameCheck {
    private static int checks;

    public static void main(String[] args) {
        eq("portra.jpg", SafeFileName.of("portra.jpg", "import.bin"));
        eq("evil.xml", SafeFileName.of("../../shared_prefs/evil.xml", "import.bin"));
        eq("evil.xml", SafeFileName.of("/data/data/app/shared_prefs/evil.xml", "import.bin"));
        eq("evil.dng", SafeFileName.of("a/b/../evil.dng", "import.bin"));
        eq("import.bin", SafeFileName.of("..", "import.bin"));
        eq("import.bin", SafeFileName.of(".", "import.bin"));
        eq("import.bin", SafeFileName.of("", "import.bin"));
        eq("import.bin", SafeFileName.of(null, "import.bin"));
        eq("import.bin", SafeFileName.of("/", "import.bin"));
        eq("import.bin", SafeFileName.of("bad\0name", "import.bin"));
        eq("..hidden", SafeFileName.of("..hidden", "import.bin"));

        System.out.println("SafeFileNameCheck: " + checks + " checks passed");
    }

    private static void eq(String expected, String actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }
}

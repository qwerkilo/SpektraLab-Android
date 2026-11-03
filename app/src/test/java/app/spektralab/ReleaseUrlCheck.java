package app.spektralab;

/** An announcement URL may only reach the release page or the update host. */
public final class ReleaseUrlCheck {
    private static int checks;

    public static void main(String[] args) {
        eq(ReleaseUrl.DEFAULT, ReleaseUrl.sanitize(null));
        eq(ReleaseUrl.DEFAULT, ReleaseUrl.sanitize("http://github.com/SAINTTaiYi/SpektraLab-Android/releases/latest"));
        eq(ReleaseUrl.DEFAULT, ReleaseUrl.sanitize("javascript:alert(1)"));
        eq(ReleaseUrl.DEFAULT, ReleaseUrl.sanitize("https://evil.example/releases/latest"));
        eq(ReleaseUrl.DEFAULT, ReleaseUrl.sanitize("https://github.com/attacker/repo/releases/latest"));
        eq(ReleaseUrl.DEFAULT, ReleaseUrl.sanitize("not a url"));

        eq("https://github.com/SAINTTaiYi/SpektraLab-Android/releases/latest",
                ReleaseUrl.sanitize("https://github.com/SAINTTaiYi/SpektraLab-Android/releases/latest"));
        eq("https://github.com/qwerkilo/SpektraLab-Android/releases/latest",
                ReleaseUrl.sanitize("https://github.com/qwerkilo/SpektraLab-Android/releases/latest"));
        eq("https://update.workshop.skin/latest.json",
                ReleaseUrl.sanitize("https://update.workshop.skin/latest.json"));
        // The fork's own release site is the announcement's URL today.
        eq("https://spektralab.workshop.skin/",
                ReleaseUrl.sanitize("https://spektralab.workshop.skin/"));
        eq("https://spektralab.workshop.skin/download",
                ReleaseUrl.sanitize("https://spektralab.workshop.skin/download"));
        eq("https://spektralab.workshop.skin/update.json",
                ReleaseUrl.sanitize("https://spektralab.workshop.skin/update.json"));
        // A lookalike host is not the fork's host.
        eq(ReleaseUrl.DEFAULT,
                ReleaseUrl.sanitize("https://spektralab.workshop.skin.evil.example/download"));
        eq(ReleaseUrl.DEFAULT,
                ReleaseUrl.sanitize("https://spektralab.workshop.skin@evil.example/download"));
        eq("https://UPDATE.WORKSHOP.SKIN/latest.json",
                ReleaseUrl.sanitize("https://UPDATE.WORKSHOP.SKIN/latest.json"));
        eq("HTTPS://github.com/SAINTTaiYi/SpektraLab-Android/releases/tag/v1",
                ReleaseUrl.sanitize("HTTPS://github.com/SAINTTaiYi/SpektraLab-Android/releases/tag/v1"));

        System.out.println("ReleaseUrlCheck: " + checks + " checks passed");
    }

    private static void eq(String expected, String actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }
}

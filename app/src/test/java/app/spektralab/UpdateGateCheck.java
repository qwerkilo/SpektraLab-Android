package app.spektralab;

/**
 * Pure JVM contract for update eligibility.
 *
 * <p>The review that produced this check found the release gate was
 * documentation-only: build 69 sat in a `preview` announcement while 1.3.4
 * stable clients compared version codes and were offered it. Version alone is
 * not an offer; channel and endpoint are part of it.
 */
public final class UpdateGateCheck {
    private static int checks;

    public static void main(String[] args) {
        channels();
        eligibility();
        endpoints();

        System.out.println("UpdateGateCheck: " + checks + " checks passed");
    }

    private static void channels() {
        eq(UpdateGate.PREVIEW, UpdateGate.channelOf("preview"));
        eq(UpdateGate.PREVIEW, UpdateGate.channelOf(" PREVIEW "));
        eq(UpdateGate.STABLE, UpdateGate.channelOf("stable"));
        // Missing or unknown channels may not open the preview gate.
        eq(UpdateGate.STABLE, UpdateGate.channelOf(null));
        eq(UpdateGate.STABLE, UpdateGate.channelOf(""));
        eq(UpdateGate.STABLE, UpdateGate.channelOf("beta"));

        ok(UpdateGate.isPreview("1.4.0-m3"));
        ok(UpdateGate.isPreview("2.0.0-rc1"));
        ok(!UpdateGate.isPreview("1.3.4"));
        ok(!UpdateGate.isPreview("1.4.0"));
        ok(!UpdateGate.isPreview(null));
        ok(!UpdateGate.isPreview(""));
    }

    /** The gate itself: build 69 in `preview` is not offered to 1.3.4 stable. */
    private static void eligibility() {
        // A 1.3.4 stable client must not be offered build 69 from preview.
        ok(!UpdateGate.eligible(69, "preview", 68, "1.3.4"));
        // A stable client is offered a newer stable build.
        ok(UpdateGate.eligible(69, "stable", 68, "1.3.4"));
        // A preview build sees both channels.
        ok(UpdateGate.eligible(70, "preview", 69, "1.4.0-m3"));
        ok(UpdateGate.eligible(70, "stable", 69, "1.4.0-m3"));
        // A missing channel is stable, never a gate bypass.
        ok(UpdateGate.eligible(69, null, 68, "1.3.4"));
        ok(UpdateGate.eligible(69, null, 68, "1.4.0-m3"));
        // Same or older code is never an update, whatever the channel.
        ok(!UpdateGate.eligible(69, "preview", 69, "1.4.0-m3"));
        ok(!UpdateGate.eligible(68, "stable", 69, "1.4.0-m3"));
        ok(!UpdateGate.eligible(100, "preview", 69, "1.3.4"));
    }

    private static void endpoints() {
        // A stable client reads only the stable announcement.
        for (String url : UpdateGate.endpoints("1.3.4")) {
            ok("stable client reads " + url, !url.contains("preview.json"));
            ok(UpdateGate.isForkEndpoint(url));
        }
        // A preview client may read the preview announcement first.
        final String[] preview = UpdateGate.endpoints("1.4.0-m3");
        ok(preview.length > UpdateGate.endpoints("1.3.4").length);
        ok(preview[0].endsWith("/preview.json"));
        ok(UpdateGate.isForkEndpoint(preview[0]));

        // Never the Android upstream's repository or worker.
        for (String url : UpdateGate.endpoints("1.4.0-m3")) {
            ok("upstream endpoint " + url, !url.contains("SAINTTaiYi"));
            ok("upstream worker " + url, !url.contains("update.workshop.skin/latest.json"));
            ok(url.startsWith("https://"));
            ok(UpdateGate.isForkEndpoint(url));
        }
        ok(!UpdateGate.isForkEndpoint("https://github.com/SAINTTaiYi/SpektraLab-Android/"));
        ok(!UpdateGate.isForkEndpoint("https://evil.example/update.json"));
        ok(!UpdateGate.isForkEndpoint(null));
    }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void ok(String what, boolean value) {
        checks++;
        if (!value) throw new AssertionError(what);
    }

    private static void ok(boolean value) {
        ok("expected true", value);
    }
}

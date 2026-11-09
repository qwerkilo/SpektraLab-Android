package app.spektralab;

import app.spektralab.core.ReleaseUrl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure JVM contract for the fork's version and its two update channels.
 *
 * <p>The version is written by hand in the manifest, Gradle, and the channel
 * files, and they have drifted before. More importantly, the known engine
 * defect that loses the bottom rebate print above ~16.7 MP is a release
 * blocker: while `docs/known-issues.md` says the gate is open, the stable
 * announcement (`update.json`, the file 1.3.4 clients read) may not carry the
 * gated build. The preview build is announced in `preview.json`, which only a
 * channel-aware client reads.
 */
public final class VersionCheck {
    private static int checks;

    /** The fork's version. Not upstream's 100 / 1.4.0. */
    private static final int CODE = 326;
    private static final String NAME = "1.6.0e-m3";
    private static final String SITE = "https://spektralab.workshop.skin/";

    public static void main(String[] args) throws Exception {
        final Path root = Paths.get("").toAbsolutePath();
        final String manifest = read(root.resolve("app/src/main/AndroidManifest.xml"));
        final String gradle = read(root.resolve("app/build.gradle"));
        final String stable = read(root.resolve("update.json"));
        final String preview = read(root.resolve("preview.json"));
        final String issues = read(root.resolve("docs/known-issues.md"));

        final int manifestCode = intGroup(manifest, "versionCode=\"(\\d+)\"", "manifest versionCode");
        final String manifestName = group(manifest, "versionName=\"([^\"]+)\"", "manifest versionName");
        final int gradleCode = intGroup(gradle, "versionCode\\s+(\\d+)", "Gradle versionCode");
        final String gradleName = group(gradle, "versionName\\s+'([^']+)'", "Gradle versionName");

        eq(CODE, manifestCode);
        eq(NAME, manifestName);
        eq(CODE, gradleCode);
        eq(NAME, gradleName);

        final org.json.JSONObject previewJson = new org.json.JSONObject(preview);
        eq(CODE, previewJson.optInt("latest_version_code"));
        eq(NAME, previewJson.optString("latest_version_name"));
        eq(SITE, previewJson.optString("url"));
        eq(UpdateGate.PREVIEW, previewJson.optString("channel"));
        ok("the preview announcement is the fork's, not upstream's",
                SITE.equals(previewJson.optString("url")));
        ok("the preview notes name the fork build",
                previewJson.optJSONArray("notes") != null
                        && previewJson.optJSONArray("notes").toString().contains(NAME));
        ok("the preview build does not claim the upstream 1.4.0 release",
                !"1.4.0".equals(previewJson.optString("latest_version_name"))
                        && previewJson.optInt("latest_version_code") != 100);

        // The stable file is the one old clients read. It may not carry the
        // gated build while the gate is open.
        final org.json.JSONObject stableJson = new org.json.JSONObject(stable);
        eq(UpdateGate.STABLE, stableJson.optString("channel"));
        ok("the stable announcement has a code", stableJson.optInt("latest_version_code") > 0);
        final String stableUrl = stableJson.optString("url");
        ok("the stable URL is allowlisted",
                ReleaseUrl.DEFAULT.equals(stableUrl)
                        || !ReleaseUrl.DEFAULT.equals(ReleaseUrl.sanitize(stableUrl)));

        // The release gate is recorded next to the defect, and both channels
        // obey it.
        ok("known issues record the >16.7 MP rebate defect",
                issues.contains("16.7 MP") && issues.contains("16,776,960"));
        ok("known issues record the workaround",
                issues.contains("Workaround"));
        // The verdict is the bolded line, not the prose below it (which names
        // both spellings while explaining how to change it).
        final boolean gateOpen = issues.contains("**Release gate: open");
        final boolean gateAccepted = issues.contains("**Release gate: accepted");
        ok("known issues record a release-gate verdict", gateOpen != gateAccepted);

        if (gateOpen) {
            ok("the stable channel does not announce the gated build",
                    stableJson.optInt("latest_version_code") < CODE
                            || !NAME.equals(stableJson.optString("latest_version_name")));
            ok("the gated build is only on the preview channel",
                    CODE == previewJson.optInt("latest_version_code")
                            && UpdateGate.PREVIEW.equals(previewJson.optString("channel")));
            ok("a stable client is not offered the preview build",
                    !UpdateGate.eligible(previewJson.optInt("latest_version_code"),
                            previewJson.optString("channel"), 68, "1.3.4"));
        }
        if (gateAccepted) {
            ok("an accepted gate permits, but does not force, stable publication",
                    stableJson.optInt("latest_version_code") <= CODE);
        }

        System.out.println("VersionCheck: " + checks + " checks passed");
    }

    private static String read(Path path) throws Exception {
        ok("missing " + path, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static int intGroup(String text, String regex, String what) {
        return Integer.parseInt(group(text, regex, what));
    }

    private static String group(String text, String regex, String what) {
        final Matcher matcher = Pattern.compile(regex).matcher(text);
        ok("missing " + what, matcher.find());
        return matcher.group(1);
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
}

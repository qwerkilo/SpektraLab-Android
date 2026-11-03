package app.spektralab;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure JVM contract for the fork's version and its release gate.
 *
 * <p>The version is written in four places by hand (two in the manifest and
 * the Gradle default, one in the announcement the updater reads), and they
 * have drifted before. More importantly, the known engine defect that loses
 * the bottom rebate print above ~16.7 MP is a release blocker: while
 * `docs/known-issues.md` says the gate is open, `update.json` may not point
 * existing clients at a `stable` build.
 */
public final class VersionCheck {
    private static int checks;

    /** The fork's version. Not upstream's 100 / 1.4.0. */
    private static final int CODE = 69;
    private static final String NAME = "1.4.0-m3";
    private static final String SITE = "https://spektralab.workshop.skin/";

    public static void main(String[] args) throws Exception {
        final Path root = Paths.get("").toAbsolutePath();
        final String manifest = read(root.resolve("app/src/main/AndroidManifest.xml"));
        final String gradle = read(root.resolve("app/build.gradle"));
        final String update = read(root.resolve("update.json"));
        final String issues = read(root.resolve("docs/known-issues.md"));

        final int manifestCode = intGroup(manifest, "versionCode=\"(\\d+)\"", "manifest versionCode");
        final String manifestName = group(manifest, "versionName=\"([^\"]+)\"", "manifest versionName");
        final int gradleCode = intGroup(gradle, "versionCode\\s+(\\d+)", "Gradle versionCode");
        final String gradleName = group(gradle, "versionName\\s+'([^']+)'", "Gradle versionName");

        eq(CODE, manifestCode);
        eq(NAME, manifestName);
        eq(CODE, gradleCode);
        eq(NAME, gradleName);

        final org.json.JSONObject announcement = new org.json.JSONObject(update);
        eq(CODE, announcement.optInt("latest_version_code"));
        eq(NAME, announcement.optString("latest_version_name"));
        eq(SITE, announcement.optString("url"));
        ok("the fork does not claim the upstream 1.4.0 release",
                !"1.4.0".equals(announcement.optString("latest_version_name"))
                        && announcement.optInt("latest_version_code") != 100);
        ok("the announcement is the fork's, not upstream's",
                "https://spektralab.workshop.skin/".equals(announcement.optString("url")));
        ok("the announcement notes name the fork build",
                announcement.optJSONArray("notes") != null
                        && announcement.optJSONArray("notes").toString().contains(NAME));

        // The release gate is recorded next to the defect, and the update
        // channel obeys it.
        ok("known issues record the >16.7 MP rebate defect",
                issues.contains("16.7 MP") && issues.contains("16,776,960"));
        ok("known issues record the workaround",
                issues.contains("Workaround"));
        final boolean gateOpen = issues.contains("Release gate: open");
        ok("known issues record a release-gate verdict",
                gateOpen || issues.contains("Release gate: accepted"));
        if (gateOpen) {
            ok("a build with the open rebate defect is not announced as stable",
                    !"stable".equals(announcement.optString("channel")));
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

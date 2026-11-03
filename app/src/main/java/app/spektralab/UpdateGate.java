package app.spektralab;

import java.util.ArrayList;
import java.util.List;

/**
 * The update channel gate: which announcement a build may be offered, and
 * where the fork's announcements live.
 *
 * <p>Two independent facts decide an update. The version code must be newer —
 * that was the only rule before — **and** a stable build may not be offered a
 * preview-channel announcement. An announcement without a channel is stable,
 * because an unknown or missing field may not open the gate. The client's own
 * channel comes from its version name: a semver pre-release such as
 * `1.4.0-m3` opts into the preview channel; `1.3.4` is stable.
 *
 * <p>The endpoints are the fork's, not the Android upstream's. The upstream
 * repository publishes its own version code and release channel; reading it
 * would offer a fork client somebody else's build. The list is also the
 * provenance allowlist: only a body fetched from one of these URLs may be
 * parsed as an announcement.
 */
public final class UpdateGate {
    public static final String STABLE = "stable";
    public static final String PREVIEW = "preview";

    private static final String SITE = "https://spektralab.workshop.skin/";
    private static final String CDN =
            "https://cdn.jsdelivr.net/gh/qwerkilo/SpektraLab-Android@master/";
    private static final String RAW =
            "https://raw.githubusercontent.com/qwerkilo/SpektraLab-Android/master/";

    private UpdateGate() { }

    /**
     * The announcement URLs for a client, most specific first. A preview build
     * reads `preview.json` before the stable file; a stable build never reads
     * the preview file at all.
     */
    public static String[] endpoints(String clientVersionName) {
        final List<String> urls = new ArrayList<>();
        if (isPreview(clientVersionName)) {
            urls.add(SITE + "preview.json");
            urls.add(CDN + "preview.json");
            urls.add(RAW + "preview.json");
        }
        urls.add(SITE + "update.json");
        urls.add(CDN + "update.json");
        urls.add(RAW + "update.json");
        return urls.toArray(new String[0]);
    }

    /** Normalise an announcement's channel; anything unknown is stable. */
    public static String channelOf(String raw) {
        return raw != null && PREVIEW.equalsIgnoreCase(raw.trim()) ? PREVIEW : STABLE;
    }

    /** A semver pre-release (`1.4.0-m3`) opts into the preview channel. */
    public static boolean isPreview(String versionName) {
        return versionName != null && versionName.indexOf('-') > 0;
    }

    /**
     * True when this build may be told about that announcement: a newer code,
     * and a channel its own build accepts.
     */
    public static boolean eligible(int remoteCode, String remoteChannel,
                                   int clientCode, String clientVersionName) {
        if (remoteCode <= clientCode) return false;
        return isPreview(clientVersionName) || !PREVIEW.equals(channelOf(remoteChannel));
    }

    /** The fork's provenance allowlist for an announcement URL. */
    public static boolean isForkEndpoint(String url) {
        if (url == null) return false;
        for (String endpoint : endpoints("9.9.9-preview")) {
            if (endpoint.equals(url)) return true;
        }
        return false;
    }
}

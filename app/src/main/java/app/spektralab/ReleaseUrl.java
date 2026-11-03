package app.spektralab;

import java.net.URI;

/**
 * The only URLs an update announcement may open.
 *
 * <p>The announcement is remote data, so the URL is treated as untrusted. Keep
 * the browser on the fork's release site, its update host, or the fork's
 * GitHub release page; anything else falls back to the release site instead of
 * opening an arbitrary scheme or domain. The upstream Android repository is
 * not the fork's channel and is not accepted here; its GitHub path stays only
 * for an announcement the fork itself may still carry.
 */
public final class ReleaseUrl {
    /** The fork's release page. */
    public static final String DEFAULT = "https://spektralab.workshop.skin/";

    private ReleaseUrl() { }

    public static String sanitize(String raw) {
        if (raw == null) return DEFAULT;
        try {
            final URI uri = new URI(raw);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return DEFAULT;
            final String host = uri.getHost();
            if (host == null) return DEFAULT;
            if ("spektralab.workshop.skin".equalsIgnoreCase(host)) return raw;
            if ("update.workshop.skin".equalsIgnoreCase(host)) return raw;
            final String path = uri.getPath();
            if ("github.com".equalsIgnoreCase(host) && path != null) {
                if (path.startsWith("/SAINTTaiYi/SpektraLab-Android/")
                        || path.startsWith("/qwerkilo/SpektraLab-Android/")) {
                    return raw;
                }
            }
        } catch (Exception ignored) { }
        return DEFAULT;
    }
}

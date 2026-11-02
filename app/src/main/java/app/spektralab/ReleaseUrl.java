package app.spektralab;

import java.net.URI;

/**
 * The only URLs an update announcement may open.
 *
 * <p>The announcement is remote data, so the URL is treated as untrusted. Keep
 * the browser on the project's release page or its update host; anything else
 * falls back to the GitHub release page instead of opening an arbitrary scheme
 * or domain.
 */
public final class ReleaseUrl {
    public static final String DEFAULT =
            "https://github.com/SAINTTaiYi/SpektraLab-Android/releases/latest";

    private ReleaseUrl() { }

    public static String sanitize(String raw) {
        if (raw == null) return DEFAULT;
        try {
            final URI uri = new URI(raw);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return DEFAULT;
            final String host = uri.getHost();
            if (host == null) return DEFAULT;
            if ("update.workshop.skin".equalsIgnoreCase(host)) return raw;
            final String path = uri.getPath();
            if ("github.com".equalsIgnoreCase(host) && path != null
                    && path.startsWith("/SAINTTaiYi/SpektraLab-Android/")) {
                return raw;
            }
        } catch (Exception ignored) { }
        return DEFAULT;
    }
}

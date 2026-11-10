package app.spektralab.core;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The small, app-private document that restores the current photograph after a
 * process recreation.
 *
 * <p>It deliberately holds only the current source identity, the Look JSON, the
 * selected page and the Scan position. It never holds a Bitmap, an engine
 * handle, a URI grant token or a multi-frame history.
 */
public final class SessionDocument {
    public static final int VERSION = 1;
    private static final int MAX_BYTES = 256 * 1024;

    private final String uri;
    private final String name;
    private final boolean raw;
    private final int tab;
    private final float scanFraction;
    private final boolean scanVisible;
    private final boolean original;
    private final JSONObject look;

    public SessionDocument(String uri, String name, boolean raw, int tab,
                           float scanFraction, boolean scanVisible, boolean original,
                           JSONObject look) {
        this.uri = uri == null ? "" : uri;
        this.name = name == null ? "" : name;
        this.raw = raw;
        this.tab = Math.max(0, Math.min(3, tab));
        this.scanFraction = Math.max(0f, Math.min(1f, scanFraction));
        this.scanVisible = scanVisible;
        this.original = original;
        this.look = look == null ? new JSONObject() : look;
    }

    public String uri() {
        return uri;
    }

    public String name() {
        return name;
    }

    public boolean raw() {
        return raw;
    }

    public int tab() {
        return tab;
    }

    public float scanFraction() {
        return scanFraction;
    }

    public boolean scanVisible() {
        return scanVisible;
    }

    /** True when the user was looking at Original, not the developed Look. */
    public boolean original() {
        return original;
    }

    public JSONObject look() {
        return look;
    }

    public JSONObject toJson() {
        final JSONObject out = new JSONObject();
        try {
            out.put("version", VERSION);
            out.put("uri", uri);
            out.put("name", name);
            out.put("raw", raw);
            out.put("tab", tab);
            out.put("scan_fraction", (double) scanFraction);
            out.put("scan_visible", scanVisible);
            out.put("original", original);
            out.put("look", look);
        } catch (org.json.JSONException ignored) {
        }
        return out;
    }

    /**
     * Null when the document is missing a source, missing a Look object, or
     * carries a version this build does not understand.
     */
    public static SessionDocument fromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        if (o.has("version") && o.optInt("version", VERSION) != VERSION) {
            return null;
        }
        final String uri = o.optString("uri", "");
        final JSONObject look = o.optJSONObject("look");
        if (uri.isEmpty() || look == null) {
            return null;
        }
        return new SessionDocument(uri, o.optString("name", ""), o.optBoolean("raw", false),
                o.optInt("tab", 0), (float) o.optDouble("scan_fraction", 0.5),
                o.optBoolean("scan_visible", false), o.optBoolean("original", false), look);
    }

    public static File file(File dir) {
        return new File(dir, "console-session.json");
    }

    /** Read a bounded UTF-8 document; null on any parse or shape error. */
    public static SessionDocument read(File file) {
        if (file == null || !file.isFile() || file.length() <= 0 || file.length() > MAX_BYTES) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) file.length());
            final byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bytes.write(buf, 0, n);
                if (bytes.size() > MAX_BYTES) {
                    return null;
                }
            }
            return fromJson(new JSONObject(new String(bytes.toByteArray(),
                    StandardCharsets.UTF_8)));
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Replace the document with one complete write. Android's rename replaces
     * the old file atomically; the delete fallback keeps the test host working.
     */
    public void write(File file) throws java.io.IOException {
        if (file == null) {
            throw new java.io.IOException("no session file");
        }
        final File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            throw new java.io.IOException("cannot create " + dir);
        }
        final File tmp = new File(file.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(toJson().toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.getFD().sync();
        }
        if (tmp.renameTo(file)) {
            return;
        }
        if (file.exists() && file.delete() && tmp.renameTo(file)) {
            return;
        }
        tmp.delete();
        throw new java.io.IOException("cannot replace " + file);
    }
}

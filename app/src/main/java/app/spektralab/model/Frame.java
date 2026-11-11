package app.spektralab.model;

import android.graphics.Bitmap;
import android.net.Uri;

import app.spektralab.FrameLookSession;
import app.spektralab.Look;
import app.spektralab.core.ScanSample;

/**
 * One photograph opened in this session, kept so the strip under the canvas
 * can bring it back without another trip through the picker.
 *
 * <p>{@code thumb} is its own small bitmap, not a reference to the live
 * source; the list must not pin one full-size ARGB buffer per photograph.
 */
public final class Frame {
    private final Uri uri;
    private final boolean raw;
    private final String name;
    private final FrameLookSession session;
    private Bitmap thumb;
    /** Scan inspect belongs to the Frame it was moved on, not to the Look. */
    private float scanFraction = ScanSample.DEFAULT_FRACTION;
    private boolean scanVisible;
    /** Whether the Frame was last left on 原图 rather than developed. */
    private boolean original = true;

    public Frame(Uri uri, boolean raw, String name, Look initial) {
        this.uri = uri;
        this.raw = raw;
        this.name = name;
        this.session = new FrameLookSession(initial);
    }

    public Uri uri() {
        return uri;
    }

    public boolean raw() {
        return raw;
    }

    public String name() {
        return name;
    }

    public Bitmap thumb() {
        return thumb;
    }

    public void setThumb(Bitmap value) {
        thumb = value;
    }

    public FrameLookSession session() {
        return session;
    }

    public float scanFraction() {
        return scanFraction;
    }

    public void setScanFraction(float value) {
        scanFraction = value;
    }

    public boolean scanVisible() {
        return scanVisible;
    }

    public void setScanVisible(boolean value) {
        scanVisible = value;
    }

    public boolean original() {
        return original;
    }

    public void setOriginal(boolean value) {
        original = value;
    }
}

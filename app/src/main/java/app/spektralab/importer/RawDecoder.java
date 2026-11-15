package app.spektralab.importer;

import android.graphics.Bitmap;

import app.spektralab.DebugSink;
import app.spektralab.engine.SpektraLabEngine;

/** The decoded RAW frame as a canvas preview, independent of the Activity. */
public final class RawDecoder {

    private RawDecoder() {
    }

    /**
     * The RAW frame in working space, packed without a transfer function. A
     * camera JPEG is not a stand-in: it is the camera's rendering, not this
     * frame. Returns null when the session exports no source frame.
     */
    public static Bitmap preview(SpektraLabEngine.Session session) {
        Bitmap rawView = null;
        try {
            final int[] d = session.exportSource("live");
            if (d != null && d[0] > 0 && d[1] > 0) {
                final int rw = d[0];
                final int rh = d[1];
                final int[] px = new int[rw * rh];
                if (session.copyArgbRange(px, 0, rh) == rw * rh) {
                    rawView = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888);
                    rawView.setPixels(px, 0, rw, 0, 0, rw, rh);
                }
            }
            try {
                int probePixel = 0;
                if (rawView != null) {
                    final int[] one = new int[1];
                    rawView.getPixels(one, 0, 1, Math.max(0, rawView.getWidth() / 2),
                            Math.max(0, rawView.getHeight() / 2), 1, 1);
                    probePixel = one[0];
                }
                DebugSink.text("raw-view", rawView == null ? "export: empty"
                        : "export " + rawView.getWidth() + "x" + rawView.getHeight()
                        + " mid=" + Integer.toHexString(probePixel));
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            try {
                DebugSink.text("raw-view", "export failed: " + t);
            } catch (Throwable ignored) {
            }
        }
        return rawView;
    }
}

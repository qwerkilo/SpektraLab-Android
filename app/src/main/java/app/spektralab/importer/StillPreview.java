package app.spektralab.importer;

import android.graphics.Bitmap;

/**
 * The comparison preview's size and lifecycle: never recycle the source when
 * {@code createScaledBitmap} returns it unchanged, which used to leave
 * {@code CompareView} holding a recycled bitmap.
 */
public final class StillPreview {

    private StillPreview() {
    }

    /** Width/height for a source fitted inside {@code maxEdge}, never upscaled. */
    public static int[] fit(int width, int height, int maxEdge) {
        final float edge = Math.max(width, height);
        final float k = Math.min(1f, (float) maxEdge / edge);
        return new int[]{Math.max(1, Math.round(width * k)),
                Math.max(1, Math.round(height * k))};
    }

    /** Scale to the preview edge and recycle the source only when a copy was made. */
    public static Bitmap create(Bitmap source, int maxEdge) {
        final int[] size = fit(source.getWidth(), source.getHeight(), maxEdge);
        final Bitmap preview = Bitmap.createScaledBitmap(source, size[0], size[1], true);
        if (preview != source) {
            source.recycle();
        }
        return preview;
    }
}

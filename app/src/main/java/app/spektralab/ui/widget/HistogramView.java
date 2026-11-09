package app.spektralab.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import app.spektralab.core.Histogram;

/** A small additive RGB histogram over the displayed bitmap. */
public final class HistogramView extends View {

    private static final int[] CHANNELS = {0xCCE05A5A, 0xCC5FBF6A, 0xCC5B7BE0};

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int[][] bins;
    private int max;

    public HistogramView(Context context) {
        this(context, null);
    }

    public HistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        fill.setStyle(Paint.Style.FILL);
    }

    /** Replace the plot from a bitmap; null clears it. */
    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            bins = null;
            max = 0;
            invalidate();
            return;
        }
        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        final int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        bins = Histogram.of(pixels, w, h);
        max = Histogram.max(bins);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        fill.setColor(0x66000000);
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 8, 8, fill);
        if (bins == null || max <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        final float bar = getWidth() / (float) Histogram.BINS;
        for (int channel = 0; channel < bins.length; ++channel) {
            fill.setColor(CHANNELS[channel]);
            path.reset();
            path.moveTo(0, getHeight());
            for (int i = 0; i < Histogram.BINS; ++i) {
                final float height = getHeight() * bins[channel][i] / (float) max;
                path.lineTo(i * bar, getHeight() - height);
            }
            path.lineTo(getWidth(), getHeight());
            path.close();
            canvas.drawPath(path, fill);
        }
        fill.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1, fill);
    }
}

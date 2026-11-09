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
    /** Cap the sampled pixels so a live preview frame stays cheap to bin. */
    private static final int MAX_SAMPLES = 160_000;

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
        final int step = Math.max(1, (int) Math.ceil(
                Math.sqrt((double) w * h / MAX_SAMPLES)));
        final int sw = (w + step - 1) / step;
        final int sh = (h + step - 1) / step;
        final int[] sample = new int[sw * sh];
        final int[] row = new int[w];
        for (int sy = 0, y = 0; y < h; y += step, ++sy) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1);
            final int base = sy * sw;
            for (int sx = 0, x = 0; x < w; x += step, ++sx) {
                sample[base + sx] = row[x];
            }
        }
        bins = Histogram.of(sample, sw, sh);
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

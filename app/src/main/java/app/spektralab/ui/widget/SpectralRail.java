package app.spektralab.ui.widget;

import android.graphics.Typeface;
import android.view.View;
import app.spektralab.Palette;
import app.spektralab.core.SpectrumSamples;

/**
 * The Spectrum's spatial reference: 380–780 nm along the preview's right
 * edge. It is a scale, not a measurement -- no sample, look, or render is
 * read here.
 */
public class SpectralRail extends View {
    private static final int TICKS = 40;   // one per 10 nm
    private final android.graphics.Paint paint =
            new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final int lineColor, majorColor, textColor, shadowColor;
    private final float density;

    public SpectralRail(android.content.Context context, Palette palette) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        lineColor = palette.color(Palette.Role.GUIDE_LINE);
        majorColor = palette.color(Palette.Role.GUIDE_CORNER);
        textColor = palette.color(Palette.Role.STAGE_INK);
        shadowColor = palette.color(Palette.Role.STAGE_SHADOW);
        setContentDescription("光谱参考刻度，380 至 780 纳米，不是测量值");
    }

    private float dp(float v) {
        return v * density;
    }

    @Override protected void onDraw(android.graphics.Canvas canvas) {
        final float h = getHeight();
        final float pad = dp(14);
        paint.setStrokeWidth(Math.max(1f, density));
        paint.setStyle(android.graphics.Paint.Style.STROKE);

        paint.setColor(lineColor);
        canvas.drawLine(0.5f, 0, 0.5f, h, paint);

        for (int i = 0; i <= TICKS; ++i) {
            final int nm = SpectrumSamples.RAIL_MIN_NM + i * 10;
            final boolean major = nm % 50 == 0;
            final float y = pad + (h - 2f * pad) * i / (float) TICKS;
            paint.setColor(major ? majorColor : lineColor);
            canvas.drawLine(0.5f, y, dp(major ? 7 : 3), y, paint);
        }

        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setShadowLayer(dp(2), 0, dp(1), shadowColor);
        paint.setTextSize(dp(5.5f));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", Typeface.NORMAL));
        final float x = dp(8);
        canvas.drawText(String.valueOf(SpectrumSamples.RAIL_MIN_NM), x,
                pad + dp(2), paint);
        canvas.drawText(String.valueOf(SpectrumSamples.RAIL_MID_NM), x,
                h * 0.5f + dp(2), paint);
        canvas.drawText(String.valueOf(SpectrumSamples.RAIL_MAX_NM), x,
                h - pad + dp(2), paint);
        paint.clearShadowLayer();
    }
}

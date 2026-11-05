package app.spektralab.ui.widget;

import android.view.View;

/**
 * A diagonal arrow used by the sheet affordance: one shaft and two head
 * strokes, drawn at the current density.
 */
public class ArrowView extends View {
    private final android.graphics.Paint paint =
            new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final float stroke;

    public ArrowView(android.content.Context context, int color) {
        super(context);
        paint.setColor(color);
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        stroke = Math.max(1f, getResources().getDisplayMetrics().density * 1.1f);
        paint.setStrokeWidth(stroke);
    }

    @Override protected void onDraw(android.graphics.Canvas canvas) {
        final float w = getWidth(), h = getHeight();
        final float pad = stroke;
        canvas.drawLine(pad, h - pad, w - pad, pad, paint);
        canvas.drawLine(w - pad, pad, w - pad * 2f - w * 0.34f, pad, paint);
        canvas.drawLine(w - pad, pad, w - pad, pad * 2f + h * 0.34f, paint);
    }
}

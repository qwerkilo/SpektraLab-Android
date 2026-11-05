package app.spektralab.ui.widget;

import android.view.View;
import app.spektralab.Palette;

/** .frame-guides: a 9 % inset rule, a centre cross, four corner marks. */
public class GuideView extends View {
    private final android.graphics.Paint paint =
            new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final int mark;
    private final Palette palette;

    public GuideView(android.content.Context context, int markLength, Palette palette) {
        super(context);
        mark = markLength;
        this.palette = palette;
    }

    @Override protected void onDraw(android.graphics.Canvas canvas) {
        final int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        final float dx = w * 0.09f, dy = h * 0.09f;
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(palette.color(Palette.Role.GUIDE_LINE));
        canvas.drawRect(dx, dy, w - dx, h - dy, paint);
        paint.setColor(palette.color(Palette.Role.GUIDE_CENTER));
        canvas.drawLine(w / 2f, dy, w / 2f, h - dy, paint);
        canvas.drawLine(dx, h / 2f, w - dx, h / 2f, paint);
        paint.setColor(palette.color(Palette.Role.GUIDE_CORNER));
        final float L = mark;
        canvas.drawLine(dx, dy, dx + L, dy, paint);
        canvas.drawLine(dx, dy, dx, dy + L, paint);
        canvas.drawLine(w - dx, dy, w - dx - L, dy, paint);
        canvas.drawLine(w - dx, dy, w - dx, dy + L, paint);
        canvas.drawLine(dx, h - dy, dx + L, h - dy, paint);
        canvas.drawLine(dx, h - dy, dx, h - dy - L, paint);
        canvas.drawLine(w - dx, h - dy, w - dx - L, h - dy, paint);
        canvas.drawLine(w - dx, h - dy, w - dx, h - dy - L, paint);
    }
}

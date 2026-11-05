package app.spektralab.ui.widget;


/**
 * A flat colour layer used behind the edge strip. It paints only the bounds
 * the framework gives it; there is no image or gradient to allocate.
 */
public class EdgeBackdrop extends android.graphics.drawable.Drawable {
    private final int background;
    private final android.graphics.Paint paint = new android.graphics.Paint();
    public EdgeBackdrop(int background) {
        this.background = background;
    }
    @Override public void draw(android.graphics.Canvas canvas) {
        paint.setColor(background);
        canvas.drawRect(getBounds(), paint);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(android.graphics.ColorFilter cf) { }
    @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
}

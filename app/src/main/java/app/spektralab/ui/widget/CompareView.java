package app.spektralab.ui.widget;

import android.graphics.Bitmap;
import android.view.View;
import app.spektralab.M3Tokens;

/**
 * The stage's picture, and the two ways to see what the film did: hold it
 * to see the frame before, or drag the middle line to keep both in view.
 *
 * Each side is drawn fit-inside, in its own frame: a rebate makes the
 * "after" a different shape from the raw, and cropping one to match the
 * other would misrepresent what is being compared.
 */
public class CompareView extends View {
    private final android.graphics.Paint paint =
            new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
    private final android.graphics.RectF dst = new android.graphics.RectF();
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final float slop;
    private final float density;
    private final int dividerColor, handleColor, accentColor;
    private Bitmap before, after;
    private boolean holding, dragging;
    private float split, downX, downY;
    private boolean pinching;
    private float zoom = 1f, panX = 0f, panY = 0f;
    private float pinchStartSpan, pinchStartZoom, pinchStartPanX, pinchStartPanY;
    private float pinchStartCx, pinchStartCy;

    private final Runnable press = new Runnable() {
        @Override public void run() {
            if (dragging || after == null || before == null) {
                return;
            }
            holding = true;
            invalidate();
        }
    };

    public CompareView(android.content.Context context, int dividerColor,
                int handleColor, int accentColor) {
        super(context);
        this.dividerColor = dividerColor;
        this.handleColor = handleColor;
        this.accentColor = accentColor;
        density = getResources().getDisplayMetrics().density;
        slop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setBefore(Bitmap b) { before = b; invalidate(); }
    public void setAfter(Bitmap b) { after = b; invalidate(); }

    public void clear() {
        before = null;
        after = null;
        holding = false;
        dragging = false;
        split = 0f;
        pinching = false;
        zoom = 1f;
        panX = 0f;
        panY = 0f;
        invalidate();
    }

    /** True when both sides exist, so the divider and hold are meaningful. */
    public boolean canCompare() {
        return before != null && after != null;
    }

    /** A touch within 24 dp of an established divider belongs to CompareView. */
    public boolean dividerHit(float x) {
        return canCompare() && split > 0f
                && Math.abs(x - split * getWidth()) <= 24f * density;
    }

    /** Hand one gesture to the divider while Scan inspect is on top. */
    public boolean forwardTouch(android.view.MotionEvent event) {
        return onTouchEvent(event);
    }

    /** Scan inspect's long-press keeps the Original hold available. */
    public void beginHold() {
        if (!canCompare()) {
            return;
        }
        holding = true;
        invalidate();
    }

    public void endHold() {
        if (!holding) {
            return;
        }
        holding = false;
        invalidate();
    }


/** Apply an opacity to a colour without changing its RGB. */
private static int withOpacity(int color, float opacity) {
    return withAlpha(color, Math.round(opacity * 255f));
}

private static int withAlpha(int color, int alpha) {
    return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
}

@Override protected void onDraw(android.graphics.Canvas canvas) {
        final Bitmap base = after != null ? after : before;
        if (base == null) {
            return;
        }
        drawFit(canvas, base);
        if (before == null || after == null) {
            return;
        }
        if (holding) {
            drawFit(canvas, before);
        } else if (split > 0f) {
            final float x = split * getWidth();
            canvas.save();
            canvas.clipRect(0f, 0f, x, getHeight());
            drawFit(canvas, before);
            canvas.restore();
            drawDivider(canvas, x);
        }
    }

    private void drawFit(android.graphics.Canvas canvas, Bitmap bmp) {
        final float vw = getWidth(), vh = getHeight();
        if (vw <= 0 || vh <= 0 || bmp.getWidth() == 0) {
            return;
        }
        final float scale = Math.min(vw / bmp.getWidth(), vh / bmp.getHeight()) * zoom;
        final float w = bmp.getWidth() * scale, h = bmp.getHeight() * scale;
        final float left = (vw - w) / 2f + panX, top = (vh - h) / 2f + panY;
        dst.set(left, top, left + w, top + h);
        canvas.drawBitmap(bmp, null, dst, paint);
    }

    /** Current scale, for the stage readout; never triggers a render. */
    public float zoomLevel() {
        return zoom;
    }

    private static float spanOf(android.view.MotionEvent e) {
        if (e.getPointerCount() < 2) {
            return 0f;
        }
        return (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
    }

    private static float midXOf(android.view.MotionEvent e) {
        return (e.getX(0) + e.getX(1)) / 2f;
    }

    private static float midYOf(android.view.MotionEvent e) {
        return (e.getY(0) + e.getY(1)) / 2f;
    }

    private void drawDivider(android.graphics.Canvas canvas, float x) {
        final float r = 6f * density, cy = getHeight() / 2f;
        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setColor(dividerColor);
        canvas.drawRect(x - 0.5f, 0f, x + 0.5f, getHeight(), paint);
        paint.setColor(handleColor);
        canvas.drawRect(x - r, cy - r, x + r, cy + r, paint);
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, density));
        paint.setColor(accentColor);
        canvas.drawRect(x - r, cy - r, x + r, cy + r, paint);
        // The divider is chrome, not measured content: its drag/hold state
        // uses the M3 state-layer opacities over the control colour.
        final float state = dragging ? M3Tokens.StateLayer.DRAGGED.opacity
                : holding ? M3Tokens.StateLayer.PRESSED.opacity : 0f;
        if (state > 0f) {
            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setColor(withOpacity(accentColor, state));
            canvas.drawRect(x - r, cy - r, x + r, cy + r, paint);
        }
        paint.setStyle(android.graphics.Paint.Style.FILL);
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent e) {
        if (after == null || before == null) {
            return false;
        }
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                beginTouch(e);
                return true;
            case android.view.MotionEvent.ACTION_POINTER_DOWN:
                return beginPinch(e);
            case android.view.MotionEvent.ACTION_MOVE:
                return moveTouch(e);
            case android.view.MotionEvent.ACTION_POINTER_UP:
                return pinching || super.onTouchEvent(e);
            case android.view.MotionEvent.ACTION_UP:
                endTouch();
                return true;
            case android.view.MotionEvent.ACTION_CANCEL:
                cancelTouch();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void beginTouch(android.view.MotionEvent e) {
        downX = e.getX();
        downY = e.getY();
        dragging = false;
        holding = false;
        pinching = false;
        handler.postDelayed(press, android.view.ViewConfiguration.getLongPressTimeout());
    }

    private boolean beginPinch(android.view.MotionEvent e) {
        if (e.getPointerCount() < 2) {
            return super.onTouchEvent(e);
        }
        handler.removeCallbacks(press);
        dragging = false;
        holding = false;
        pinching = true;
        pinchStartSpan = Math.max(1f, spanOf(e));
        pinchStartZoom = zoom;
        pinchStartPanX = panX;
        pinchStartPanY = panY;
        pinchStartCx = midXOf(e);
        pinchStartCy = midYOf(e);
        return true;
    }

    private boolean moveTouch(android.view.MotionEvent e) {
        if (pinching) {
            return movePinch(e);
        }
        if (holding) {
            return true;
        }
        if (!dragging) {
            if (Math.abs(e.getX() - downX) < slop
                    && Math.abs(e.getY() - downY) < slop) {
                return true;
            }
            dragging = true;
            handler.removeCallbacks(press);
        }
        split = Math.max(0.02f, Math.min(0.98f,
                e.getX() / Math.max(1, getWidth())));
        invalidate();
        return true;
    }

    private boolean movePinch(android.view.MotionEvent e) {
        if (e.getPointerCount() < 2) {
            return true;
        }
        final float span = Math.max(1f, spanOf(e));
        zoom = Math.max(0.2f, Math.min(8f, pinchStartZoom * span / pinchStartSpan));
        panX = pinchStartPanX + (midXOf(e) - pinchStartCx);
        panY = pinchStartPanY + (midYOf(e) - pinchStartCy);
        invalidate();
        return true;
    }

    private void endTouch() {
        handler.removeCallbacks(press);
        pinching = false;
        if (holding) {
            holding = false;
            invalidate();
        } else if (!dragging && split > 0f) {
            // A tap closes it: the divider is a gesture, not a mode.
            split = 0f;
            invalidate();
        }
        dragging = false;
    }

    private void cancelTouch() {
        handler.removeCallbacks(press);
        pinching = false;
        holding = false;
        dragging = false;
        invalidate();
    }
}

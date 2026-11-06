package app.spektralab.ui.widget;

import android.graphics.Bitmap;
import android.view.View;
import app.spektralab.Palette;
import app.spektralab.core.ScanSample;

/**
 * Scan inspect's draggable line and its touch/keyboard surface.
 *
 * <p>It sits over CompareView only while the mode is on. A touch that
 * starts on an established compare divider is forwarded, and a long press
 * still asks CompareView for the Original hold, so the overlay never makes
 * the core review gesture unreachable. The fraction is reported to
 * accessibility as a 0–100 range with scroll and set-progress actions.
 */
public class ScanOverlayView extends View {
    public interface Listener {
        void onScanSample(ScanSample sample, float fraction);
    }

    private final android.graphics.Paint paint =
            new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path triangle = new android.graphics.Path();
    private final android.graphics.RectF image = new android.graphics.RectF();
    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final float density;
    private final float slop;
    private final CompareView compare;
    private final Listener listener;
    private final int accentColor, shadowColor;
    private Bitmap preview;
    private int[] row;
    private int rowWidth;
    private float fraction = ScanSample.DEFAULT_FRACTION;
    private float downX, downY;
    private boolean dragging, holding, forwarding;

    private final Runnable hold = new Runnable() {
        @Override public void run() {
            if (dragging || compare == null || !compare.canCompare()) {
                return;
            }
            holding = true;
            compare.beginHold();
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
    };

    public ScanOverlayView(android.content.Context context, Palette palette,
                    CompareView compare, Listener listener) {
        super(context);
        this.compare = compare;
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        slop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        accentColor = palette.color(Palette.Role.ACCENT);
        shadowColor = palette.color(Palette.Role.STAGE_SHADOW);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("扫描线，位置 50%");
    }

    private float dp(float v) {
        return v * density;
    }

    /** Cache the centre row once per bitmap; every move samples it. */
    public void setPreviewBitmap(Bitmap bitmap) {
        if (bitmap == preview) {
            if (bitmap != null) {
                notifySample();
            }
            return;
        }
        preview = bitmap;
        if (bitmap == null) {
            row = null;
            rowWidth = 0;
        } else {
            rowWidth = bitmap.getWidth();
            row = new int[rowWidth];
            bitmap.getPixels(row, 0, rowWidth, 0, bitmap.getHeight() / 2, rowWidth, 1);
        }
        invalidate();
        notifySample();
    }

    public void setScanVisible(boolean visible) {
        setVisibility(visible ? VISIBLE : GONE);
        if (!visible) {
            handler.removeCallbacks(hold);
            if (holding) {
                holding = false;
                if (compare != null) {
                    compare.endHold();
                }
            }
            dragging = false;
            forwarding = false;
        }
        invalidate();
    }

    public void setFraction(float value) {
        float next = Float.isNaN(value) ? ScanSample.DEFAULT_FRACTION : value;
        next = Math.max(0f, Math.min(1f, next));
        if (Math.abs(next - fraction) < 1e-5f) {
            notifySample();
            return;
        }
        fraction = next;
        invalidate();
        notifySample();
    }

    private void notifySample() {
        if (listener == null) {
            return;
        }
        listener.onScanSample(ScanSample.at(row, rowWidth, fraction), fraction);
    }

    private boolean imageRect() {
        final int vw = getWidth(), vh = getHeight();
        if (preview == null || vw <= 0 || vh <= 0) {
            return false;
        }
        final int bw = preview.getWidth(), bh = preview.getHeight();
        if (bw <= 0 || bh <= 0) {
            return false;
        }
        final float scale = Math.min(vw / (float) bw, vh / (float) bh);
        final float w = bw * scale, h = bh * scale;
        final float left = (vw - w) * 0.5f, top = (vh - h) * 0.5f;
        image.set(left, top, left + w, top + h);
        return true;
    }

    private float fractionAt(float x) {
        if (!imageRect() || image.width() <= 0f) {
            return fraction;
        }
        return (x - image.left) / image.width();
    }

    @Override protected void onDraw(android.graphics.Canvas canvas) {
        if (preview == null || !imageRect()) {
            return;
        }
        final float x = image.left + fraction * image.width();

        // A dark halo under the accent keeps the line readable on both a
        // bright print and a night scene, in either Theme mode.
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(shadowColor);
        paint.setAlpha(170);
        canvas.drawLine(x, image.top, x, image.bottom, paint);

        paint.setStrokeWidth(Math.max(1f, density));
        paint.setColor(accentColor);
        paint.setAlpha(255);
        canvas.drawLine(x, image.top, x, image.bottom, paint);

        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setColor(shadowColor);
        triangle.reset();
        triangle.moveTo(x, image.top);
        triangle.lineTo(x - dp(5), image.top + dp(9));
        triangle.lineTo(x + dp(5), image.top + dp(9));
        triangle.close();
        canvas.drawPath(triangle, paint);

        paint.setColor(accentColor);
        triangle.reset();
        triangle.moveTo(x, image.top);
        triangle.lineTo(x - dp(3), image.top + dp(6));
        triangle.lineTo(x + dp(3), image.top + dp(6));
        triangle.close();
        canvas.drawPath(triangle, paint);

        paint.setColor(shadowColor);
        triangle.reset();
        triangle.moveTo(x, image.bottom);
        triangle.lineTo(x - dp(5), image.bottom - dp(9));
        triangle.lineTo(x + dp(5), image.bottom - dp(9));
        triangle.close();
        canvas.drawPath(triangle, paint);

        paint.setColor(accentColor);
        triangle.reset();
        triangle.moveTo(x, image.bottom);
        triangle.lineTo(x - dp(3), image.bottom - dp(6));
        triangle.lineTo(x + dp(3), image.bottom - dp(6));
        triangle.close();
        canvas.drawPath(triangle, paint);
        paint.setAlpha(255);
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent event) {
        if (getVisibility() != VISIBLE || preview == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                requestFocus();
                downX = event.getX();
                downY = event.getY();
                dragging = false;
                holding = false;
                // Keep the compare divider draggable while Scan is on.
                if (compare != null && compare.dividerHit(downX)) {
                    forwarding = true;
                    return compare.forwardTouch(event);
                }
                forwarding = false;
                handler.postDelayed(hold, android.view.ViewConfiguration.getLongPressTimeout());
                return true;
            case android.view.MotionEvent.ACTION_MOVE:
                if (forwarding) {
                    return compare.forwardTouch(event);
                }
                if (holding) {
                    return true;
                }
                if (!dragging) {
                    if (Math.abs(event.getX() - downX) < slop
                            && Math.abs(event.getY() - downY) < slop) {
                        return true;
                    }
                    dragging = true;
                    handler.removeCallbacks(hold);
                }
                setFraction(fractionAt(event.getX()));
                return true;
            case android.view.MotionEvent.ACTION_UP:
                if (forwarding) {
                    forwarding = false;
                    return compare.forwardTouch(event);
                }
                handler.removeCallbacks(hold);
                if (holding) {
                    holding = false;
                    if (compare != null) {
                    compare.endHold();
                }
                } else if (!dragging) {
                    setFraction(fractionAt(event.getX()));
                }
                dragging = false;
                return true;
            case android.view.MotionEvent.ACTION_CANCEL:
                if (forwarding) {
                    forwarding = false;
                    compare.forwardTouch(event);
                    return true;
                }
                handler.removeCallbacks(hold);
                if (holding) {
                    holding = false;
                    if (compare != null) {
                    compare.endHold();
                }
                }
                dragging = false;
                return true;
            default:
                return true;
        }
    }

    /** Arrow, Home, and End handling shared by the view and the Activity. */
    public boolean handleKey(int keyCode, boolean shift) {
        if (getVisibility() != VISIBLE || preview == null) {
            return false;
        }
        final float step = shift ? 0.05f : 0.01f;
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                setFraction(fraction - step);
                return true;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                setFraction(fraction + step);
                return true;
            case android.view.KeyEvent.KEYCODE_MOVE_HOME:
                setFraction(0f);
                return true;
            case android.view.KeyEvent.KEYCODE_MOVE_END:
                setFraction(1f);
                return true;
            default:
                return false;
        }
    }

    @Override public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        return handleKey(keyCode, event.isShiftPressed()) || super.onKeyDown(keyCode, event);
    }

    @Override public CharSequence getAccessibilityClassName() {
        return android.widget.SeekBar.class.getName();
    }

    @Override public void onInitializeAccessibilityNodeInfo(
            android.view.accessibility.AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.SeekBar.class.getName());
        info.setRangeInfo(android.view.accessibility.AccessibilityNodeInfo.RangeInfo.obtain(
                android.view.accessibility.AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
                0f, 100f, fraction * 100f));
        info.setContentDescription("扫描线，位置 " + Math.round(fraction * 100f)
                + "%，左右方向键移动");
        if (getVisibility() == VISIBLE && preview != null) {
            info.addAction(android.view.accessibility.AccessibilityNodeInfo
                    .AccessibilityAction.ACTION_SCROLL_FORWARD);
            info.addAction(android.view.accessibility.AccessibilityNodeInfo
                    .AccessibilityAction.ACTION_SCROLL_BACKWARD);
            info.addAction(android.view.accessibility.AccessibilityNodeInfo
                    .AccessibilityAction.ACTION_SET_PROGRESS);
        }
    }

    @Override public boolean performAccessibilityAction(int action, android.os.Bundle args) {
        if (getVisibility() == VISIBLE && preview != null) {
            if (action == android.view.accessibility.AccessibilityNodeInfo
                    .ACTION_SCROLL_FORWARD) {
                setFraction(fraction + 0.01f);
                return true;
            }
            if (action == android.view.accessibility.AccessibilityNodeInfo
                    .ACTION_SCROLL_BACKWARD) {
                setFraction(fraction - 0.01f);
                return true;
            }
            if (action == android.view.accessibility.AccessibilityNodeInfo
                    .AccessibilityAction.ACTION_SET_PROGRESS.getId()) {
                final float target = args == null
                        ? fraction * 100f
                        : args.getFloat(android.view.accessibility.AccessibilityNodeInfo
                                .ACTION_ARGUMENT_PROGRESS_VALUE, fraction * 100f);
                setFraction(target / 100f);
                return true;
            }
        }
        return super.performAccessibilityAction(action, args);
    }

    @Override protected void onDetachedFromWindow() {
        handler.removeCallbacks(hold);
        super.onDetachedFromWindow();
    }
}

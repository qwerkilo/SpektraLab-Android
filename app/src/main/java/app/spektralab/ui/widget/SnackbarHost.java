package app.spektralab.ui.widget;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

/**
 * The M3 snackbar's parent. A CoordinatorLayout is what Snackbar expects
 * for anchoring and swipe behaviour; the width cap is the M3 600 dp
 * maximum, so a tablet gets a readable bar rather than a full-window strip.
 */
public final class SnackbarHost extends CoordinatorLayout {
    private final int maxWidthPx;

    public SnackbarHost(android.content.Context context) {
        super(context);
        maxWidthPx = Math.round(600f * context.getResources().getDisplayMetrics().density);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode != MeasureSpec.UNSPECIFIED) {
            final int width = Math.min(MeasureSpec.getSize(widthMeasureSpec), maxWidthPx);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}

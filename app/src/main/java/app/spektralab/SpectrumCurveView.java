package app.spektralab;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.google.android.material.color.MaterialColors;

/**
 * The film panel's Spectrum: the selected stock's shipped series across its
 * measured wavelengths. Three channels, one point per real sample. A gap ends
 * the line; nothing is interpolated, and no missing sample is drawn at zero.
 * A stock without samples says so and carries no line at all.
 *
 * <p>Material 3 has no plot component, so this view is product-owned. It sits
 * inside a {@code MaterialCardView}, which owns the surface, radius, and
 * outline. The control colours come from M3 roles; the caption, legend, and
 * wavelength endpoints use the label-small role. The spectral R/G/B stay the
 * measured Palette colours and never enter the scheme.
 */
final class SpectrumCurveView extends View {
    private static final M3Tokens.TypeRole LABEL_ROLE = M3Tokens.TypeRole.LABEL_SMALL;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int lineColor, mutedColor, scanColor;
    private final int[] seriesColors;
    private final float density;
    private final Typeface labelTypeface;
    private final float labelSizePx;
    private final float labelTracking;

    private SpectrumCurve curve = SpectrumCurve.unavailable();
    private String seriesLabel = "";
    private String emptyLabel = SpectrumCurve.UNAVAILABLE_LABEL;
    private boolean scanMarkVisible;
    private float scanMarkFraction = ScanSample.DEFAULT_FRACTION;
    private ValueAnimator revealAnimator;
    private float reveal = 1f;

    SpectrumCurveView(Context context, Palette palette, Typeface labelTypeface) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        lineColor = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOutlineVariant,
                palette.color(Palette.Role.LINE));
        mutedColor = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                palette.color(Palette.Role.MUTED_INK));
        scanColor = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorPrimary,
                palette.color(Palette.Role.ACCENT));
        seriesColors = new int[]{
                palette.color(Palette.Role.SPECTRUM_RED),
                palette.color(Palette.Role.SPECTRUM_GREEN),
                palette.color(Palette.Role.SPECTRUM_BLUE),
        };
        this.labelTypeface = labelTypeface == null ? Typeface.DEFAULT : labelTypeface;
        labelSizePx = sizePx(LABEL_ROLE);
        labelTracking = LABEL_ROLE.letterSpacingEm;
    }

    private float dp(float v) {
        return v * density;
    }

    private float sizePx(M3Tokens.TypeRole role) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                role.sizeSp, getResources().getDisplayMetrics());
    }

    /**
     * Show a curve. A new available curve reveals left to right once; the same
     * curve object (a render-state refresh) never replays the reveal, and a
     * zero duration shows the whole plot at once for reduced motion.
     */
    void setCurve(SpectrumCurve curve, String seriesLabel, String emptyLabel,
                  int durationMs) {
        final SpectrumCurve next = curve == null ? SpectrumCurve.unavailable() : curve;
        final boolean same = next == this.curve;
        this.curve = next;
        this.seriesLabel = seriesLabel == null ? "" : seriesLabel;
        this.emptyLabel = emptyLabel == null ? SpectrumCurve.UNAVAILABLE_LABEL : emptyLabel;

        if (same) {
            // A render-state refresh re-sends the same curve. Let a running
            // reveal finish, and keep a settled one at the full plot.
            if (revealAnimator != null && revealAnimator.isRunning()) {
                invalidate();
                return;
            }
            reveal = 1f;
            invalidate();
            return;
        }

        cancelReveal();
        if (durationMs > 0 && next.available()) {
            reveal = 0f;
            revealAnimator = ValueAnimator.ofFloat(0f, 1f);
            revealAnimator.setDuration(durationMs);
            revealAnimator.setInterpolator(new DecelerateInterpolator());
            revealAnimator.addUpdateListener(a -> {
                reveal = (float) a.getAnimatedValue();
                invalidate();
            });
            revealAnimator.start();
        } else {
            reveal = 1f;
        }
        invalidate();
    }

    /** Scan inspect's fraction on the same 0..1 axis the samples use. */
    void setScanMark(boolean visible, float fraction) {
        final float next = Float.isNaN(fraction) ? ScanSample.DEFAULT_FRACTION : fraction;
        final float clamped = Math.max(0f, Math.min(1f, next));
        if (scanMarkVisible == visible && Math.abs(scanMarkFraction - clamped) < 1e-5f) return;
        scanMarkVisible = visible;
        scanMarkFraction = clamped;
        invalidate();
    }

    private void cancelReveal() {
        if (revealAnimator == null) return;
        revealAnimator.cancel();
        revealAnimator.removeAllUpdateListeners();
        revealAnimator = null;
    }

    @Override protected void onDetachedFromWindow() {
        // A detached view must come back settled, not frozen mid-reveal.
        cancelReveal();
        reveal = 1f;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        final float w = getWidth(), h = getHeight();
        final float pad = dp(8);
        // The MaterialCardView behind this view owns the surface, its radius,
        // and its ripple; the plot draws only its own ink.
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(labelTypeface);
        paint.setTextSize(labelSizePx);
        paint.setLetterSpacing(labelTracking);
        paint.setColor(mutedColor);
        paint.setAlpha(255);

        final boolean available = curve.available();
        final String caption = available
                ? seriesLabel + " \u00b7 " + curve.sampleCount() + " SMP"
                : "SPECTRUM";
        final float captionBaseline = pad + labelSizePx;
        canvas.drawText(caption, pad, captionBaseline, paint);

        if (available) {
            float legendRight = w - pad;
            for (int channel = curve.channelCount() - 1; channel >= 0; --channel) {
                final String label = SpectrumCurve.channelLabel(channel);
                final float labelWidth = paint.measureText(label);
                paint.setColor(seriesColors[channel]);
                canvas.drawText(label, legendRight - labelWidth, captionBaseline, paint);
                legendRight -= labelWidth + dp(6);
            }
        }

        final float top = captionBaseline + dp(6);
        final float bottom = Math.max(top + dp(1), h - labelSizePx - dp(6));
        final float left = pad;
        final float right = w - pad;
        final float plotWidth = right - left;
        final float plotHeight = bottom - top;

        paint.setLetterSpacing(0);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, density * 0.5f));
        paint.setColor(lineColor);
        paint.setAlpha(64);
        for (int i = 0; i <= 3; ++i) {
            final float y = top + plotHeight * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }
        paint.setAlpha(255);

        if (!available) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(mutedColor);
            paint.setTypeface(labelTypeface);
            paint.setTextSize(labelSizePx);
            paint.setLetterSpacing(labelTracking);
            final float textWidth = paint.measureText(emptyLabel);
            canvas.drawText(emptyLabel, Math.max(pad, (w - textWidth) * 0.5f),
                    (top + bottom) * 0.5f + labelSizePx * 0.35f, paint);
            return;
        }

        paint.setStrokeWidth(dp(1.1f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        canvas.save();
        canvas.clipRect(left, top, left + plotWidth * reveal, bottom + dp(2));
        for (int channel = 0; channel < curve.channelCount(); ++channel) {
            paint.setColor(seriesColors[channel]);
            final int runs = curve.runCount(channel);
            for (int run = 0; run < runs; ++run) {
                final int length = curve.runLength(channel, run);
                path.reset();
                int points = 0;
                float lastX = left, lastY = bottom;
                for (int point = 0; point < length; ++point) {
                    final double xFraction = curve.fractionAtNm(
                            curve.nmAt(channel, run, point));
                    final double yFraction = curve.normalizedAt(channel, run, point);
                    if (Double.isNaN(xFraction) || Double.isNaN(yFraction)) continue;
                    final float x = left + (float) xFraction * plotWidth;
                    final float y = bottom - (float) yFraction * plotHeight;
                    lastX = x;
                    lastY = y;
                    if (points == 0) path.moveTo(x, y);
                    else path.lineTo(x, y);
                    points++;
                }
                if (points == 1) {
                    // A single measured point stays a dot: never a zero-length line.
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(lastX, lastY, dp(1.4f), paint);
                    paint.setStyle(Paint.Style.STROKE);
                } else if (points > 1) {
                    canvas.drawPath(path, paint);
                }
            }
        }
        canvas.restore();

        // A plotter head while the trace is being revealed. It disappears at
        // rest, so the finished curve carries no extra chrome.
        if (reveal < 1f) {
            final float head = left + reveal * plotWidth;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, density * 0.5f));
            paint.setColor(scanColor);
            paint.setAlpha(90);
            canvas.drawLine(head, top - dp(2), head, bottom + dp(2), paint);
            paint.setAlpha(255);
        }

        // Scan inspect's position, on the same fraction axis as the curve.
        // When Scan is off the Activity clears this mark.
        if (scanMarkVisible) {
            final float x = left + scanMarkFraction * plotWidth;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, density));
            paint.setColor(scanColor);
            paint.setAlpha(210);
            canvas.drawLine(x, top - dp(2), x, bottom + dp(2), paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, top - dp(2), dp(1.6f), paint);
            paint.setAlpha(255);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mutedColor);
        paint.setTypeface(labelTypeface);
        paint.setTextSize(labelSizePx);
        paint.setLetterSpacing(labelTracking);
        final String lo = formatNm(curve.minNm());
        final String hi = formatNm(curve.maxNm()) + " nm";
        final float axisBaseline = h - dp(4);
        canvas.drawText(lo, left, axisBaseline, paint);
        canvas.drawText(hi, right - paint.measureText(hi), axisBaseline, paint);
    }

    private static String formatNm(double nm) {
        return String.format(java.util.Locale.US, "%.0f", nm);
    }
}

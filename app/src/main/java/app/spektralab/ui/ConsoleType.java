package app.spektralab.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.util.TypedValue;
import android.widget.TextView;

import app.spektralab.M3Tokens;

/**
 * The console's brand typography: Inter for Latin, OPPO Sans for Chinese, and
 * the M3 type roles applied through one place.
 *
 * <p>It owns no console state; a panel builds a {@code TextView} through this
 * object instead of copying a size or a span rule.
 */
public final class ConsoleType {
    private final Activity activity;
    private final Typeface body;
    private final Typeface han;

    public ConsoleType(Activity activity, Typeface body, Typeface han) {
        this.activity = activity;
        this.body = body;
        this.han = han;
    }

    public TextView label(String text, float sp, int color, Typeface family) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (family != null) {
            view.setTypeface(family);
        }
        view.setIncludeFontPadding(false);
        return view;
    }

    public CharSequence mixedText(String text) {
        return mixedText(text, 400);
    }

    public CharSequence mixedText(String text, int weight) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int i = 0;
        while (i < text.length()) {
            final boolean cjk = M3Chrome.isCjk(text.charAt(i));
            int j = i + 1;
            while (j < text.length() && M3Chrome.isCjk(text.charAt(j)) == cjk) {
                ++j;
            }
            builder.setSpan(new TfSpan(cjk ? han : body, weight), i, j,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            i = j;
        }
        return builder;
    }

    /** Mixed Chinese/Latin runs with one M3 role: size, weight, tracking, line height. */
    public TextView mixedM3Label(String text, M3Tokens.TypeRole role, int color) {
        final TextView view = new TextView(activity);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, role.sizeSp);
        view.setTextColor(color);
        view.setText(mixedText(text, role.weight));
        view.setIncludeFontPadding(false);
        view.setLetterSpacing(role.letterSpacingEm);
        applyM3LineHeight(view, role);
        return view;
    }

    /** Text with one M3 type role: size, weight, tracking, line height, brand family. */
    public TextView m3Label(String text, M3Tokens.TypeRole role, int color, Typeface family) {
        final TextView view = label(text, role.sizeSp, color, family);
        applyTypeRole(view, role, family);
        return view;
    }

    /** Apply a role to an existing TextView, including a Material component's label. */
    public void applyTypeRole(TextView view, M3Tokens.TypeRole role, Typeface family) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, role.sizeSp);
        view.setTypeface(M3Chrome.weighted(family, role.weight));
        view.setLetterSpacing(role.letterSpacingEm);
        applyM3LineHeight(view, role);
    }

    /** A run with one family, for the wordmark's Inter Light. */
    public CharSequence typed(String text, Typeface family) {
        final android.text.SpannableString span = new android.text.SpannableString(text);
        span.setSpan(new TfSpan(family, 400), 0, span.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }

    /** Make each line exactly the role's M3 line height, not the font's default. */
    public void applyM3LineHeight(TextView view, M3Tokens.TypeRole role) {
        final android.graphics.Paint.FontMetrics metrics = view.getPaint().getFontMetrics();
        final float natural = metrics.descent - metrics.ascent;
        final float target = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, role.lineHeightSp,
                activity.getResources().getDisplayMetrics());
        view.setLineSpacing(Math.max(0f, target - natural), 1f);
    }

    /** A run keeps Latin on Inter and Chinese on OPPO Sans. */
    private static final class TfSpan extends MetricAffectingSpan {
        private final Typeface family;
        private final int weight;

        private TfSpan(Typeface family, int weight) {
            this.family = family;
            this.weight = weight;
        }

        @Override
        public void updateDrawState(TextPaint paint) {
            paint.setTypeface(M3Chrome.weighted(family, weight));
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            paint.setTypeface(M3Chrome.weighted(family, weight));
        }
    }
}

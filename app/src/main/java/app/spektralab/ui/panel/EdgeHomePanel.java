package app.spektralab.ui.panel;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.util.function.Consumer;
import java.util.function.Supplier;

import app.spektralab.M3Tokens;
import app.spektralab.Palette;
import app.spektralab.ui.ConsoleTheme;
import app.spektralab.ui.ConsoleType;

/**
 * The Edge landing page: one card per edge feature, with a live summary that
 * is repainted through the Activity's refresher list.
 */
public final class EdgeHomePanel {
    /** The summaries and page entries the home cards need. */
    public interface Host {
        String rebateSummary();

        String dateSummary();

        void onOpenRebate();

        void onOpenDate();
    }

    private final Activity activity;
    private final ConsoleType type;
    private final ConsoleTheme theme;
    private final Typeface tfBody;
    private final Typeface tfHan;
    private final Host host;
    private final Consumer<Runnable> refreshers;
    private final View head;

    private final LinearLayout root;

    public EdgeHomePanel(Activity activity, ConsoleType type, ConsoleTheme theme,
                         Typeface tfBody, Typeface tfHan, View head,
                         Host host, Consumer<Runnable> refreshers) {
        this.activity = activity;
        this.type = type;
        this.theme = theme;
        this.tfBody = tfBody;
        this.tfHan = tfHan;
        this.head = head;
        this.host = host;
        this.refreshers = refreshers;

        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(head, new LinearLayout.LayoutParams(-1, -2));
        root.addView(buildStrip(), new LinearLayout.LayoutParams(-1, -2));
        final TextView note = type.m3Label("片门与尺度联动；片边只在这张图可用的片门里选",
                M3Tokens.TypeRole.LABEL_SMALL, theme.onSurfaceVariant(), tfHan);
        final LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.topMargin = dp(8);
        root.addView(note, noteLp);
    }

    public View view() {
        return root;
    }

    private View buildStrip() {
        final HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        final LinearLayout strip = new LinearLayout(activity);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(0, dp(4), 0, dp(4));
        strip.addView(entryCard("片边  REBATE", host::rebateSummary,
                "打开片边设置", host::onOpenRebate), cardParams());
        strip.addView(entryCard("日期水印  DATE BACK", host::dateSummary,
                "打开日期水印设置", host::onOpenDate), cardParams());
        scroll.addView(strip, new FrameLayout.LayoutParams(-2, -2));
        return scroll;
    }

    private LinearLayout.LayoutParams cardParams() {
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(176), -2);
        lp.rightMargin = dp(8);
        return lp;
    }

    private MaterialCardView entryCard(String title, Supplier<String> summary,
                                       String description, Runnable onOpen) {
        final MaterialCardView card = new MaterialCardView(activity);
        card.setCardElevation(0f);
        card.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(theme.role(
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE_ELEVATED));
        card.setStrokeColor(theme.outlineVariant());
        card.setClickable(true);
        card.setFocusable(true);

        final LinearLayout stack = new LinearLayout(activity);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setPadding(dp(12), dp(10), dp(12), dp(10));

        final LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        final TextView name = type.mixedM3Label(title,
                M3Tokens.TypeRole.TITLE_SMALL, theme.onSurface());
        name.setMaxLines(2);
        titleRow.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        titleRow.addView(type.m3Label("›", M3Tokens.TypeRole.TITLE_SMALL,
                theme.onSurfaceVariant(), tfBody));
        stack.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        final TextView summaryView = type.m3Label(summary.get(),
                M3Tokens.TypeRole.LABEL_SMALL, theme.onSurfaceVariant(), tfHan);
        final LinearLayout.LayoutParams summaryLp = new LinearLayout.LayoutParams(-1, -2);
        summaryLp.topMargin = dp(6);
        stack.addView(summaryView, summaryLp);

        card.addView(stack, new FrameLayout.LayoutParams(-1, -2));
        card.setOnClickListener(v -> onOpen.run());
        refreshers.accept(() -> {
            final String text = summary.get();
            summaryView.setText(text);
            card.setContentDescription(description + "，" + text);
        });
        return card;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

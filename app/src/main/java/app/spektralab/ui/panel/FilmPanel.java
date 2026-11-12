package app.spektralab.ui.panel;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import app.spektralab.M3Tokens;
import app.spektralab.Palette;
import app.spektralab.ui.ConsoleTheme;
import app.spektralab.ui.ConsoleType;
import app.spektralab.ui.M3Chrome;
import app.spektralab.ui.widget.SpectrumCurveView;

/**
 * The film workspace: section head, brand facets, film shelf, spectral card,
 * measured row and paper choice. The panel builds the static tree; the
 * Activity still owns the catalogue, the Look and the refresh policy.
 */
public final class FilmPanel {
    /** The Activity-side actions the panel cannot own yet. */
    public interface Host {
        /** Open the paper picker. */
        void onPaperPicker();
    }

    private final Activity activity;
    private final ConsoleType type;
    private final ConsoleTheme theme;
    private final Palette palette;
    private final Typeface tfBody;
    private final Typeface tfHan;
    private final Host host;
    private final int cardHeightDp;
    private final int curveHeightDp;
    private final int metricsHeightDp;
    private final int paperHeightDp;
    private final int touchDp;

    private final LinearLayout root;
    private ChipGroup brandRow;
    private HorizontalScrollView chipScroll;
    private LinearLayout chipRow;
    private MaterialCardView curveCard;
    private SpectrumCurveView curve;
    private LinearLayout metricsRow;
    private TextView filmNote;
    private TextView paperValue;

    public FilmPanel(Activity activity, ConsoleType type, ConsoleTheme theme,
                     Palette palette, Typeface tfBody, Typeface tfHan, Host host,
                     View sectionHead, int cardHeightDp, int curveHeightDp,
                     int metricsHeightDp, int paperHeightDp, int touchDp) {
        this.activity = activity;
        this.type = type;
        this.theme = theme;
        this.palette = palette;
        this.tfBody = tfBody;
        this.tfHan = tfHan;
        this.host = host;
        this.cardHeightDp = cardHeightDp;
        this.curveHeightDp = curveHeightDp;
        this.metricsHeightDp = metricsHeightDp;
        this.paperHeightDp = paperHeightDp;
        this.touchDp = touchDp;

        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(sectionHead, new LinearLayout.LayoutParams(-1, -2));

        buildBrandRow();
        buildShelf();
        buildCurveCard();
        buildMetrics();
        buildPaperRow();
    }

    private void buildBrandRow() {
        brandRow = new ChipGroup(activity);
        brandRow.setSingleLine(true);
        brandRow.setSingleSelection(true);
        brandRow.setSelectionRequired(true);
        brandRow.setChipSpacingHorizontal(dp(4));
        brandRow.setChipSpacingVertical(0);
        final HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(false);
        scroll.addView(brandRow, new android.widget.FrameLayout.LayoutParams(-2, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, dp(touchDp)));
    }

    private void buildShelf() {
        chipRow = new LinearLayout(activity);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setGravity(Gravity.CENTER_VERTICAL);
        chipScroll = new HorizontalScrollView(activity);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setClipToPadding(false);
        chipScroll.setFillViewport(false);
        chipScroll.addView(chipRow, new android.widget.FrameLayout.LayoutParams(-2, -1));
        root.addView(chipScroll, new LinearLayout.LayoutParams(-1, dp(cardHeightDp)));
    }

    private void buildCurveCard() {
        curveCard = new MaterialCardView(activity);
        curveCard.setCardElevation(0f);
        curveCard.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
        curveCard.setCardBackgroundColor(theme.role(
                com.google.android.material.R.attr.colorSurfaceContainer,
                Palette.Role.SURFACE_ELEVATED));
        curveCard.setStrokeWidth(dp(1));
        curveCard.setStrokeColor(theme.role(
                com.google.android.material.R.attr.colorOutlineVariant,
                Palette.Role.LINE));
        curve = new SpectrumCurveView(activity, palette, tfBody);
        curveCard.addView(curve, new android.widget.FrameLayout.LayoutParams(-1, -1));
        root.addView(curveCard, new LinearLayout.LayoutParams(-1, dp(curveHeightDp)));
    }

    private void buildMetrics() {
        metricsRow = new LinearLayout(activity);
        metricsRow.setOrientation(LinearLayout.HORIZONTAL);
        metricsRow.setGravity(Gravity.CENTER_VERTICAL);
        metricsRow.setMinimumHeight(dp(metricsHeightDp));
        root.addView(metricsRow, new LinearLayout.LayoutParams(-1, -2));

        filmNote = type.m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                theme.onSurfaceVariant(), tfBody);
        filmNote.setSingleLine(false);
        filmNote.setMaxLines(2);
        filmNote.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(filmNote, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildPaperRow() {
        final LinearLayout paperRow = new LinearLayout(activity);
        paperRow.setOrientation(LinearLayout.HORIZONTAL);
        paperRow.setGravity(Gravity.CENTER_VERTICAL);
        paperRow.setMinimumHeight(dp(paperHeightDp));
        paperRow.addView(type.mixedM3Label("相纸 / 拷贝片",
                M3Tokens.TypeRole.LABEL_LARGE, theme.onSurfaceVariant()));
        final MaterialCardView paperChoice = new MaterialCardView(activity);
        paperChoice.setCardElevation(0f);
        paperChoice.setRadius(dp(M3Tokens.ShapeRole.SMALL.radiusDp));
        paperChoice.setCardBackgroundColor(theme.role(
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Palette.Role.SURFACE_ELEVATED));
        paperChoice.setStrokeWidth(0);
        paperChoice.setClickable(true);
        paperChoice.setFocusable(true);
        paperChoice.setMinimumHeight(dp(paperHeightDp));
        paperChoice.setContentDescription("选择相纸 / 拷贝片");
        paperChoice.setRippleColor(ColorStateList.valueOf(
                M3Chrome.withOpacity(theme.primary(), M3Tokens.StateLayer.PRESSED.opacity)));
        paperChoice.setOnClickListener(v -> host.onPaperPicker());
        final LinearLayout paperInner = new LinearLayout(activity);
        paperInner.setOrientation(LinearLayout.HORIZONTAL);
        paperInner.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        paperInner.setPadding(dp(12), 0, dp(10), 0);
        paperValue = type.m3Label("Kodak Portra Endura",
                M3Tokens.TypeRole.BODY_MEDIUM, theme.onSurface(), tfHan);
        paperValue.setSingleLine(true);
        paperValue.setEllipsize(TextUtils.TruncateAt.END);
        paperValue.setGravity(Gravity.END);
        paperInner.addView(paperValue, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView arrow = type.m3Label("↓", M3Tokens.TypeRole.LABEL_LARGE,
                theme.primary(), tfBody);
        final LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(-2, -2);
        arrowLp.leftMargin = dp(7);
        paperInner.addView(arrow, arrowLp);
        paperChoice.addView(paperInner, new android.widget.FrameLayout.LayoutParams(-1, -1));
        paperRow.addView(paperChoice, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(paperRow, new LinearLayout.LayoutParams(-1, -2));
    }

    public LinearLayout view() {
        return root;
    }

    public ChipGroup brandRow() {
        return brandRow;
    }

    public HorizontalScrollView chipScroll() {
        return chipScroll;
    }

    public LinearLayout chipRow() {
        return chipRow;
    }

    public MaterialCardView curveCard() {
        return curveCard;
    }

    public SpectrumCurveView curve() {
        return curve;
    }

    public LinearLayout metricsRow() {
        return metricsRow;
    }

    public TextView filmNote() {
        return filmNote;
    }

    public TextView paperValue() {
        return paperValue;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

package app.spektralab.ui.panel;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import app.spektralab.M3Tokens;
import app.spektralab.Palette;
import app.spektralab.core.ScanSample;
import app.spektralab.ui.ConsoleTheme;
import app.spektralab.ui.ConsoleType;
import app.spektralab.ui.M3Chrome;
import app.spektralab.ui.widget.ArrowView;
import app.spektralab.ui.widget.CompareView;
import app.spektralab.ui.widget.HistogramView;
import app.spektralab.ui.widget.ScanOverlayView;
import app.spektralab.ui.widget.SpectralRail;

/**
 * The Prism stage: the picture, its Compare and Scan layers, and every corner
 * readout. Construction lives here; the Activity still owns the Frame, the
 * render thread and the Look, and talks to the panel through {@link Host}.
 *
 * <p>Order inside the stage is load-bearing: Compare, then Scan, then the
 * corner readouts, the scan chip/card, the shelf handle (added by the caller)
 * and the progress bars last.
 */
public final class StagePanel {
    /** The stage's clearance below the floating top bar. */
    public static final int TOP_CLEARANCE_DP = 78;

    /** The Activity-side behaviour the stage cannot own yet. */
    public interface Host {
        /** The bitmap the stage is showing: the Original or the last preview. */
        Bitmap displayedBitmap();

        /** True when a source file is loaded; the Scan chip depends on it. */
        boolean sourcePresent();

        /** The bitmap Scan inspect may read, or null when there is none. */
        Bitmap scanBitmap();

        /** The import menu behind the empty-stage control. */
        void onImportMenu();

        /** Toggle Scan inspect. */
        void onToggleScan();

        /** A new sample from the scan line. */
        void onScanSample(ScanSample sample, float fraction);
    }

    private final Activity activity;
    private final ConsoleType type;
    private final ConsoleTheme theme;
    private final Palette palette;
    private final Typeface tfDisplay;
    private final Typeface tfBody;
    private final Typeface tfHan;
    private final Host host;
    private final boolean largeText;
    private final FrameLayout root;
    private final CompareView compare;
    private final ScanOverlayView scanOverlay;
    private final HistogramView histogram;
    private View emptyState;
    private View rawState;
    private TextView rawName;
    private View busyLabel;
    private TextView fileLabel;
    private TextView stageMeta;
    private TextView stageExposureValue;
    private TextView statusText;
    private SpectralRail spectralRail;
    private Chip scanToggleChip;
    private MaterialCardView scanReadout;
    private TextView scanReadoutRgb;
    private TextView scanReadoutLambda;
    private LinearProgressIndicator progressBar;
    private LinearProgressIndicator exportProgressBar;
    private boolean histogramOn;
    private boolean progressAdded;

    public StagePanel(Activity activity, ConsoleType type, ConsoleTheme theme,
                      Palette palette, Typeface tfDisplay, Typeface tfBody,
                      Typeface tfHan, Host host, boolean largeText) {
        this.activity = activity;
        this.type = type;
        this.theme = theme;
        this.palette = palette;
        this.tfDisplay = tfDisplay;
        this.tfBody = tfBody;
        this.tfHan = tfHan;
        this.host = host;
        this.largeText = largeText;

        root = new FrameLayout(activity);
        root.setBackgroundColor(palette.color(Palette.Role.STAGE));
        root.setClipChildren(false);
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                updateReadoutWidths());

        compare = new CompareView(activity,
                theme.role(com.google.android.material.R.attr.colorOutline, Palette.Role.LINE),
                theme.surface(), theme.primary());
        compare.setVisibility(View.GONE);
        root.addView(compare, new FrameLayout.LayoutParams(-1, -1));

        // Scan draws over the picture but under the corner readouts and the
        // shelf handle. It handles touches only while the mode is on; when it
        // is off, CompareView keeps the divider and the long-press hold.
        scanOverlay = new ScanOverlayView(activity, palette, compare,
                host::onScanSample);
        scanOverlay.setVisibility(View.GONE);
        root.addView(scanOverlay, new FrameLayout.LayoutParams(-1, -1));

        histogram = new HistogramView(activity);
        histogram.setContentDescription("RGB 直方图");
        histogram.setVisibility(histogramOn ? View.VISIBLE : View.GONE);
        if (histogramOn) {
            histogram.setBitmap(host.displayedBitmap());
        }

        addEmptyState();
        addRawState();
        addBusyLabel();
        addReadouts();
        addScanControls();
        final Bitmap scan = host.scanBitmap();
        syncScan(false, ScanSample.DEFAULT_FRACTION, scan, scan != null);
    }

    public FrameLayout frame() {
        return root;
    }

    public CompareView compare() {
        return compare;
    }

    public ScanOverlayView scanOverlay() {
        return scanOverlay;
    }

    public HistogramView histogram() {
        return histogram;
    }

    public View emptyState() {
        return emptyState;
    }

    public View rawState() {
        return rawState;
    }

    public TextView rawName() {
        return rawName;
    }

    public View busyLabel() {
        return busyLabel;
    }

    public TextView fileLabel() {
        return fileLabel;
    }

    public TextView stageMeta() {
        return stageMeta;
    }

    public TextView stageExposureValue() {
        return stageExposureValue;
    }

    public TextView statusText() {
        return statusText;
    }

    public SpectralRail spectralRail() {
        return spectralRail;
    }

    public Chip scanToggleChip() {
        return scanToggleChip;
    }

    public MaterialCardView scanReadout() {
        return scanReadout;
    }

    public TextView scanReadoutRgb() {
        return scanReadoutRgb;
    }

    public TextView scanReadoutLambda() {
        return scanReadoutLambda;
    }

    public LinearProgressIndicator progressBar() {
        return progressBar;
    }

    public LinearProgressIndicator exportProgressBar() {
        return exportProgressBar;
    }

    public boolean histogramOn() {
        return histogramOn;
    }

    /** Add the two thin progress bars after the caller's shelf handle. */
    public void addProgress() {
        if (progressAdded) {
            return;
        }
        progressAdded = true;
        final FrameLayout jobs = new FrameLayout(activity);
        progressBar = bar();
        jobs.addView(progressBar, new FrameLayout.LayoutParams(-1, dp(2)));
        exportProgressBar = bar();
        jobs.addView(exportProgressBar, new FrameLayout.LayoutParams(-1, dp(2)));
        root.addView(jobs, new FrameLayout.LayoutParams(-1, dp(2), Gravity.BOTTOM));
    }

    /** Keep TalkBack on the scan line after its toggle. */
    public void wireScanTraversal() {
        if (scanToggleChip == null) {
            return;
        }
        scanToggleChip.setId(View.generateViewId());
        scanOverlay.setId(View.generateViewId());
        scanOverlay.setAccessibilityTraversalAfter(scanToggleChip.getId());
    }

    public void updateHistogram(Bitmap bitmap) {
        if (histogramOn) {
            histogram.setBitmap(bitmap);
        }
    }

    public void toggleHistogram() {
        histogramOn = !histogramOn;
        histogram.setVisibility(histogramOn ? View.VISIBLE : View.GONE);
        if (histogramOn) {
            histogram.setBitmap(host.displayedBitmap());
        }
    }

    /** Put the overlay, its readout and the chip on one scan state. */
    public void syncScan(boolean visible, float fraction, Bitmap bitmap,
                         boolean toggleEnabled) {
        scanOverlay.setFraction(fraction);
        scanOverlay.setPreviewBitmap(bitmap);
        final boolean show = visible && bitmap != null;
        scanOverlay.setScanVisible(show);
        scanReadout.setVisibility(show ? View.VISIBLE : View.GONE);
        scanToggleChip.setChecked(visible);
        scanToggleChip.setEnabled(toggleEnabled);
        scanToggleChip.setAlpha(toggleEnabled ? 1f : M3Tokens.DISABLED_CONTENT_ALPHA);
        scanToggleChip.setVisibility(toggleEnabled || (visible && host.sourcePresent())
                ? View.VISIBLE : View.GONE);
        final String state;
        if (visible) {
            state = toggleEnabled ? "扫描检视已开启，点击关闭" : "扫描检视已开启，等待预览";
        } else {
            state = toggleEnabled ? "扫描检视，点击开启" : "扫描检视，需先导入照片";
        }
        scanToggleChip.setContentDescription(state);
    }

    private LinearProgressIndicator bar() {
        final LinearProgressIndicator bar = new LinearProgressIndicator(activity);
        bar.setIndeterminate(true);
        bar.setTrackThickness(dp(2));
        bar.setIndicatorColor(palette.color(Palette.Role.STAGE_INK));
        bar.setTrackColor(android.graphics.Color.TRANSPARENT);
        bar.setVisibility(View.GONE);
        return bar;
    }

    private void addEmptyState() {
        final LinearLayout empty = new LinearLayout(activity);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(16), dp(16), dp(16), dp(16));
        final LinearLayout load = new LinearLayout(activity);
        load.setOrientation(LinearLayout.HORIZONTAL);
        load.setGravity(Gravity.CENTER);
        load.setBackground(pressableFill());
        load.setPadding(dp(18), 0, dp(18), 0);
        load.setOnClickListener(v -> host.onImportMenu());
        if (largeText) {
            load.setTranslationY(dp(32));
        }
        load.addView(type.m3Label("选择照片",
                largeText ? M3Tokens.TypeRole.LABEL_MEDIUM : M3Tokens.TypeRole.LABEL_LARGE,
                theme.primary(), tfHan));
        final ArrowView arrow = new ArrowView(activity, palette.color(Palette.Role.ACCENT));
        final LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(dp(14), dp(14));
        arrowLp.leftMargin = dp(8);
        load.addView(arrow, arrowLp);
        empty.addView(load, new LinearLayout.LayoutParams(-2, dp(48)));
        root.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        emptyState = empty;
    }

    private android.graphics.drawable.Drawable pressableFill() {
        final android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();
        shape.setColor(palette.color(Palette.Role.SURFACE_ELEVATED));
        shape.setCornerRadius(dp(2));
        final int ripple = (palette.color(Palette.Role.ACCENT) & 0x00FFFFFF) | 0x33000000;
        return new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(ripple), shape, null);
    }

    private void addRawState() {
        final LinearLayout raw = new LinearLayout(activity);
        raw.setOrientation(LinearLayout.VERTICAL);
        raw.setGravity(Gravity.CENTER);
        raw.setPadding(dp(24), dp(16), dp(24), dp(16));
        raw.setVisibility(View.GONE);
        final TextView title = type.label("RAW LOADED", 34,
                palette.color(Palette.Role.STAGE_INK), tfDisplay);
        title.setLetterSpacing(-0.055f);
        raw.addView(title);
        final TextView name = type.label("", 9,
                palette.color(Palette.Role.STAGE_INK), tfHan);
        name.setAlpha(0.7f);
        name.setLineSpacing(dp(4), 1f);
        name.setGravity(Gravity.CENTER);
        final LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(-2, -2);
        nameLp.topMargin = dp(13);
        raw.addView(name, nameLp);
        root.addView(raw, new FrameLayout.LayoutParams(-1, -1));
        rawState = raw;
        rawName = name;
    }

    private void addBusyLabel() {
        final TextView busy = type.label("RENDERING", 18,
                palette.color(Palette.Role.STAGE_INK), tfDisplay);
        busy.setLetterSpacing(0.14f);
        busy.setGravity(Gravity.CENTER);
        busy.setBackgroundColor(palette.color(Palette.Role.STAGE_BUSY));
        busy.setVisibility(View.GONE);
        root.addView(busy, new FrameLayout.LayoutParams(-1, -1));
        busyLabel = busy;
    }

    private void addReadouts() {
        addScrims();
        if (largeText) {
            // The 2×2 table carries film, paper, exposure and spectrum; at
            // 2.0× the stage is too short for the file and exposure corners.
            addStatusAndRail();
            return;
        }
        addFileReadout();
        addExposureReadout();
        addStatusAndRail();
    }

    private void addScrims() {
        final View top = new View(activity);
        top.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                        palette.color(Palette.Role.STAGE_SCRIM_TOP),
                        palette.color(Palette.Role.STAGE_SCRIM_MID),
                        palette.color(Palette.Role.STAGE_SCRIM_END) }));
        top.setClickable(false);
        root.addView(top, new FrameLayout.LayoutParams(-1, dp(76), Gravity.TOP));

        final View bottom = new View(activity);
        bottom.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {
                        palette.color(Palette.Role.STAGE_SCRIM_TOP),
                        palette.color(Palette.Role.STAGE_SCRIM_MID),
                        palette.color(Palette.Role.STAGE_SCRIM_END) }));
        bottom.setClickable(false);
        root.addView(bottom, new FrameLayout.LayoutParams(-1, dp(68), Gravity.BOTTOM));
    }

    private void addFileReadout() {
        final LinearLayout info = new LinearLayout(activity);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setClickable(false);
        final TextView file = type.label("未选择照片", 6.5f,
                palette.color(Palette.Role.STAGE_INK), tfHan);
        file.setAlpha(0.82f);
        file.setSingleLine(true);
        file.setEllipsize(TextUtils.TruncateAt.END);
        file.setShadowLayer(dp(2), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        info.addView(file, new LinearLayout.LayoutParams(-1, -2));
        final TextView meta = type.label("NO FRAME", 5.5f,
                palette.color(Palette.Role.STAGE_INK), tfBody);
        meta.setAlpha(0.66f);
        meta.setLetterSpacing(0.12f);
        meta.setSingleLine(true);
        meta.setShadowLayer(dp(2), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        final LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
        metaLp.topMargin = dp(2);
        info.addView(meta, metaLp);
        final FrameLayout.LayoutParams infoLp = new FrameLayout.LayoutParams(
                -1, -2, Gravity.TOP | Gravity.START);
        infoLp.leftMargin = dp(14);
        infoLp.rightMargin = dp(122);
        infoLp.topMargin = dp(TOP_CLEARANCE_DP);
        root.addView(info, infoLp);
        fileLabel = file;
        stageMeta = meta;
    }

    private void addExposureReadout() {
        final LinearLayout exposure = new LinearLayout(activity);
        exposure.setOrientation(LinearLayout.VERTICAL);
        exposure.setGravity(Gravity.END);
        exposure.setClickable(false);
        final TextView caption = type.label("PRINT EXPOSURE", 5.5f,
                palette.color(Palette.Role.STAGE_INK), tfBody);
        caption.setAlpha(0.66f);
        caption.setLetterSpacing(0.12f);
        caption.setGravity(Gravity.END);
        caption.setShadowLayer(dp(2), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        exposure.addView(caption);
        final LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM | Gravity.END);
        final TextView value = type.label("1.00", 27,
                palette.color(Palette.Role.STAGE_INK), tfDisplay);
        value.setLetterSpacing(-0.05f);
        value.setSingleLine(true);
        value.setShadowLayer(dp(3), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        row.addView(value);
        final TextView ev = type.label("EV", 7,
                palette.color(Palette.Role.STAGE_INK), tfBody);
        ev.setAlpha(0.8f);
        final LinearLayout.LayoutParams evLp = new LinearLayout.LayoutParams(-2, -2);
        evLp.leftMargin = dp(4);
        evLp.bottomMargin = dp(3);
        row.addView(ev, evLp);
        exposure.addView(row);
        // The histogram lives inside the exposure block so it can never draw
        // over the EV value, at any font scale.
        final LinearLayout.LayoutParams histogramLp =
                new LinearLayout.LayoutParams(dp(132), dp(64));
        histogramLp.topMargin = dp(6);
        histogramLp.gravity = Gravity.END;
        exposure.addView(histogram, histogramLp);
        final FrameLayout.LayoutParams exposureLp = new FrameLayout.LayoutParams(
                -2, -2, Gravity.TOP | Gravity.END);
        exposureLp.rightMargin = dp(34);
        exposureLp.topMargin = dp(TOP_CLEARANCE_DP - 4);
        root.addView(exposure, exposureLp);
        stageExposureValue = value;
    }

    private void addStatusAndRail() {
        final TextView status = type.label("等待导入", 6.5f,
                palette.color(Palette.Role.STAGE_INK), tfHan);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        status.setGravity(Gravity.END);
        status.setAlpha(0.76f);
        status.setShadowLayer(dp(2), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        status.setMaxWidth(Math.round(activity.getResources().getConfiguration().screenWidthDp
                * 0.60f * activity.getResources().getDisplayMetrics().density));
        final FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(
                -2, -2, Gravity.BOTTOM | Gravity.END);
        statusLp.rightMargin = dp(34);
        statusLp.bottomMargin = dp(10);
        root.addView(status, statusLp);
        statusText = status;

        spectralRail = new SpectralRail(activity, palette);
        spectralRail.setBackgroundColor(palette.color(Palette.Role.STAGE_SCRIM_TOP));
        root.addView(spectralRail, new FrameLayout.LayoutParams(
                dp(26), -1, Gravity.END));
    }

    private void addScanControls() {
        addScanToggleChip();
        addScanReadoutCard();
        scanToggleChip.setChecked(false);
    }

    private void addScanToggleChip() {
        final int chipSurface = theme.role(
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE_ELEVATED);
        final int chipSelected = theme.role(
                com.google.android.material.R.attr.colorSecondaryContainer,
                Palette.Role.ACCENT_SOFT);
        final int chipText = theme.role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
        final int chipTextSelected = theme.role(
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                Palette.Role.ACCENT);
        final int chipOutline = theme.role(
                com.google.android.material.R.attr.colorOutlineVariant,
                Palette.Role.LINE);
        final Chip chip = new Chip(activity, null,
                com.google.android.material.R.attr.chipStyle);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipIconVisible(false);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setText(type.mixedText("SCAN · 扫描", M3Tokens.TypeRole.LABEL_LARGE.weight));
        type.applyTypeRole(chip, M3Tokens.TypeRole.LABEL_LARGE, tfHan);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setChipBackgroundColor(M3Chrome.checkedColors(chipSelected, chipSurface));
        chip.setChipStrokeWidth(Math.max(1, dp(1)));
        chip.setChipStrokeColor(M3Chrome.checkedColors(chipSelected, chipOutline));
        chip.setTextColor(M3Chrome.checkedColors(chipTextSelected, chipText));
        chip.setContentDescription("扫描检视，点击开启");
        chip.setOnClickListener(v -> host.onToggleScan());
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                -2, -2, Gravity.TOP | Gravity.START);
        lp.leftMargin = dp(14);
        lp.topMargin = dp(TOP_CLEARANCE_DP + 42);
        root.addView(chip, lp);
        scanToggleChip = chip;
    }

    private void addScanReadoutCard() {
        final int cardSurface = theme.role(
                com.google.android.material.R.attr.colorSurfaceContainer,
                Palette.Role.SURFACE_ELEVATED);
        final int cardText = theme.role(
                com.google.android.material.R.attr.colorOnSurface,
                Palette.Role.INK);
        final int cardMuted = theme.role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
        final MaterialCardView card = new MaterialCardView(activity, null,
                com.google.android.material.R.attr.materialCardViewFilledStyle);
        card.setCardBackgroundColor(cardSurface);
        card.setRadius(dp(M3Tokens.ShapeRole.SMALL.radiusDp));
        card.setCardElevation(0f);
        card.setUseCompatPadding(false);
        card.setClickable(false);
        card.setFocusable(false);
        card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        card.setVisibility(View.GONE);

        final LinearLayout block = new LinearLayout(activity);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(12), dp(7), dp(12), dp(8));
        card.addView(block, new FrameLayout.LayoutParams(-2, -2));

        final TextView caption = type.m3Label("SCAN INSPECT · CENTRE ROW",
                M3Tokens.TypeRole.LABEL_SMALL, cardMuted, tfBody);
        caption.setSingleLine(true);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        block.addView(caption, new LinearLayout.LayoutParams(-2, -2));

        final TextView rgb = type.m3Label("R \u2014  G \u2014  B \u2014",
                M3Tokens.TypeRole.BODY_SMALL, cardText, tfBody);
        rgb.setSingleLine(true);
        rgb.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        final LinearLayout.LayoutParams rgbLp = new LinearLayout.LayoutParams(-2, -2);
        rgbLp.topMargin = dp(3);
        block.addView(rgb, rgbLp);

        final TextView lambda = type.m3Label("\u03bb \u2014",
                M3Tokens.TypeRole.BODY_SMALL, cardText, tfDisplay);
        lambda.setSingleLine(true);
        lambda.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        block.addView(lambda, new LinearLayout.LayoutParams(-2, -2));

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                -2, -2, Gravity.BOTTOM | Gravity.START);
        lp.leftMargin = dp(14);
        // Clear of the 62 dp shelf handle and its 12 dp margin.
        lp.bottomMargin = dp(84);
        root.addView(card, lp);
        scanReadout = card;
        scanReadoutRgb = rgb;
        scanReadoutLambda = lambda;
    }

    private void updateReadoutWidths() {
        if (statusText == null || root.getWidth() == 0) {
            return;
        }
        final int stageWidth = root.getWidth();
        final int available = Math.max(dp(120), stageWidth - dp(96));
        statusText.setMaxWidth(Math.min(available, Math.round(stageWidth * 0.60f)));
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

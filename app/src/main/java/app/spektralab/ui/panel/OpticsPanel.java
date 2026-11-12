package app.spektralab.ui.panel;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;

import app.spektralab.M3Tokens;
import app.spektralab.OpticsDelta;
import app.spektralab.Palette;
import app.spektralab.ui.ConsoleTheme;
import app.spektralab.ui.ConsoleType;
import app.spektralab.ui.M3Chrome;

/**
 * The optics head, the 22-parameter strip and the active-parameter readout.
 * The single slider and the two choice rows are appended by the Activity,
 * which still owns the Look and the one-render-per-release policy.
 */
public final class OpticsPanel {
    /** The Activity-side state and navigation the panel cannot own yet. */
    public interface Host {
        OpticsDelta.Param activeParam();

        boolean unlocked();

        void onLocked();

        void onSelect(int index);

        void onReset();

        void onView();

        void onMeter();

        void onScanner();

        void onSliderValue(float value);

        void onSliderStart();

        void onSliderStop();

        void onCameraFilter();

        void onProtectionNorm();
    }

    /** Builds the Activity's M3 buttons so the panel keeps the exact style. */
    public interface Buttons {
        MaterialButton action(CharSequence text, String description, Runnable onClick);

        MaterialButton text(CharSequence text, String description, int color,
                            Runnable onClick);
    }

    private final Activity activity;
    private final ConsoleType type;
    private final ConsoleTheme theme;
    private final Typeface tfDisplay;
    private final Typeface tfBody;
    private final Typeface tfHan;
    private final int touchDp;
    private final boolean largeText;
    private final Host host;
    private final Buttons buttons;

    private final LinearLayout root;
    private final List<Chip> chips = new ArrayList<>();
    private HorizontalScrollView stripScroll;
    private TextView lockNote;
    private TextView name;
    private MaterialButton reset;
    private TextView value;
    private TextView unit;
    private TextView delta;
    private TextView range;
    private Slider slider;
    private MaterialButton filterButton;
    private MaterialButton normButton;

    public OpticsPanel(Activity activity, ConsoleType type, ConsoleTheme theme,
                       Typeface tfDisplay, Typeface tfBody,
                       Typeface tfHan, int touchDp, boolean largeText,
                       Host host, Buttons buttons) {
        this.activity = activity;
        this.type = type;
        this.theme = theme;
        this.tfDisplay = tfDisplay;
        this.tfBody = tfBody;
        this.tfHan = tfHan;
        this.touchDp = touchDp;
        this.largeText = largeText;
        this.host = host;
        this.buttons = buttons;

        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setOnClickListener(v -> {
            if (!host.unlocked()) {
                host.onLocked();
            }
        });
        root.addView(buildHead(), new LinearLayout.LayoutParams(-1, -2));
        root.addView(buildStrip(), new LinearLayout.LayoutParams(-1, -2));
        root.addView(buildReadout(), new LinearLayout.LayoutParams(-1, -2));
        buildSlider();
        filterButton = choiceRow("镜头滤镜 / CAMERA FILTER", host::onCameraFilter);
        normButton = choiceRow("保护基准 / PROTECTION NORM", host::onProtectionNorm);
    }

    public LinearLayout view() {
        return root;
    }

    public List<Chip> chips() {
        return chips;
    }

    public HorizontalScrollView stripScroll() {
        return stripScroll;
    }

    public TextView lockNote() {
        return lockNote;
    }

    public TextView name() {
        return name;
    }

    public MaterialButton reset() {
        return reset;
    }

    public TextView value() {
        return value;
    }

    public TextView unit() {
        return unit;
    }

    public TextView delta() {
        return delta;
    }

    public TextView range() {
        return range;
    }

    public Slider slider() {
        return slider;
    }

    public MaterialButton filterButton() {
        return filterButton;
    }

    public MaterialButton normButton() {
        return normButton;
    }

    private View buildHead() {
        final LinearLayout head = new LinearLayout(activity);
        head.setOrientation(largeText ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        final LinearLayout titles = new LinearLayout(activity);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER_VERTICAL);
        final TextView title = type.mixedM3Label("OPTICS / 光学调整",
                M3Tokens.TypeRole.TITLE_SMALL, theme.onSurface());
        titles.addView(title, new LinearLayout.LayoutParams(-1, -2));

        lockNote = type.m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                theme.onSurfaceVariant(), tfHan);
        lockNote.setSingleLine(true);
        lockNote.setEllipsize(TextUtils.TruncateAt.END);
        lockNote.setVisibility(View.GONE);
        titles.addView(lockNote, new LinearLayout.LayoutParams(-1, -2));
        head.addView(titles, largeText
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -2, 1f));

        final MaterialButton viewAction = buttons.action(type.mixedText("视图"),
                "打开视图调整：白平衡、亮度、颜色、分级", host::onView);
        final MaterialButton meterAction = buttons.action(type.mixedText("测光"),
                "选择相机测光方式", host::onMeter);
        final MaterialButton scanAction = buttons.action(type.mixedText("扫描仪"),
                "打开扫描页：SP-3000 模型预设与黑白校正", host::onScanner);
        if (largeText) {
            final LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            actions.addView(viewAction, new LinearLayout.LayoutParams(-2, dp(48)));
            actions.addView(meterAction, new LinearLayout.LayoutParams(-2, dp(48)));
            actions.addView(scanAction, new LinearLayout.LayoutParams(-2, dp(48)));
            final HorizontalScrollView scroll = new HorizontalScrollView(activity);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setClipToPadding(false);
            scroll.addView(actions, new android.widget.FrameLayout.LayoutParams(-2, dp(48)));
            head.addView(scroll, new LinearLayout.LayoutParams(-1, dp(48)));
        } else {
            head.addView(viewAction, new LinearLayout.LayoutParams(-2, dp(48)));
            head.addView(meterAction, new LinearLayout.LayoutParams(-2, dp(48)));
            head.addView(scanAction, new LinearLayout.LayoutParams(-2, dp(48)));
        }
        return head;
    }

    private View buildStrip() {
        final LinearLayout strip = new LinearLayout(activity);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < OpticsDelta.Param.values().length; ++i) {
            final int index = i;
            final OpticsDelta.Param spec = OpticsDelta.paramAt(i);
            final Chip chip = new Chip(activity);
            styleChip(activity, theme, chip);
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setSingleLine(true);
            chip.setText(type.mixedText(spec.glyph + "  " + spec.label));
            chip.setContentDescription(spec.label + "，参数 " + (i + 1) + " / "
                    + OpticsDelta.Param.values().length);
            chip.setMinimumHeight(dp(touchDp));
            chip.setOnClickListener(v -> {
                if (!host.unlocked()) {
                    host.onLocked();
                    return;
                }
                host.onSelect(index);
            });
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(6);
            strip.addView(chip, lp);
            chips.add(chip);
        }
        final HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(false);
        scroll.addView(strip, new android.widget.FrameLayout.LayoutParams(-2, -2));
        stripScroll = scroll;
        return scroll;
    }

    private View buildReadout() {
        final LinearLayout block = new LinearLayout(activity);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(6), 0, 0);

        final LinearLayout top = new LinearLayout(activity);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        name = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL,
                theme.onSurface(), tfHan);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        ViewCompat.setAccessibilityHeading(name, true);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        reset = buttons.text("重置", "重置当前参数", theme.onSurfaceVariant(),
                host::onReset);
        top.addView(reset, new LinearLayout.LayoutParams(-2, dp(48)));
        block.addView(top, new LinearLayout.LayoutParams(-1, -2));

        final LinearLayout values = new LinearLayout(activity);
        values.setOrientation(LinearLayout.HORIZONTAL);
        values.setGravity(Gravity.BOTTOM);
        value = type.m3Label("", M3Tokens.TypeRole.HEADLINE_SMALL,
                theme.primary(), tfDisplay);
        values.addView(value);
        unit = type.m3Label("", M3Tokens.TypeRole.LABEL_MEDIUM,
                theme.onSurfaceVariant(), tfBody);
        final LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(-2, -2);
        unitLp.leftMargin = dp(4);
        unitLp.bottomMargin = dp(4);
        values.addView(unit, unitLp);
        delta = type.m3Label("", M3Tokens.TypeRole.LABEL_MEDIUM,
                theme.onSurfaceVariant(), tfDisplay);
        final LinearLayout.LayoutParams deltaLp = new LinearLayout.LayoutParams(-2, -2);
        deltaLp.leftMargin = dp(10);
        deltaLp.bottomMargin = dp(4);
        values.addView(delta, deltaLp);
        block.addView(values, new LinearLayout.LayoutParams(-1, -2));

        range = type.m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                theme.onSurfaceVariant(), tfHan);
        range.setSingleLine(false);
        block.addView(range, new LinearLayout.LayoutParams(-1, -2));
        return block;
    }

    private void buildSlider() {
        slider = new Slider(activity);
        slider.setId(View.generateViewId());
        slider.setMinimumHeight(dp(48));
        // The head already shows the live value; a floating tooltip would be
        // clipped by the row and repeat it.
        slider.setLabelBehavior(LabelFormatter.LABEL_GONE);
        slider.setLabelFormatter(value -> {
            final OpticsDelta.Param spec = host.activeParam();
            return OpticsDelta.valueText(value) + " " + spec.unit;
        });
        slider.addOnChangeListener((s, value, fromUser) -> host.onSliderValue(value));
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(Slider s) {
                host.onSliderStart();
            }

            @Override public void onStopTrackingTouch(Slider s) {
                host.onSliderStop();
            }
        });
        root.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private MaterialButton choiceRow(String title, Runnable open) {
        final boolean stacked = largeText;
        final LinearLayout row = new LinearLayout(activity);
        row.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(touchDp));
        final TextView titleView = type.m3Label(title, M3Tokens.TypeRole.LABEL_MEDIUM,
                theme.onSurfaceVariant(), tfHan);
        row.addView(titleView, stacked
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -2, 1f));
        final MaterialButton button = buttons.text("", title, theme.primary(), open);
        button.setGravity(stacked
                ? (Gravity.START | Gravity.CENTER_VERTICAL)
                : (Gravity.END | Gravity.CENTER_VERTICAL));
        button.setSingleLine(false);
        button.setMaxLines(2);
        row.addView(button, stacked
                ? new LinearLayout.LayoutParams(-1, dp(touchDp))
                : new LinearLayout.LayoutParams(-2, dp(touchDp)));
        root.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    /** One filter-chip style from the current theme; the sheets share it. */
    public static void styleChip(Activity activity, ConsoleTheme theme, Chip chip) {
        final int density = Math.round(activity.getResources().getDisplayMetrics().density);
        chip.setChipDrawable(ChipDrawable.createFromAttributes(activity, null, 0,
                com.google.android.material.R.style.Widget_Material3_Chip_Filter));
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
        chip.setChipBackgroundColor(M3Chrome.checkedColors(chipSelected, chipSurface));
        chip.setChipStrokeWidth(Math.max(1, density));
        chip.setChipStrokeColor(M3Chrome.checkedColors(chipSelected, chipOutline));
        chip.setTextColor(M3Chrome.checkedColors(chipTextSelected, chipText));
        chip.setEnsureMinTouchTargetSize(true);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

package app.spektralab.ui.panel;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import app.spektralab.ControlChoice;
import app.spektralab.DateBack;
import app.spektralab.EdgeLookAccess;
import app.spektralab.GateScale;
import app.spektralab.M3Tokens;
import app.spektralab.Palette;
import app.spektralab.RebateControls;
import app.spektralab.ui.ConsoleTheme;
import app.spektralab.ui.ConsoleType;

/**
 * The two Edge secondary pages: the film rebate and the date back.
 *
 * <p>The panel owns both page bodies, their widgets and their sync. The
 * Activity keeps the Look, the shared switch row, the dialogs and the
 * one-render commit; the panel adds its row refreshers to the Activity's list
 * so the landing cards and the open page repaint in the same pass.
 */
public final class EdgePagesPanel {
    /** The live Look, page actions and page chrome the pages need. */
    public interface Host {
        /** The current Frame's live Look; the panel never keeps a copy. */
        EdgeLookAccess look();

        /** Leave a secondary page and return to the two entry cards. */
        void onBack();

        /** The captured Gate card label, including the empty state. */
        String rebateGateLabel();

        /** Open the Gate picker sheet. */
        void showEdgePicker();

        /** Whether this Frame's shape can carry any shipped Gate. */
        boolean canHostRebate();

        /** A custom imprint, or the Frame's own EXIF date. */
        String effectiveDateText();

        /** The date detail line: the typed text, or the Frame's EXIF date. */
        String imprintDate();

        void askText(String title, String initial, Consumer<String> onValue);

        void askSeed(String title, int initial, IntConsumer onValue);

        void showChoiceSheet(String title, String note, List<String> labels,
                             int selectedIndex, IntConsumer onPick);

        void showSnackbar(String message);

        void requestLivePreview();

        void commitRebate();

        /** The Optics/Edge commit delay, so both gestures share one value. */
        long commitDelayMs();

        /** True when the page head stacks at the larger font scale. */
        boolean largeText();
    }

    /** The Activity's shared controls, so the pages keep their exact style. */
    public interface Rows {
        View switchRow(String labelText, TextView detail, MaterialSwitch toggle,
                       BooleanSupplier current, Consumer<Boolean> onChange);

        String toggleDescription(String labelText, boolean on);

        void setSyncingToggles(boolean syncing);

        MaterialButton textButton(CharSequence text, String description, int color,
                                  Runnable action);
    }

    private final Activity activity;
    private final ConsoleType type;
    private final ConsoleTheme theme;
    private final Typeface tfBody;
    private final Typeface tfHan;
    private final Host host;
    private final Rows rows;
    private final Consumer<Runnable> refreshers;
    private final Runnable refreshAll;

    private final LinearLayout rebatePage;
    private final LinearLayout datePage;
    private LinearLayout rebateControls;
    private LinearLayout dateControls;

    private MaterialSwitch rebateSwitch, edgeTextSwitch;
    private MaterialCardView rebateGateCard;
    private TextView rebateGateValue, rebateModeValue, rebateGateStyleValue;
    private TextView rebateHolesValue, rebateCarrierValue;
    private TextView rebateCameraSeedValue, rebateFrameSeedValue;
    private TextView rebateFogValue, rebateLeaksValue, rebateFrameNoValue, rebateFNumberValue;
    private Slider rebateFogSlider, rebateLeaksSlider, rebateFrameNoSlider, rebateFNumberSlider;

    private MaterialSwitch dateSwitch;
    private TextView dateDetail;
    private TextView dateTextValue, datePlacementValue, dateCornerValue, dateStyleValue;
    private TextView dateDataValue, dateTextBValue;
    private TextView dateInsetXValue, dateInsetYValue, dateSizeValue, dateEvValue;
    private Slider dateInsetXSlider, dateInsetYSlider, dateSizeSlider, dateEvSlider;

    /** True while a Look restore fills the pages; their listeners must not render. */
    private boolean syncing = false;

    private final Runnable rebateCommit;

    public EdgePagesPanel(Activity activity, ConsoleType type, ConsoleTheme theme,
                          Typeface tfBody, Typeface tfHan, Host host, Rows rows,
                          Consumer<Runnable> refreshers, Runnable refreshAll) {
        this.activity = activity;
        this.type = type;
        this.theme = theme;
        this.tfBody = tfBody;
        this.tfHan = tfHan;
        this.host = host;
        this.rows = rows;
        this.refreshers = refreshers;
        this.refreshAll = refreshAll;
        this.rebateCommit = () -> {
            if (!syncing) {
                host.commitRebate();
            }
        };

        rebatePage = buildRebatePage();
        datePage = buildDatePage();
    }

    public View rebatePage() {
        return rebatePage;
    }

    public View datePage() {
        return datePage;
    }

    /**
     * The Rebate secondary page. Its switch owns whether the controls are
     * enabled; they stay visible and dimmed while off, so the page explains
     * what is available without pretending the controls work.
     */
    private LinearLayout buildRebatePage() {
        final LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setVisibility(View.GONE);
        page.addView(pageHead("片边 / REBATE"), new LinearLayout.LayoutParams(-1, -2));

        final int onSurface = theme.role(
                com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK);
        final int onSurfaceVariant = theme.role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);

        rebateSwitch = new MaterialSwitch(activity);
        page.addView(rows.switchRow("开启片边", null, rebateSwitch,
                () -> host.look().overscanOn(), on -> {
                    if (on && !host.canHostRebate()) {
                        host.showSnackbar("这张图的长宽比不对应任何片门");
                        syncRebate();
                        return;
                    }
                    host.look().overscanOn(on);
                    syncRebate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, dp(56)));

        rebateControls = new LinearLayout(activity);
        rebateControls.setOrientation(LinearLayout.VERTICAL);

        addRebateGateCard(onSurface, onSurfaceVariant);
        addRebateToneControls(onSurface);
        addRebateStructureControls(onSurface);
        page.addView(rebateControls, new LinearLayout.LayoutParams(-1, -2));
        return page;
    }

    /** The rebate gate card and its live label. */
    private void addRebateGateCard(int onSurface, int onSurfaceVariant) {
        rebateGateValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateGateCard = new MaterialCardView(activity);
        rebateGateCard.setCardElevation(0f);
        rebateGateCard.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
        rebateGateCard.setStrokeWidth(dp(1));
        rebateGateCard.setCardBackgroundColor(theme.role(
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE_ELEVATED));
        rebateGateCard.setStrokeColor(theme.outlineVariant());
        rebateGateCard.setClickable(true);
        rebateGateCard.setFocusable(true);
        rebateGateCard.setOnClickListener(v -> host.showEdgePicker());
        final LinearLayout gateRow = new LinearLayout(activity);
        gateRow.setOrientation(LinearLayout.HORIZONTAL);
        gateRow.setGravity(Gravity.CENTER_VERTICAL);
        gateRow.setPadding(dp(14), 0, dp(14), 0);
        gateRow.addView(type.m3Label("片门格式", M3Tokens.TypeRole.BODY_MEDIUM,
                onSurfaceVariant, tfHan), new LinearLayout.LayoutParams(0, -2, 1f));
        rebateGateValue.setGravity(Gravity.END);
        gateRow.addView(rebateGateValue, new LinearLayout.LayoutParams(-2, -2));
        final TextView gateArrow = type.m3Label("↓", M3Tokens.TypeRole.LABEL_LARGE,
                onSurfaceVariant, tfBody);
        final LinearLayout.LayoutParams gateArrowLp = new LinearLayout.LayoutParams(-2, -2);
        gateArrowLp.leftMargin = dp(7);
        gateRow.addView(gateArrow, gateArrowLp);
        rebateGateCard.addView(gateRow, new FrameLayout.LayoutParams(-1, -1));
        refreshers.accept(() -> {
            final String label = host.rebateGateLabel();
            rebateGateValue.setText(label);
            rebateGateCard.setContentDescription("片门格式，" + label);
        });
        final LinearLayout.LayoutParams gateLp = new LinearLayout.LayoutParams(-1, dp(52));
        gateLp.bottomMargin = dp(4);
        rebateControls.addView(rebateGateCard, gateLp);
    }

    /** The rebate switches and tone sliders. */
    private void addRebateToneControls(int onSurface) {
        edgeTextSwitch = new MaterialSwitch(activity);
        rebateControls.addView(rows.switchRow("片边文字（厂标 / 张数）", null,
                edgeTextSwitch, () -> host.look().edgeTextOn(), on -> {
                    host.look().edgeTextOn(on);
                    syncRebate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, dp(56)));

        rebateFogValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateFogSlider = rebateSliderRow(rebateControls, "片基雾",
                "片基本身的灰雾。0 是完全透明，越大越脏",
                RebateControls.FOG_MIN, RebateControls.FOG_MAX, 0.05f,
                RebateControls.clampFog(host.look().edgeFog()),
                value -> String.format(Locale.US, "%.2f", value),
                value -> host.look().edgeFog(RebateControls.clampFog(value)),
                () -> host.look().edgeFog(), rebateFogValue);

        rebateLeaksValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateLeaksSlider = rebateSliderRow(rebateControls, "漏光",
                "卷轴漏进来的光，从片边渗向画面",
                RebateControls.LEAKS_MIN, RebateControls.LEAKS_MAX, 0.05f,
                RebateControls.clampLeaks(host.look().edgeLeaks()),
                value -> String.format(Locale.US, "%.2f", value),
                value -> host.look().edgeLeaks(RebateControls.clampLeaks(value)),
                () -> host.look().edgeLeaks(), rebateLeaksValue);

        rebateFrameNoValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateFrameNoSlider = rebateSliderRow(rebateControls, "片边张数",
                "片边上印的张数。0 = 按 seed 随机",
                RebateControls.FRAME_NUMBER_MIN, RebateControls.FRAME_NUMBER_MAX, 1f,
                RebateControls.clampFrameNumber(host.look().frameNumber()),
                value -> value < 0.5f ? "自动" : String.valueOf(Math.round(value)),
                value -> host.look().frameNumber(RebateControls.clampFrameNumber(value)),
                () -> host.look().frameNumber(), rebateFrameNoValue);

        rebateFNumberValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateFNumberSlider = rebateSliderRow(rebateControls, "光圈值",
                "相机记录的 f 值。决定片门边缘的虚化。0 = 未知（按 f/5.6）",
                RebateControls.F_NUMBER_MIN, RebateControls.F_NUMBER_MAX, 1f,
                RebateControls.clampFNumber(host.look().edgeFNumber()),
                value -> value < 0.5f
                        ? "未知"
                        : "f/" + String.format(Locale.US, "%.1f", value),
                value -> host.look().edgeFNumber(RebateControls.clampFNumber(value)),
                () -> host.look().edgeFNumber(), rebateFNumberValue);
    }

    /** The rebate mode, gate, holes, carrier and seed rows. */
    private void addRebateStructureControls(int onSurface) {
        rebateModeValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateControls.addView(edgeChoiceRow("片边模式", rebateModeValue, RebateControls.MODES,
                () -> host.look().edgeMode(), id -> {
                    host.look().edgeMode(id);
                    syncRebate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        rebateGateStyleValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateControls.addView(edgeChoiceRow("片门形状", rebateGateStyleValue,
                RebateControls.GATE_STYLES, () -> host.look().edgeGate(), id -> {
                    host.look().edgeGate(id);
                    syncRebate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        rebateHolesValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateControls.addView(edgeChoiceRow("齿孔", rebateHolesValue, RebateControls.HOLES,
                () -> host.look().edgeHoles(), id -> {
                    host.look().edgeHoles(id);
                    syncRebate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        rebateCarrierValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateControls.addView(edgeChoiceRow("片夹", rebateCarrierValue,
                RebateControls.CARRIERS, () -> host.look().edgeCarrier(), id -> {
                    host.look().edgeCarrier(id);
                    syncRebate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        rebateCameraSeedValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateControls.addView(rebateValueRow("片边种子", rebateCameraSeedValue,
                () -> String.valueOf(host.look().cameraSeed()),
                () -> host.askSeed("片边随机种子（0 – 2147483647）",
                        host.look().cameraSeed(), value -> {
                            host.look().cameraSeed(value);
                            syncRebate();
                            host.commitRebate();
                        })), new LinearLayout.LayoutParams(-1, -2));

        rebateFrameSeedValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateControls.addView(rebateValueRow("帧种子", rebateFrameSeedValue,
                () -> String.valueOf(host.look().frameSeed()),
                () -> host.askSeed("帧随机种子（0 – 2147483647）",
                        host.look().frameSeed(), value -> {
                            host.look().frameSeed(value);
                            syncRebate();
                            host.commitRebate();
                        })), new LinearLayout.LayoutParams(-1, -2));
    }

    /**
     * The Date back secondary page. It follows the Rebate page's shape: its
     * switch owns whether the controls are enabled, and the controls stay
     * visible and dimmed while off. The default text comes from the Frame's
     * EXIF capture date; a custom text is sent to the engine exactly as typed.
     */
    private LinearLayout buildDatePage() {
        final LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setVisibility(View.GONE);
        page.addView(pageHead("日期水印 / DATE BACK"), new LinearLayout.LayoutParams(-1, -2));

        final int onSurface = theme.role(
                com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK);
        final int onSurfaceVariant = theme.role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);

        dateSwitch = new MaterialSwitch(activity);
        dateDetail = type.m3Label("", M3Tokens.TypeRole.LABEL_SMALL, onSurfaceVariant, tfHan);
        page.addView(rows.switchRow("开启日期水印", dateDetail, dateSwitch,
                () -> host.look().dateOn(), on -> {
                    host.look().dateOn(on);
                    syncDate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, dp(56)));

        dateControls = new LinearLayout(activity);
        dateControls.setOrientation(LinearLayout.VERTICAL);

        // The one field the app used to decide for the user: the EXIF date is
        // the default, but the imprint has always been editable.
        addDateTextField(onSurface);
        addDateGeometryControls(onSurface);
        addDateStyleControls(onSurface);
        addDateDataControls(onSurface);

        dateTextBValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(rebateValueRow("第二帧日期", dateTextBValue,
                () -> host.look().dateTextB().isEmpty() ? "（空）" : host.look().dateTextB(),
                () -> host.askText("半格对的第二帧日期（用不到就留空）",
                        host.look().dateTextB(), value -> {
                            host.look().dateTextB(value == null ? "" : value.trim());
                            syncDate();
                            host.commitRebate();
                        })), new LinearLayout.LayoutParams(-1, -2));

        page.addView(dateControls, new LinearLayout.LayoutParams(-1, -2));
        return page;
    }

    /** The editable date imprint field. */
    private void addDateTextField(int onSurface) {
        dateTextValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(rebateValueRow("日期文字", dateTextValue,
                host::effectiveDateText, () ->
                        host.askText("印在画面上的日期（可以自己写）",
                                host.effectiveDateText(), value -> {
                                    host.look().dateText(value == null || value.trim().isEmpty()
                                            ? "" : value.trim());
                                    syncDate();
                                    host.commitRebate();
                                })), new LinearLayout.LayoutParams(-1, -2));
    }

    /** The date inset, size and exposure sliders. */
    private void addDateGeometryControls(int onSurface) {
        dateInsetXValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        dateInsetXSlider = rebateSliderRow(dateControls, "左右内边距",
                "字到片门左右边的距离，毫米",
                DateBack.INSET_MIN, DateBack.INSET_MAX, 0.1f,
                DateBack.clampInset(host.look().dateInsetX()),
                value -> String.format(Locale.US, "%.1f mm", value),
                value -> host.look().dateInsetX(DateBack.clampInset(value)),
                () -> host.look().dateInsetX(), dateInsetXValue);

        dateInsetYValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        dateInsetYSlider = rebateSliderRow(dateControls, "上下内边距",
                "字到片门上下边的距离，毫米",
                DateBack.INSET_MIN, DateBack.INSET_MAX, 0.1f,
                DateBack.clampInset(host.look().dateInsetY()),
                value -> String.format(Locale.US, "%.1f mm", value),
                value -> host.look().dateInsetY(DateBack.clampInset(value)),
                () -> host.look().dateInsetY(), dateInsetYValue);

        dateSizeValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        dateSizeSlider = rebateSliderRow(dateControls, "日期大小",
                "相对该样式的字高",
                DateBack.SIZE_MIN, DateBack.SIZE_MAX, 0.05f,
                DateBack.clampSize(host.look().dateSize()),
                value -> String.format(Locale.US, "%.2f×", value),
                value -> host.look().dateSize(DateBack.clampSize(value)),
                () -> host.look().dateSize(), dateSizeValue);

        dateEvValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        dateEvSlider = rebateSliderRow(dateControls, "日期亮度",
                "红光在 18% 灰之上的曝光，单位挡",
                DateBack.EV_MIN, DateBack.EV_MAX, 0.1f,
                DateBack.clampEv(host.look().dateEv()),
                value -> String.format(Locale.US, "%+.1f EV", value),
                value -> host.look().dateEv(DateBack.clampEv(value)),
                () -> host.look().dateEv(), dateEvValue);
    }

    /** The date placement, corner and style choices. */
    private void addDateStyleControls(int onSurface) {
        datePlacementValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(edgeChoiceRow("日期位置", datePlacementValue,
                DateBack.PLACEMENTS, () -> host.look().datePlacement(), id -> {
                    host.look().datePlacement(id);
                    syncDate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        dateCornerValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(edgeChoiceRow("日期角位", dateCornerValue,
                DateBack.CORNERS, () -> host.look().dateCorner(), id -> {
                    host.look().dateCorner(id);
                    syncDate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        dateStyleValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(edgeChoiceRow("日期样式", dateStyleValue,
                DateBack.STYLES, () -> host.look().dateStyle(), id -> {
                    host.look().dateStyle(id);
                    syncDate();
                    host.commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));
    }

    /** The capture-data and second-frame text fields. */
    private void addDateDataControls(int onSurface) {
        dateDataValue = type.m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(rebateValueRow("拍摄数据", dateDataValue,
                () -> host.look().dataText().isEmpty() ? "（空）" : host.look().dataText(),
                () -> host.askText("拍摄数据（和日期一起印在片边）",
                        host.look().dataText(), value -> {
                            host.look().dataText(value == null ? "" : value.trim());
                            syncDate();
                            host.commitRebate();
                        })), new LinearLayout.LayoutParams(-1, -2));
    }

    /** Back plus the page name; the two pages share one header shape. */
    private View pageHead(String title) {
        final LinearLayout head = new LinearLayout(activity);
        head.setOrientation(host.largeText()
                ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        final MaterialButton back = rows.textButton("‹ 返回", "返回边框页",
                theme.primary(), host::onBack);
        head.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        final TextView name = type.mixedM3Label(title, M3Tokens.TypeRole.TITLE_SMALL,
                theme.onSurfaceVariant());
        final LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                host.largeText() ? -1 : 0, -2, host.largeText() ? 0f : 1f);
        nameLp.leftMargin = host.largeText() ? 0 : dp(8);
        head.addView(name, nameLp);
        return head;
    }

    /**
     * Fill the Rebate page from the live fields. It runs after a Look restore,
     * a Gate change, or a page open, so the page never keeps a second copy of
     * the Look. The switch guard keeps the slider listeners from committing a
     * render while they are being moved into place.
     */
    public void syncRebate() {
        if (rebateSwitch == null) {
            return;
        }
        syncing = true;
        rows.setSyncingToggles(true);
        try {
            final EdgeLookAccess look = host.look();
            rebateSwitch.setChecked(look.overscanOn());
            rebateSwitch.setContentDescription(rows.toggleDescription("开启片边", look.overscanOn()));
            if (edgeTextSwitch != null) {
                edgeTextSwitch.setChecked(look.edgeTextOn());
                edgeTextSwitch.setContentDescription(rows.toggleDescription(
                        "片边文字（厂标 / 张数）", look.edgeTextOn()));
            }
            refreshAll.run();
        } finally {
            syncing = false;
            rows.setSyncingToggles(false);
        }
        setEnabledDeep(rebateControls, host.look().overscanOn());
    }

    /**
     * Fill the Date back page from the live fields. Like the Rebate page it
     * runs after a Look restore or a page open, and its guard keeps the slider
     * listeners from committing a render while they are being moved into place.
     */
    public void syncDate() {
        if (dateSwitch == null) {
            return;
        }
        syncing = true;
        rows.setSyncingToggles(true);
        try {
            final EdgeLookAccess look = host.look();
            dateSwitch.setChecked(look.dateOn());
            dateSwitch.setContentDescription(rows.toggleDescription("开启日期水印", look.dateOn()));
            syncDateDetail();
            refreshAll.run();
        } finally {
            syncing = false;
            rows.setSyncingToggles(false);
        }
        setEnabledDeep(dateControls, host.look().dateOn());
    }

    /** Show the date back's line only while the imprint is enabled. */
    public void syncDateDetail() {
        if (dateDetail == null) {
            return;
        }
        if (host.look().dateOn()) {
            dateDetail.setText(host.imprintDate());
            dateDetail.setVisibility(View.VISIBLE);
        } else {
            dateDetail.setText("");
            dateDetail.setVisibility(View.GONE);
        }
    }

    /** Dim and disable a whole control group without hiding what it offers. */
    private static void setEnabledDeep(View view, boolean enabled) {
        if (view == null) {
            return;
        }
        setEnabledRecursive(view, enabled);
        // One 38 % overlay for the group; setting it on every child too would
        // compound the opacity and make nested labels unreadable.
        view.setAlpha(enabled ? 1f : M3Tokens.DISABLED_CONTENT_ALPHA);
    }

    private static void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            final android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); ++i) {
                setEnabledRecursive(group.getChildAt(i), enabled);
            }
        }
    }

    /** A slider, its label, and its live value; the pages own eight of them. */
    private Slider rebateSliderRow(LinearLayout parent, String name, String hint,
                                   float min, float max, float step, float initial,
                                   Function<Float, String> format,
                                   Consumer<Float> apply,
                                   Supplier<Float> current,
                                   TextView valueView) {
        final LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(4), 0, 0);

        final LinearLayout head = new LinearLayout(activity);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(type.m3Label(name, M3Tokens.TypeRole.BODY_MEDIUM,
                theme.role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK),
                tfHan), new LinearLayout.LayoutParams(0, -2, 1f));
        valueView.setText(format.apply(initial));
        type.applyTypeRole(valueView, M3Tokens.TypeRole.TITLE_SMALL, tfBody);
        valueView.setTextColor(theme.primary());
        head.addView(valueView, new LinearLayout.LayoutParams(-2, -2));
        row.addView(head, new LinearLayout.LayoutParams(-1, -2));

        if (hint != null && !hint.isEmpty()) {
            final TextView note = type.m3Label(hint, M3Tokens.TypeRole.LABEL_SMALL,
                    theme.onSurfaceVariant(), tfHan);
            note.setLineSpacing(dp(2), 1f);
            final LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
            noteLp.topMargin = dp(1);
            row.addView(note, noteLp);
        }

        final Slider bar = new Slider(activity);
        bar.setId(View.generateViewId());
        bar.setValueFrom(min);
        bar.setValueTo(max);
        bar.setStepSize(step);
        bar.setValue(snapToStep(clampToSlider(initial, min, max), min, step));
        bar.setMinimumHeight(dp(48));
        bar.setLabelFormatter(value -> format.apply(value));
        bar.setLabelBehavior(com.google.android.material.slider.LabelFormatter.LABEL_GONE);
        bar.setContentDescription(name);
        valueView.setLabelFor(bar.getId());
        bar.addOnChangeListener((slider, value, fromUser) -> {
            valueView.setText(format.apply(value));
            // A programmatic park (a Look restore, a page refresh) moves the
            // thumb only. Writing the snapped step back would replace a stored
            // 2.43 mm with 2.4 before the user touched anything.
            if (!fromUser) {
                return;
            }
            apply.accept(value);
            if (!syncing) {
                host.requestLivePreview();
                slider.removeCallbacks(rebateCommit);
                slider.postDelayed(rebateCommit, host.commitDelayMs());
            }
        });
        bar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(Slider slider) {
                slider.removeCallbacks(rebateCommit);
            }
            @Override public void onStopTrackingTouch(Slider slider) {
                slider.removeCallbacks(rebateCommit);
                if (!syncing) {
                    host.commitRebate();
                }
            }
        });
        refreshers.accept(() -> bar.setValue(snapToStep(
                clampToSlider(current.get(), min, max), min, step)));
        row.addView(bar, new LinearLayout.LayoutParams(-1, dp(48)));
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return bar;
    }

    /** A chosen value row backed by the M3 radio sheet; the id is the Look's. */
    private View edgeChoiceRow(String labelText, TextView valueView,
                               List<? extends ControlChoice> choices,
                               Supplier<String> current,
                               Consumer<String> setter) {
        return rebateValueRow(labelText, valueView,
                () -> String.valueOf(ControlChoice.labelOf(choices, current.get())),
                () -> {
                    final int selected = ControlChoice.indexOf(choices, current.get());
                    host.showChoiceSheet(labelText, "只列出这一版支持的选项",
                            Arrays.asList(ControlChoice.labels(choices)),
                            selected, picked -> {
                                if (picked < 0 || picked >= choices.size()) {
                                    return;
                                }
                                setter.accept(choices.get(picked).id);
                            });
                });
    }

    /** A non-sheet row: a label, the live value, and a chevron-like arrow. */
    private View rebateValueRow(String labelText, TextView valueView,
                                Supplier<String> current,
                                Runnable onClick) {
        final LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(52));
        row.setPadding(dp(2), 0, dp(2), 0);
        row.addView(type.m3Label(labelText, M3Tokens.TypeRole.BODY_MEDIUM,
                theme.role(com.google.android.material.R.attr.colorOnSurfaceVariant,
                        Palette.Role.MUTED_INK), tfHan),
                new LinearLayout.LayoutParams(0, -2, 1f));
        type.applyTypeRole(valueView, M3Tokens.TypeRole.TITLE_SMALL, tfHan);
        valueView.setTextColor(theme.onSurface());
        valueView.setGravity(Gravity.END);
        row.addView(valueView, new LinearLayout.LayoutParams(-2, -2));
        final TextView arrow = type.m3Label("↓", M3Tokens.TypeRole.LABEL_LARGE,
                theme.onSurfaceVariant(), tfBody);
        final LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(-2, -2);
        arrowLp.leftMargin = dp(7);
        row.addView(arrow, arrowLp);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> onClick.run());
        refreshers.accept(() -> {
            final String value = current.get();
            valueView.setText(value);
            row.setContentDescription(labelText + "，" + value);
        });
        return row;
    }

    private static float clampToSlider(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Put a stored value on the slider's own step grid. An older Look may hold
     * a raw engine value (2.43 mm) that is not a multiple of the page's step;
     * Material's Slider rejects that instead of nudging it.
     */
    private static float snapToStep(float value, float min, float step) {
        if (step <= 0f) {
            return value;
        }
        final float snapped = min + Math.round((value - min) / step) * step;
        return Math.round(snapped * 1000f) / 1000f;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

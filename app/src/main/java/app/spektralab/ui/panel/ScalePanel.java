package app.spektralab.ui.panel;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import app.spektralab.GateScale;
import app.spektralab.M3Tokens;
import app.spektralab.Palette;
import app.spektralab.core.FormatCardLayout;
import app.spektralab.ui.ConsoleTheme;
import app.spektralab.ui.ConsoleType;
import app.spektralab.ui.M3Chrome;

/**
 * The Scale gate strip: one card per {@link GateScale.Gate} that fits the
 * frame shape. The Activity still owns the Look and applies a tap.
 */
public final class ScalePanel {
    /** Supplies the selected Gate and applies a tap. */
    public interface Host {
        GateScale.Gate activeGate();

        void onPick(String gateId);
    }

    private final Activity activity;
    private final ConsoleType type;
    private final ConsoleTheme theme;
    private final Typeface tfDisplay;
    private final Typeface tfBody;
    private final Typeface tfHan;
    private final Host host;

    private final HorizontalScrollView scroll;
    private final LinearLayout strip;
    private final List<View> cards = new ArrayList<>();
    private List<String> gateIds = new ArrayList<>();

    public ScalePanel(Activity activity, ConsoleType type, ConsoleTheme theme,
                      Typeface tfDisplay, Typeface tfBody, Typeface tfHan,
                      Host host) {
        this.activity = activity;
        this.type = type;
        this.theme = theme;
        this.tfDisplay = tfDisplay;
        this.tfBody = tfBody;
        this.tfHan = tfHan;
        this.host = host;

        scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        strip = new LinearLayout(activity);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(0, dp(10), 0, 0);
        scroll.addView(strip, new FrameLayout.LayoutParams(
                -2, dp(stripHeightDp())));
    }

    public View view() {
        return scroll;
    }

    public View strip() {
        return strip;
    }

    public int stripHeight() {
        return dp(stripHeightDp());
    }

    public float fontScale() {
        return Math.max(1f,
                activity.getResources().getConfiguration().fontScale);
    }

    /** Replace the strip with the Gates that fit the current frame shape. */
    public void rebuild(List<String> ids) {
        this.gateIds = ids == null ? new ArrayList<>() : new ArrayList<>(ids);
        strip.removeAllViews();
        cards.clear();
        if (gateIds.isEmpty()) {
            showNone();
        }
        final int width = FormatCardLayout.widthDp(fontScale());
        final int height = FormatCardLayout.heightDp(fontScale());
        for (String gateId : gateIds) {
            final GateScale.Gate gate = GateScale.byId(gateId);
            if (gate == null) {
                continue;
            }
            final boolean active = host.activeGate() != null
                    && gate.id.equals(host.activeGate().id);
            final MaterialCardView card = buildCard(gate, active);
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(width), dp(height));
            lp.rightMargin = dp(9);
            strip.addView(card, lp);
            cards.add(card);
        }
    }

    /** Scroll the selected card into view. */
    public void revealSelected() {
        final GateScale.Gate active = host.activeGate();
        if (active == null) {
            return;
        }
        final int index = gateIds.indexOf(active.id);
        if (index < 0 || index >= cards.size()) {
            return;
        }
        final View target = cards.get(index);
        scroll.post(() -> scroll.smoothScrollTo(
                Math.max(0, target.getLeft() - dp(8)), 0));
    }

    private void showNone() {
        final TextView none = type.m3Label("这张长宽比没有可用片门",
                M3Tokens.TypeRole.LABEL_LARGE, theme.onSurfaceVariant(), tfHan);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                -2, dp(FormatCardLayout.heightDp(fontScale())));
        lp.leftMargin = dp(2);
        strip.addView(none, lp);
    }

    private MaterialCardView buildCard(GateScale.Gate gate, boolean active) {
        final int surface = theme.role(
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE_ELEVATED);
        final int selectedSurface = theme.role(
                com.google.android.material.R.attr.colorSecondaryContainer,
                Palette.Role.SURFACE_ELEVATED);
        final int outline = theme.outlineVariant();
        final int selectedOutline = theme.primary();
        final int onSurface = theme.onSurface();
        final int onSelected = theme.role(
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                Palette.Role.INK);
        final int onSurfaceVariant = theme.onSurfaceVariant();

        final MaterialCardView card = new MaterialCardView(activity);
        card.setCardElevation(0f);
        card.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(M3Chrome.checkedColors(selectedSurface, surface));
        card.setStrokeColor(M3Chrome.checkedColors(selectedOutline, outline));
        card.setCheckable(true);
        card.setChecked(active);
        card.setCheckedIconResource(
                com.google.android.material.R.drawable.mtrl_ic_check_mark);
        card.setCheckedIconTint(android.content.res.ColorStateList.valueOf(onSelected));
        card.setCheckedIconSize(dp(14));
        card.setCheckedIconMargin(dp(4));
        card.setCheckedIconGravity(MaterialCardView.CHECKED_ICON_GRAVITY_TOP_END);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(gate.label + "，" + mmText(gate.longEdgeMm)
                + " 毫米" + (active ? "，已选" : ""));
        card.addView(buildCardContent(gate, active, onSurface, onSurfaceVariant,
                onSelected), new FrameLayout.LayoutParams(-1, -1));
        card.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(
                    View view, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(view, info);
                info.setClassName(android.widget.RadioButton.class.getName());
                info.setCheckable(true);
                info.setChecked(active);
            }
        });
        card.setOnClickListener(v -> host.onPick(gate.id));
        return card;
    }

    private LinearLayout buildCardContent(GateScale.Gate gate, boolean active,
                                          int onSurface, int onSurfaceVariant,
                                          int onSelected) {
        final LinearLayout stack = new LinearLayout(activity);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setPadding(dp(9), dp(11), dp(8), dp(7));

        final LinearLayout valueRow = new LinearLayout(activity);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.BOTTOM);
        valueRow.addView(type.m3Label(mmText(gate.longEdgeMm),
                M3Tokens.TypeRole.TITLE_LARGE, active ? onSelected : onSurface,
                tfDisplay));
        final TextView unit = type.m3Label("mm", M3Tokens.TypeRole.LABEL_SMALL,
                onSurfaceVariant, tfBody);
        final LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(-2, -2);
        unitLp.leftMargin = dp(2);
        unitLp.bottomMargin = dp(2);
        valueRow.addView(unit, unitLp);
        stack.addView(valueRow);

        final TextView kind = type.m3Label(gate.panoramic() ? "宽幅" : "常规",
                M3Tokens.TypeRole.LABEL_SMALL, onSurfaceVariant, tfHan);
        final LinearLayout.LayoutParams kindLp = new LinearLayout.LayoutParams(-2, -2);
        kindLp.topMargin = dp(4);
        stack.addView(kind, kindLp);
        stack.addView(new View(activity), new LinearLayout.LayoutParams(1, 0, 1f));

        final TextView name = type.m3Label(gate.label,
                M3Tokens.TypeRole.LABEL_MEDIUM, active ? onSelected : onSurface, tfHan);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        stack.addView(name);
        return stack;
    }

    private String mmText(float mm) {
        if (Math.abs(mm - Math.round(mm)) < 0.05f) {
            return String.valueOf(Math.round(mm));
        }
        return String.format(java.util.Locale.US, "%.1f", mm);
    }

    private int stripHeightDp() {
        return FormatCardLayout.stripHeightDp(fontScale());
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

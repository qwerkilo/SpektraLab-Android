package app.spektralab.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.google.android.material.color.MaterialColors;

import app.spektralab.Palette;

/**
 * Resolves the Material 3 colour roles for the active theme.
 *
 * <p>Dynamic colour is applied by the theme; the {@link Palette} role is only
 * the pre-theme and pre-API-31 fallback. Panels ask this object for a role so
 * no colour is hardcoded and a theme switch only swaps the instance.
 */
public final class ConsoleTheme {
    private final Activity activity;
    private Palette palette;

    public ConsoleTheme(Activity activity, Palette palette) {
        this.activity = activity;
        this.palette = palette;
    }

    /** Point at the palette of the theme that is being rebuilt. */
    public void setPalette(Palette value) {
        palette = value;
    }

    /** The M3 surface role, resolved from the active theme (dynamic on API 31+). */
    public int surface() {
        return MaterialColors.getColor(activity,
                com.google.android.material.R.attr.colorSurface,
                palette.color(Palette.Role.SYSTEM_BAR_BACKGROUND));
    }

    /** The M3 background role, used by the shell instead of Palette.BACKGROUND. */
    public int background() {
        return role(android.R.attr.colorBackground, Palette.Role.BACKGROUND);
    }

    public int outlineVariant() {
        return role(com.google.android.material.R.attr.colorOutlineVariant, Palette.Role.LINE);
    }

    public int onSurface() {
        return role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK);
    }

    public int onSurfaceVariant() {
        return role(com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
    }

    public int primary() {
        return role(com.google.android.material.R.attr.colorPrimary, Palette.Role.ACCENT);
    }

    /** A control colour role, with the Palette only as the pre-theme fallback. */
    public int role(int attr, Palette.Role fallback) {
        return MaterialColors.getColor(activity, attr, palette.color(fallback));
    }

    /**
     * The Prism Table glass surface: a translucent M3 surface with a specular
     * outline. Android has no backdrop blur for arbitrary in-Activity views, so
     * this is a deliberate simulated glass, not a claimed window blur.
     */
    public Drawable glassSurface(int radiusDp) {
        final int surface = surface();
        final GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {
                        M3Chrome.withAlpha(surface, GlassStyle.SURFACE_START_ALPHA),
                        M3Chrome.withAlpha(surface, GlassStyle.SURFACE_END_ALPHA) });
        glass.setCornerRadius(dp(radiusDp));
        glass.setStroke(Math.max(1, dp(1)),
                M3Chrome.withAlpha(outlineVariant(), GlassStyle.OUTLINE_ALPHA));
        return glass;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

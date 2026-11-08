package app.spektralab;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Guards the panel rows that the governance refactor dropped and the theme
 * rebuild's page restore: the Date back corner/style choices, the Optics
 * readout, and the open Edge page surviving a theme switch. The source-level
 * {@code ChoiceRowCoverageCheck} cannot see a missing readout group, and the
 * device matrix catches it only after a build and a phone.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class PanelRowsTest {

    @Test
    public void dateBackKeepsCornerAndStyleRows() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "边框");
        click(activity, "日期水印");
        assertShown(activity, "日期角位");
        assertShown(activity, "日期样式");
    }

    @Test
    public void opticsKeepsItsReadout() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "光学");
        assertShown(activity, "放大机曝光");
    }

    @Test
    public void themeSwitchKeepsTheOpenEdgePage() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "边框");
        click(activity, "日期水印");
        assertShown(activity, "日期角位");
        clickTheme(activity);
        assertShown(activity, "日期角位");
    }

    private static void clickTheme(MainActivity activity) {
        final View dark = find(activity, "DARK");
        final View light = find(activity, "LIGHT");
        final View target = dark != null ? dark : light;
        assertNotNull("no theme button labelled DARK/LIGHT", target);
        clickTarget(target);
    }

    private static void assertShown(MainActivity activity, String label) {
        final View target = find(activity, label);
        assertNotNull("no view labelled " + label, target);
        assertTrue(label + " is not visible", target.isShown());
    }

    private static void click(MainActivity activity, String label) {
        final View target = find(activity, label);
        assertNotNull("no view labelled " + label, target);
        clickTarget(target);
    }

    private static void clickTarget(View target) {
        View clickable = target;
        while (clickable != null && !clickable.isClickable()) {
            clickable = clickable.getParent() instanceof View
                    ? (View) clickable.getParent() : null;
        }
        assertNotNull("no clickable ancestor", clickable);
        clickable.performClick();
    }

    private static View find(MainActivity activity, String label) {
        return find(activity.getWindow().getDecorView(), label);
    }

    private static View find(View root, String label) {
        if (root instanceof TextView) {
            final CharSequence text = ((TextView) root).getText();
            if (text != null && text.toString().contains(label)) {
                return root;
            }
        }
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); ++i) {
                final View found = find(group.getChildAt(i), label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

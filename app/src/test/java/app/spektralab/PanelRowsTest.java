package app.spektralab;

import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Guards the panel rows that the governance refactor dropped: the Date back
 * corner/style choices and the Optics readout. The source-level
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
        assertNotNull(find(activity, "日期角位"));
        assertNotNull(find(activity, "日期样式"));
    }

    @Test
    public void opticsKeepsItsReadout() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "光学");
        assertNotNull(find(activity, "放大机曝光"));
    }

    private static void click(MainActivity activity, String label) {
        final View target = find(activity, label);
        assertNotNull("no view labelled " + label, target);
        View clickable = target;
        while (clickable != null && !clickable.isClickable()) {
            clickable = clickable.getParent() instanceof View
                    ? (View) clickable.getParent() : null;
        }
        assertNotNull("no clickable ancestor for " + label, clickable);
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

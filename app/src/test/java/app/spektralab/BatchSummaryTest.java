package app.spektralab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A batch export reopens the frame it started from. That restore used to
 * overwrite the completion line with 「已恢复 · 原图」, and the fix holds the
 * summary until the restore's terminal state.
 *
 * <p>This pins the no-render branch: a frame without a film stays on 原图 and
 * never produces a preview callback, so {@code finishImportRender} is the
 * last chance to show the summary.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class BatchSummaryTest {

    @Test
    public void anOriginalRestoreShowsThePendingBatchSummary() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        set(activity, "pendingBatchSummary", "批量导出完成：1 张成功，1 张跳过（没有胶片）");

        invoke(activity, "finishImportRender");

        assertEquals("批量导出完成：1 张成功，1 张跳过（没有胶片）", status(activity));
    }

    @Test
    public void theSummaryIsConsumedOnce() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        set(activity, "pendingBatchSummary", "批量导出完成：1 张成功");

        invoke(activity, "finishImportRender");
        invoke(activity, "finishImportRender");

        assertNotEquals("批量导出完成：1 张成功", status(activity));
    }

    private static String status(MainActivity activity) throws Exception {
        final Field field = MainActivity.class.getDeclaredField("statusText");
        field.setAccessible(true);
        return ((TextView) field.get(activity)).getText().toString();
    }

    private static void set(MainActivity activity, String name, String value) throws Exception {
        final Field field = MainActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(activity, value);
    }

    private static void invoke(MainActivity activity, String name) throws Exception {
        final Method method = MainActivity.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(activity);
    }
}

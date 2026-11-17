package app.spektralab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

/**
 * A batch export cannot decide a reopened frame is ready by reading
 * {@code session != null} from its worker: the reopen is posted to the UI
 * thread, so the worker can pass the check on the frame it is replacing.
 *
 * <p>The gate is an explicit completion signal: the import's terminal path
 * releases it, and only then does the worker wait for the render.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class BatchReopenGateTest {

    @Test
    public void theImportCompletionReleasesTheBatchGate() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        final CountDownLatch gate = new CountDownLatch(1);
        final Field field = MainActivity.class.getDeclaredField("batchReopenLatch");
        field.setAccessible(true);
        field.set(activity, gate);

        invoke(activity, "finishImportRender");

        assertEquals("the reopen's terminal path must release the gate", 0, gate.getCount());
    }

    @Test
    public void aFrameWithNoLookStillReleasesTheGate() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        final CountDownLatch gate = new CountDownLatch(1);
        final Field field = MainActivity.class.getDeclaredField("batchReopenLatch");
        field.setAccessible(true);
        field.set(activity, gate);

        final Method release = MainActivity.class.getDeclaredMethod("releaseBatchReopen", boolean.class);
        release.setAccessible(true);
        release.invoke(activity, false);
        release.invoke(activity, false);

        assertTrue("releasing twice must be safe", gate.getCount() == 0);
    }

    private static void invoke(MainActivity activity, String name) throws Exception {
        final Method method = MainActivity.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(activity);
    }
}

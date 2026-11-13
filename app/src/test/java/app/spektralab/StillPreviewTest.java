package app.spektralab;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import app.spektralab.importer.StillPreview;

/** Preview sizing and the source-recycle rule that CompareView depends on. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class StillPreviewTest {

    @Test
    public void fitsLargeLandscapeInsideTheEdge() {
        assertArrayEquals(new int[]{1024, 512}, StillPreview.fit(4000, 2000, 1024));
        assertArrayEquals(new int[]{512, 1024}, StillPreview.fit(2000, 4000, 1024));
    }

    @Test
    public void neverUpscales() {
        assertArrayEquals(new int[]{500, 250}, StillPreview.fit(500, 250, 1024));
    }

    @Test
    public void keepsTheMinimumOfOnePixel() {
        assertArrayEquals(new int[]{1, 1024}, StillPreview.fit(1, 10000, 1024));
    }

    @Test
    public void recyclesTheSourceWhenScaled() {
        final Bitmap large = Bitmap.createBitmap(2048, 1024, Bitmap.Config.ARGB_8888);
        final Bitmap preview = StillPreview.create(large, 1024);
        assertTrue(large.isRecycled());
        assertFalse(preview.isRecycled());

    }
}

package app.spektralab;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.ByteBuffer;

import app.spektralab.importer.SourcePacker;

/** The import plane is RGBA→RGB floats, one row at a time. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class SourcePackerTest {

    @Test
    public void bytesForIsThreeFloatsPerPixel() {
        assertEquals(24L, SourcePacker.bytesFor(2, 1));
        assertEquals(24L * 3L * 2L, SourcePacker.bytesFor(2, 6));
    }

    @Test
    public void packsRgbInRowOrder() {
        final Bitmap bitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(0, 0, Color.rgb(255, 0, 0));
        bitmap.setPixel(1, 0, Color.rgb(0, 128, 255));

        final ByteBuffer out = SourcePacker.pack(bitmap, bytes -> ByteBuffer.allocate((int) bytes));

        assertEquals(0, out.position());
        assertEquals(24, out.limit());
        assertEquals(1.0f, out.getFloat(), 1e-6);
        assertEquals(0.0f, out.getFloat(), 1e-6);
        assertEquals(0.0f, out.getFloat(), 1e-6);
        assertEquals(0.0f, out.getFloat(), 1e-6);
        assertEquals(128f / 255f, out.getFloat(), 1e-6);
        assertEquals(1.0f, out.getFloat(), 1e-6);
    }
}

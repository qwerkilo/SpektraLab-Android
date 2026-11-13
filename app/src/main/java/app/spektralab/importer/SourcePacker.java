package app.spektralab.importer;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import app.spektralab.engine.SpektraLabEngine;

/**
 * Pack a decoded bitmap into the engine's float RGB plane.
 *
 * <p>A full {@code int[]} of a 20 MP frame would be another 80 MB on the Java
 * heap next to the direct buffer; the row buffer is one line. The buffer is
 * allocated through the engine because {@code ByteBuffer.allocateDirect} is
 * capped at the heap growth limit; it has no cleaner and must be freed with
 * {@link #release(ByteBuffer)}.
 */
public final class SourcePacker {

    /** Allocates the destination; the Activity passes the engine's allocator. */
    public interface Allocator {
        ByteBuffer allocate(long bytes);
    }

    private SourcePacker() {
    }

    /** Bytes the 3-channel float plane needs for a width × height frame. */
    public static long bytesFor(int width, int height) {
        return (long) width * height * 3L * 4L;
    }

    public static ByteBuffer pack(Bitmap bitmap, Allocator allocator) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final ByteBuffer out = allocator.allocate(bytesFor(width, height))
                .order(ByteOrder.nativeOrder());
        final int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1);
            for (int px : row) {
                out.putFloat(((px >> 16) & 0xFF) / 255.0f);
                out.putFloat(((px >> 8) & 0xFF) / 255.0f);
                out.putFloat((px & 0xFF) / 255.0f);
            }
        }
        out.rewind();
        return out;
    }

    /** Free a plane allocated through {@link SpektraLabEngine#allocDirect}. */
    public static void release(ByteBuffer buffer) {
        if (buffer != null) {
            SpektraLabEngine.freeDirect(buffer);
        }
    }
}

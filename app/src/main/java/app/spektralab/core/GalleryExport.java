package app.spektralab.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies a staged export into a pending destination, verifies the copy, then
 * publishes it. Any failure discards the destination so a broken pending row
 * cannot survive.
 */
public final class GalleryExport {

    private static final int BUFFER_BYTES = 262144;

    private GalleryExport() {
    }

    /** The destination, normally a pending MediaStore row. */
    public interface Sink {
        /** Opens the destination for writing, or returns null. */
        OutputStream openOutput() throws IOException;

        /** Opens the destination for read-back verification, or returns null. */
        InputStream openInput() throws IOException;

        /** Makes the verified copy visible. */
        void publish() throws IOException;

        /** Removes a failed destination; cleanup must not mask the first error. */
        void discard();
    }

    /**
     * Copies {@code staged}, verifies length and SHA-256, then publishes.
     * Returns the verified digest. On any failure the sink is discarded.
     */
    public static ExportDigest.Result write(File staged, Sink sink) throws IOException {
        try {
            copy(staged, sink);
            final ExportDigest.Result expected = ExportDigest.scan(staged);
            final ExportDigest.Result copied;
            try (InputStream in = sink.openInput()) {
                if (in == null) {
                    throw new IOException("无法回读导出文件");
                }
                copied = ExportDigest.scan(in);
            }
            if (!ExportDigest.matches(expected, copied)) {
                throw new IOException("导出校验失败：长度或摘要不一致");
            }
            sink.publish();
            return copied;
        } catch (Throwable failure) {
            try {
                sink.discard();
            } catch (Throwable ignored) {
                // The row may already be gone; the original failure matters more.
            }
            if (failure instanceof IOException) {
                throw (IOException) failure;
            }
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            throw new IOException(failure);
        }
    }

    private static void copy(File staged, Sink sink) throws IOException {
        try (InputStream in = new FileInputStream(staged);
             OutputStream out = sink.openOutput()) {
            if (out == null) {
                throw new IOException("无法打开输出流");
            }
            final byte[] buffer = new byte[BUFFER_BYTES];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
    }
}

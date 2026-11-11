package app.spektralab;

import app.spektralab.core.ExportDigest;
import app.spektralab.core.GalleryExport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** A gallery export publishes only a verified copy and discards every failure. */
public final class GalleryExportCheck {

    private static int checks;

    public static void main(String[] args) throws Exception {
        final File staged = Files.createTempFile("spektralab-export", ".jpg").toFile();
        final byte[] hello = "hello".getBytes(StandardCharsets.UTF_8);
        Files.write(staged.toPath(), hello);
        try {
            final FakeSink ok = new FakeSink();
            final ExportDigest.Result result = GalleryExport.write(staged, ok);
            eq(5L, result.length, "verified length");
            eq("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                    ExportDigest.hex(result.sha256), "verified digest");
            ok(!ok.discard, "success keeps the row");
            ok(ok.publish, "success publishes");
            eq("hello", ok.copy.toString(StandardCharsets.UTF_8.name()), "copied bytes");

            final FakeSink mismatch = new FakeSink();
            mismatch.readBack = "hellp".getBytes(StandardCharsets.UTF_8);
            ok(threw(mismatch, staged), "digest mismatch rejected");
            ok(!mismatch.publish, "mismatch does not publish");
            ok(mismatch.discard, "mismatch deletes the pending row");

            final FakeSink noOutput = new FakeSink();
            noOutput.failOpen = true;
            ok(threw(noOutput, staged), "open failure rejected");
            ok(noOutput.discard, "open failure deletes the pending row");

            final FakeSink noInput = new FakeSink();
            noInput.nullInput = true;
            ok(threw(noInput, staged), "read-back failure rejected");
            ok(noInput.discard, "read-back failure deletes the pending row");

            final FakeSink noPublish = new FakeSink();
            noPublish.failPublish = true;
            ok(threw(noPublish, staged), "publish failure rejected");
            ok(noPublish.discard, "publish failure deletes the pending row");

            final FakeSink stubborn = new FakeSink();
            stubborn.failPublish = true;
            stubborn.discardThrows = true;
            ok(threw(stubborn, staged), "discard failure keeps the first error");
        } finally {
            Files.deleteIfExists(staged.toPath());
        }
        System.out.println("GalleryExportCheck: " + checks + " checks passed");
    }

    private static boolean threw(FakeSink sink, File staged) {
        try {
            GalleryExport.write(staged, sink);
            return false;
        } catch (Exception expected) {
            return true;
        }
    }

    private static final class FakeSink implements GalleryExport.Sink {
        private final ByteArrayOutputStream copy = new ByteArrayOutputStream();
        private byte[] readBack;
        private boolean failOpen;
        private boolean nullInput;
        private boolean failPublish;
        private boolean discardThrows;
        private boolean publish;
        private boolean discard;

        @Override
        public OutputStream openOutput() throws IOException {
            if (failOpen) {
                throw new IOException("no output");
            }
            return copy;
        }

        @Override
        public InputStream openInput() {
            if (nullInput) {
                return null;
            }
            return new ByteArrayInputStream(readBack == null ? copy.toByteArray() : readBack);
        }

        @Override
        public void publish() throws IOException {
            if (failPublish) {
                throw new IOException("no publish");
            }
            publish = true;
        }

        @Override
        public void discard() {
            discard = true;
            if (discardThrows) {
                throw new IllegalStateException("discard failed");
            }
        }
    }

    private static void ok(boolean condition, String label) {
        ++checks;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void eq(Object want, Object got, String label) {
        ++checks;
        if (!java.util.Objects.equals(want, got)) {
            throw new AssertionError(label + ": want " + want + ", got " + got);
        }
    }

    private static void eq(long want, long got, String label) {
        ++checks;
        if (want != got) {
            throw new AssertionError(label + ": want " + want + ", got " + got);
        }
    }
}

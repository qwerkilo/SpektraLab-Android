package app.spektralab;

import app.spektralab.core.ImageFormat;

/** Checks the filename-based format decisions used by import. */
public final class ImageFormatCheck {
    private static int checks;

    private ImageFormatCheck() {
    }

    public static void main(String[] args) {
        ok(ImageFormat.isRaw("shot.DNG"));
        ok(ImageFormat.isRaw("shot.arw"));
        ok(ImageFormat.isRaw("shot.rw2"));
        ok(!ImageFormat.isRaw("shot.jpg"));
        ok(!ImageFormat.isRaw("shot.heic"));
        ok(!ImageFormat.isRaw(null));
        equal(".iiq", ImageFormat.unsupportedRawExtension("shot.IIQ"));
        equal(".mos", ImageFormat.unsupportedRawExtension("shot.mos"));
        equal(".heic.raw", ImageFormat.unsupportedRawExtension("shot.heic.raw"));
        equal(null, ImageFormat.unsupportedRawExtension("shot.dng"));
        System.out.println("ImageFormatCheck: " + checks + " checks passed");
    }

    private static void ok(boolean value) {
        if (!value) {
            throw new AssertionError("expected true");
        }
        checks++;
    }

    private static void equal(String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
        checks++;
    }
}

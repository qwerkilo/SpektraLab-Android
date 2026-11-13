package app.spektralab;

import app.spektralab.core.ExifTransform;

/** EXIF orientation values mapped to a rotate/flip pair. */
public final class ExifTransformCheck {

    private static int checks;

    private ExifTransformCheck() {
    }

    public static void main(String[] args) {
        same(0, 0, false, false);
        same(1, 0, false, false);
        same(2, 0, true, false);
        same(3, 180, false, false);
        same(4, 0, false, true);
        same(5, 90, true, false);
        same(6, 90, false, false);
        same(7, -90, true, false);
        same(8, 270, false, false);
        same(9, 0, false, false);
        same(-1, 0, false, false);

        check(ExifTransform.of(1).isIdentity());
        check(!ExifTransform.of(3).isIdentity());
        check(!ExifTransform.of(2).isIdentity());

        System.out.println("ExifTransformCheck: " + checks + " checks passed");
    }

    private static void same(int orientation, int degrees, boolean flipX, boolean flipY) {
        final ExifTransform t = ExifTransform.of(orientation);
        ++checks;
        if (t.degrees != degrees || t.flipX != flipX || t.flipY != flipY) {
            throw new AssertionError("orientation " + orientation + " -> " + t.degrees
                    + " flipX=" + t.flipX + " flipY=" + t.flipY);
        }
    }

    private static void check(boolean ok) {
        ++checks;
        if (!ok) {
            throw new AssertionError("identity mismatch");
        }
    }
}

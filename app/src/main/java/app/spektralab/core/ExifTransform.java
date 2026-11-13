package app.spektralab.core;

/**
 * The eight EXIF orientation values as one rotate + optional flip. Pure, so the
 * mapping is checked without a bitmap.
 */
public final class ExifTransform {

    public static final ExifTransform NONE = new ExifTransform(0, false, false);

    public final int degrees;
    public final boolean flipX;
    public final boolean flipY;

    private ExifTransform(int degrees, boolean flipX, boolean flipY) {
        this.degrees = degrees;
        this.flipX = flipX;
        this.flipY = flipY;
    }

    /** Unknown, normal and unreadable values all leave the pixels alone. */
    public static ExifTransform of(int orientation) {
        switch (orientation) {
            case 2:
                return new ExifTransform(0, true, false);
            case 3:
                return new ExifTransform(180, false, false);
            case 4:
                return new ExifTransform(0, false, true);
            case 5:
                return new ExifTransform(90, true, false);
            case 6:
                return new ExifTransform(90, false, false);
            case 7:
                return new ExifTransform(-90, true, false);
            case 8:
                return new ExifTransform(270, false, false);
            default:
                return NONE;
        }
    }

    public boolean isIdentity() {
        return degrees == 0 && !flipX && !flipY;
    }
}

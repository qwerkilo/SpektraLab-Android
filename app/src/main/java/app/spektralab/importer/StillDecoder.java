package app.spektralab.importer;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Size;

import androidx.exifinterface.media.ExifInterface;

import java.io.InputStream;

import app.spektralab.DebugSink;
import app.spektralab.core.ExifTransform;
import app.spektralab.core.ImportBudget;

/**
 * Decode one still, apply its EXIF orientation and scale it inside the import
 * budget. No engine call, no View, no Look: the caller owns those.
 */
public final class StillDecoder {

    /** The bitmap to pack plus the size and orientation it ended up with. */
    public static final class Result {
        public final Bitmap bitmap;
        public final int width;
        public final int height;
        public final int orientation;

        Result(Bitmap bitmap, int orientation) {
            this.bitmap = bitmap;
            this.width = bitmap.getWidth();
            this.height = bitmap.getHeight();
            this.orientation = orientation;
        }
    }

    private StillDecoder() {
    }

    /** @return the decoded bitmap, or null when every decoder refused it */
    public static Result decode(ContentResolver resolver, Uri uri, int capPixels,
                                int edgeCap, long availableBytes) {
        Bitmap bitmap = decodeBitmap(resolver, uri, capPixels, edgeCap);
        if (bitmap == null) {
            return null;
        }
        final int orientation = readOrientation(resolver, uri);
        bitmap = rotateForOrientation(bitmap, orientation);
        try {
            DebugSink.text("exif", "orientation=" + orientation + " bitmap="
                    + bitmap.getWidth() + "x" + bitmap.getHeight());
        } catch (Throwable ignored) {
        }

        final double k = ImportBudget.scale(bitmap.getWidth(), bitmap.getHeight(),
                capPixels, edgeCap, availableBytes);
        if (k < 1.0) {
            final int nw = Math.max(1, (int) Math.round(bitmap.getWidth() * k));
            final int nh = Math.max(1, (int) Math.round(bitmap.getHeight() * k));
            final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true);
            if (scaled != bitmap) {
                bitmap.recycle();
            }
            bitmap = scaled;
            try {
                DebugSink.text("import-scale", "k=" + k + " -> " + nw + "x" + nh
                        + " avail=" + availableBytes);
            } catch (Throwable ignored) {
            }
        }
        return new Result(bitmap, orientation);
    }

    /**
     * `BitmapFactory` first -- the path every JPEG and PNG has always taken --
     * and `ImageDecoder` when that comes back empty. The second attempt is for
     * HEIC/HEIF: Samsung and iPhone save 10-bit HEIF, `BitmapFactory` returns
     * null (and no bounds), while the platform decoder behind `ImageDecoder`
     * (API 28+) hands back a software bitmap.
     */
    private static Bitmap decodeBitmap(ContentResolver resolver, Uri uri,
                                       int capPixels, int edgeCap) {
        try {
            final BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream head = resolver.openInputStream(uri)) {
                if (head != null) {
                    BitmapFactory.decodeStream(head, null, bounds);
                }
            } catch (Throwable t) {
                try {
                    DebugSink.text("decode", "sniff " + t);
                } catch (Throwable ignored) {
                }
            }
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                int sample = 1;
                while ((long) (bounds.outWidth / (sample * 2))
                        * (bounds.outHeight / (sample * 2)) >= capPixels
                        || Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= edgeCap) {
                    sample *= 2;
                }
                final BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = sample;
                try (InputStream stream = resolver.openInputStream(uri)) {
                    final Bitmap decoded = BitmapFactory.decodeStream(stream, null, opts);
                    if (decoded != null) {
                        return decoded;
                    }
                    try {
                        DebugSink.text("decode", "null at sample " + sample);
                    } catch (Throwable ignored) {
                    }
                } catch (Throwable t) {
                    try {
                        DebugSink.text("decode", "stream " + t);
                    } catch (Throwable ignored) {
                    }
                }
            } else {
                try {
                    DebugSink.text("decode", "no bounds " + bounds.outWidth
                            + "x" + bounds.outHeight);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            try {
                DebugSink.text("decode", "outer " + t);
            } catch (Throwable ignored) {
            }
        }
        return decodeViaImageDecoder(resolver, uri, capPixels, edgeCap);
    }

    /**
     * The platform decoder through its newer door, scaled in the decoder so the
     * full-size bitmap never exists. Software allocator on purpose: a hardware
     * bitmap cannot be read back with `getPixels`, and the import path reads
     * every pixel into a float buffer.
     */
    private static Bitmap decodeViaImageDecoder(ContentResolver resolver, Uri uri,
                                                final int capPixels, final int edgeCap) {
        if (Build.VERSION.SDK_INT < 28) {
            return null;
        }
        try {
            final ImageDecoder.Source src = ImageDecoder.createSource(resolver, uri);
            final Bitmap decoded = ImageDecoder.decodeBitmap(src,
                    (decoder, info, source) -> {
                        final Size size = info.getSize();
                        final int w = size.getWidth();
                        final int h = size.getHeight();
                        final double k = ImportBudget.scale(w, h, capPixels, edgeCap, 0);
                        if (k < 1.0) {
                            decoder.setTargetSize(Math.max(1, (int) Math.round(w * k)),
                                    Math.max(1, (int) Math.round(h * k)));
                        }
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
            try {
                DebugSink.text("decode", "imagedecoder " + decoded.getWidth()
                        + "x" + decoded.getHeight());
            } catch (Throwable ignored) {
            }
            return decoded;
        } catch (Throwable t) {
            try {
                DebugSink.text("decode", "imagedecoder " + t);
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    private static int readOrientation(ContentResolver resolver, Uri uri) {
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in != null) {
                return new ExifInterface(in).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Throwable ignored) {
        }
        try (ParcelFileDescriptor fd = resolver.openFileDescriptor(uri, "r")) {
            if (fd != null) {
                return new ExifInterface(fd.getFileDescriptor()).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    /** The orientation tag as a rotate + flip; identity leaves the pixels alone. */
    private static Bitmap rotateForOrientation(Bitmap bitmap, int orientation) {
        if (orientation <= 0) {
            return bitmap;
        }
        final ExifTransform t = ExifTransform.of(orientation);
        if (t.isIdentity()) {
            return bitmap;
        }
        try {
            final Matrix matrix = new Matrix();
            if (t.degrees != 0) {
                matrix.postRotate(t.degrees);
            }
            if (t.flipX || t.flipY) {
                matrix.postScale(t.flipX ? -1 : 1, t.flipY ? -1 : 1);
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            return bitmap;
        }
    }
}

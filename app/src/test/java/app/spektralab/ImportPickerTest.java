package app.spektralab;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import app.spektralab.ui.panel.ImportPicker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The import chains, without a device picker.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ImportPickerTest {

    private static class Recorder implements ImportPicker.Host {
        final List<Intent> seen = new ArrayList<>();
        final List<Intent> started = new ArrayList<>();
        boolean noPicker;
        int refuseFirst;

        @Override public boolean canResolve(Intent intent) {
            seen.add(intent);
            if (refuseFirst > 0) {
                --refuseFirst;
                return false;
            }
            return true;
        }

        @Override public void start(Intent intent, int requestCode) {
            started.add(intent);
        }

        @Override public void noPicker(boolean documents) {
            noPicker = true;
        }
    }

    @Test
    public void galleryPrefersThePhotoPickerThenFallsBackToDocuments() {
        final Recorder host = new Recorder();
        new ImportPicker(host).pickGallery();

        assertEquals(1, host.started.size());
        assertEquals(android.provider.MediaStore.ACTION_PICK_IMAGES,
                host.started.get(0).getAction());
        assertTrue(!host.noPicker);
    }

    @Test
    public void gallerySkipsUnresolvablePickersAndKeepsTheGrant() {
        final Recorder host = new Recorder();
        host.refuseFirst = 2;
        new ImportPicker(host).pickGallery();

        final Intent started = host.started.get(0);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, started.getAction());
        assertTrue("the document fallback must keep its persistable grant",
                (started.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0);
        assertEquals(3, host.seen.size());
    }

    @Test
    public void documentsAsksForAnyFileWithRawMimeTypes() {
        final Recorder host = new Recorder();
        new ImportPicker(host).pickDocuments();

        final Intent first = host.started.get(0);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, first.getAction());
        assertEquals("*/*", first.getType());
        assertTrue((first.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0);
        final String[] mime = first.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);
        assertTrue(contains(mime, "image/x-adobe-dng"));
        assertTrue(contains(mime, "image/heic"));
    }

    @Test
    public void noResolvablePickerReportsOnce() {
        final Recorder host = new Recorder();
        host.refuseFirst = Integer.MAX_VALUE;
        new ImportPicker(host).pickDocuments();

        assertTrue(host.noPicker);
        assertEquals(2, host.seen.size());
        assertEquals(0, host.started.size());
    }

    @Test
    public void aThrowingPickerFallsThroughToTheNextOne() {
        final Recorder host = new Recorder() {
            @Override public void start(Intent intent, int requestCode) {
                if (started.isEmpty()) {
                    started.add(intent);
                    throw new SecurityException("provider vanished");
                }
                super.start(intent, requestCode);
            }
        };
        new ImportPicker(host).pickGallery();

        assertEquals(2, host.started.size());
        assertEquals(Intent.ACTION_GET_CONTENT, host.started.get(1).getAction());
        assertTrue(!host.noPicker);
    }

    private static boolean contains(String[] values, String wanted) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (wanted.equals(value)) {
                return true;
            }
        }
        return false;
    }
}

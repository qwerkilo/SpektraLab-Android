package app.spektralab.ui.panel;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import app.spektralab.DebugSink;

/**
 * Builds the two import intent chains and launches the first one the ROM
 * resolves.
 *
 * <p>Gallery and Documents are separate entries on purpose: a RAW container is
 * often typed {@code application/octet-stream}, so an {@code image/*} filter
 * hides it; Documents asks for any file and names the known RAW/HEIF types.
 * The launch must never crash the process, so each candidate is tried in order
 * and a throwing picker falls through to the next.
 */
public final class ImportPicker {

    /** Request code shared with {@code onActivityResult}. */
    public static final int REQUEST_CODE = 1;

    /** What the Activity provides; the controller owns the decision. */
    public interface Host {
        boolean canResolve(Intent intent);

        void start(Intent intent, int requestCode);

        /** No picker in the chain could be launched. */
        void noPicker(boolean documents);
    }

    private final Host host;

    public ImportPicker(Host host) {
        this.host = host;
    }

    /** Gallery or the photo picker. */
    public void pickGallery() {
        final List<Intent> chain = new ArrayList<>();
        // GET_CONTENT first: on HyperOS the ACTION_PICK_IMAGES component resolves
        // but finishes immediately, so preferring it makes 从相册选择 do nothing.
        chain.add(new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true));
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            chain.add(new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES));
        }
        chain.add(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
        launch(chain, false);
    }

    /** Documents, for RAW and HEIC the gallery will not show. */
    public void pickDocuments() {
        final String[] types = {
                "image/*",
                "image/heic", "image/heif", "image/x-heic", "image/x-heif", "image/x-cfif",
                "application/octet-stream",
                "image/x-sony-arw", "image/x-canon-cr2", "image/x-canon-cr3",
                "image/x-nikon-nef", "image/x-adobe-dng", "image/x-panasonic-rw2",
                "image/x-panasonic-raw", "image/x-olympus-orf", "image/x-fuji-raf",
                "image/x-pentax-pef", "image/x-samsung-srw", "image/x-sigma-x3f"
        };
        final List<Intent> chain = new ArrayList<>();
        chain.add(document(Intent.ACTION_OPEN_DOCUMENT, types));
        chain.add(document(Intent.ACTION_GET_CONTENT, types));
        launch(chain, true);
    }

    private static Intent document(String action, String[] types) {
        final Intent intent = new Intent(action);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (Intent.ACTION_OPEN_DOCUMENT.equals(action)) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }
        return intent;
    }

    private void launch(List<Intent> chain, boolean documents) {
        for (Intent intent : chain) {
            try {
                if (!host.canResolve(intent)) {
                    log("unresolvable " + intent.getAction());
                    continue;
                }
                host.start(intent, REQUEST_CODE);
                log("launched " + intent.getAction());
                return;
            } catch (Throwable t) {
                // Resolvable and still refused: a provider vanished between the
                // check and the launch, or the picker throws in its own onCreate.
                log(intent.getAction() + " failed: " + t);
            }
        }
        host.noPicker(documents);
    }

    private static void log(String text) {
        try {
            DebugSink.text("picker", text);
        } catch (Throwable ignored) {
            // Diagnostics must never break picking.
        }
    }
}

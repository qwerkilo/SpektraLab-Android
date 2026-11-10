package app.spektralab;

import app.spektralab.core.SessionDocument;

import org.json.JSONObject;

/** The current-frame session document round-trips and rejects bad input. */
public final class SessionDocumentCheck {

    private static int checks;

    public static void main(String[] args) throws Exception {
        final java.io.File dir = java.nio.file.Files.createTempDirectory("spk-session").toFile();
        final java.io.File file = SessionDocument.file(dir);

        final JSONObject look = new JSONObject().put("film", "kodak_portra_400")
                .put("grain", 0.95).put("gturn", 90);
        final SessionDocument first = new SessionDocument(
                "content://example/photo.jpg", "photo.jpg", false, 2, 0.4f, true, false, look);
        first.write(file);
        final SessionDocument back = SessionDocument.read(file);
        ok(back != null, "document reads back");
        eq(first.uri(), back.uri(), "uri");
        eq(first.name(), back.name(), "name");
        eq(false, back.raw(), "raw");
        eq(2, back.tab(), "tab");
        eq(0.4f, back.scanFraction(), "scan fraction");
        eq(true, back.scanVisible(), "scan visible");
        eq(false, back.original(), "original");
        eq("kodak_portra_400", back.look().optString("film"), "look film");
        ok(!new java.io.File(file.getPath() + ".tmp").exists(), "temporary file removed");

        final SessionDocument second = new SessionDocument(
                "content://example/second.dng", "second.dng", true, 3, 0.8f, false, true,
                new JSONObject().put("film", "kodak_tri_x_400"));
        second.write(file);
        final SessionDocument replaced = SessionDocument.read(file);
        eq("content://example/second.dng", replaced.uri(), "write replaces document");
        eq(true, replaced.raw(), "raw survives");
        eq(true, replaced.original(), "original survives");

        ok(SessionDocument.fromJson(new JSONObject()) == null, "missing uri rejected");
        ok(SessionDocument.fromJson(new JSONObject().put("uri", "x")) == null,
                "missing look rejected");

        java.nio.file.Files.write(file.toPath(), "{not json".getBytes(
                java.nio.charset.StandardCharsets.UTF_8));
        ok(SessionDocument.read(file) == null, "malformed JSON rejected");
        java.nio.file.Files.write(file.toPath(), new byte[0]);
        ok(SessionDocument.read(file) == null, "empty file rejected");

        file.delete();
        dir.delete();
        System.out.println("SessionDocumentCheck: " + checks + " checks passed");
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

    private static void eq(float want, float got, String label) {
        ++checks;
        if (Math.abs(want - got) > 1e-5f) {
            throw new AssertionError(label + ": want " + want + ", got " + got);
        }
    }
}

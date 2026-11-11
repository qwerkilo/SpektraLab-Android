package app.spektralab;

import app.spektralab.core.ExportDigest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** The export verifier compares length and SHA-256, not just bytes. */
public final class ExportDigestCheck {

    private static int checks;

    public static void main(String[] args) throws Exception {
        final byte[] hello = "hello".getBytes(StandardCharsets.UTF_8);
        final ExportDigest.Result a = ExportDigest.scan(new ByteArrayInputStream(hello));
        eq(5L, a.length, "byte count");
        eq("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                ExportDigest.hex(a.sha256), "known SHA-256");
        ok(ExportDigest.matches(a, a), "same result matches");

        final ExportDigest.Result b = ExportDigest.scan(new ByteArrayInputStream(hello));
        ok(ExportDigest.matches(a, b), "equal bytes match");
        final byte[] changed = "hellp".getBytes(StandardCharsets.UTF_8);
        ok(!ExportDigest.matches(a,
                ExportDigest.scan(new ByteArrayInputStream(changed))),
                "one-byte difference rejected");
        ok(!ExportDigest.matches(a,
                ExportDigest.scan(new ByteArrayInputStream("hello!".getBytes(
                        StandardCharsets.UTF_8)))), "length difference rejected");
        ok(!ExportDigest.matches(a, null), "null rejected");

        System.out.println("ExportDigestCheck: " + checks + " checks passed");
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

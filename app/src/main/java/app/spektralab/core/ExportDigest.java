package app.spektralab.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/** Length + SHA-256 verification for a gallery export. */
public final class ExportDigest {

    private ExportDigest() {
    }

    /** A stream's byte count and SHA-256 digest. */
    public static final class Result {
        public final long length;
        public final byte[] sha256;

        Result(long length, byte[] sha256) {
            this.length = length;
            this.sha256 = sha256;
        }
    }

    public static Result scan(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return scan(in);
        }
    }

    public static Result scan(InputStream in) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
        final byte[] buf = new byte[262144];
        long length = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            digest.update(buf, 0, n);
            length += n;
        }
        return new Result(length, digest.digest());
    }

    /** True only when both the byte count and the digest agree. */
    public static boolean matches(Result a, Result b) {
        return a != null && b != null && a.length == b.length
                && MessageDigest.isEqual(a.sha256, b.sha256);
    }

    public static String hex(byte[] bytes) {
        final StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(Character.forDigit((b >>> 4) & 0x0f, 16));
            out.append(Character.forDigit(b & 0x0f, 16));
        }
        return out.toString();
    }
}

package app.spektralab;

import app.spektralab.core.ErrorText;
import app.spektralab.engine.SpektraLabEngine;

/**
 * Every failure the user can see carries a stable code.
 *
 * The engine classifies (see spk_error_code); this table says what to do about
 * it in words. A code without a message would be a riddle, and a message
 * without a code cannot be reported — so the UI shows both, always, and a
 * screenshot is enough to file the bug.
 */
public final class SpkError {

    private SpkError() {}

    /**
     * The stable code for a raw engine message, or 0 when the engine has no
     * opinion. Callers that want to *act* on a failure branch on this: E-1001
     * is the one with a cure (a smaller session), and a string compare on the
     * wording would break the moment the wording improves.
     */
    public static int codeOf(String raw) {
        if (raw == null || raw.isEmpty()) return 0;
        try { return SpektraLabEngine.errorCode(raw); } catch (Throwable ignored) { return 0; }
    }

    /**
     * A MediaStore write failure can also arrive as a SecurityException, but
     * the cure is not "re-import the photo". Identify it before the picker
     * branch so E-1304 keeps meaning the picker's grant.
     */
    static boolean isMediaStoreFailure(String raw) {
        if (raw == null) return false;
        return raw.contains("MediaStore") || raw.contains("MediaProvider")
                || raw.contains("IS_PENDING") || raw.contains("RELATIVE_PATH")
                || raw.contains("无法创建媒体文件") || raw.contains("无法打开输出流")
                || raw.contains("openOutputStream");
    }

    /**
     * The pixels are finished and on disk; the provider refused its own new
     * row. That distinction matters: "render failed" sends the user to their
     * photo library, "the album would not take the file" sends them to a
     * restart, and the two need different words.
     */
    static boolean isAlbumWriteFailure(String raw) {
        if (raw == null) return false;
        return raw.contains("相册写入失败") || raw.contains("No item at content://media/");
    }

    static boolean isPermissionFailure(String raw) {
        if (raw == null || isMediaStoreFailure(raw)) return false;
        return raw.contains("SecurityException") || raw.contains("Permission Denial")
                || raw.contains("that is not exported") || raw.contains("not exported from UID");
    }

    /** "[E-1001] 中文说明" plus the engine's own text, which carries the numbers. */
    public static String format(String raw) {
        if (raw == null || raw.isEmpty()) return "[E-0000] 未知错误";
        if (isAlbumWriteFailure(raw)) return "[E-1306] " + ErrorText.GALLERY_WRITE + "（" + raw + "）";
        if (isMediaStoreFailure(raw)) return "[E-1306] " + ErrorText.GALLERY_WRITE + "（" + raw + "）";
        if (isPermissionFailure(raw)) return "[E-1304] " + ErrorText.IMPORT_PERMISSION + "（" + raw + "）";
        int code = codeOf(raw);
        if (code == 0) code = 9000;
        if (code == 9000 && isNativeLoadFailure(raw)) {
            if (raw.contains("libvulkan.so")) return "[E-9000] " + ErrorText.NO_VULKAN + "（" + raw + "）";
            return "[E-9000] " + ErrorText.NATIVE_LOAD + "（" + raw + "）";
        }
        return "[E-" + code + "] " + explain(code) + "（" + raw + "）";
    }

    static boolean isNativeLoadFailure(String raw) {
        if (raw == null) return false;
        return raw.contains("libspektrafilm_engine") || raw.contains("引擎库没有加载成功")
                || raw.contains("dlopen failed") || raw.contains("UnsatisfiedLink");
    }

    /** Short form for a status line, without the engine text. */
    public static String shortForm(String raw) {
        if (isMediaStoreFailure(raw)) return ErrorText.shortForm(1306);
        if (isPermissionFailure(raw)) return ErrorText.shortForm(1304);
        int code = codeOf(raw);
        if (code == 0) code = 9000;
        if (code == 9000 && isNativeLoadFailure(raw)) return ErrorText.shortForm(9000);
        final String known = ErrorText.shortForm(code);
        return known != null ? known : "[E-" + code + "] " + explain(code);
    }

    /** The full sentence for one stable code. */
    public static String explain(int code) {
        return ErrorText.explain(code);
    }
}

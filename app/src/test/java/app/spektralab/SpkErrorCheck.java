package app.spektralab;

/** The user-facing error table keeps its codes and its native-load branch. */
public final class SpkErrorCheck {
    private static int checks;

    public static void main(String[] args) {
        eq("[E-0000] 未知错误", SpkError.format(null));
        eq("[E-0000] 未知错误", SpkError.format(""));

        ok(SpkError.explain(1001).contains("GPU"));
        ok(SpkError.explain(1009).contains("显存"));
        ok(SpkError.explain(1010).contains("42 MP"));
        ok(SpkError.explain(1108).contains("HE"));
        ok(SpkError.explain(1109).contains("DNG"));
        ok(SpkError.explain(1110).contains("机型"));
        ok(SpkError.explain(1302).contains("参数"));
        ok(SpkError.explain(2001).contains("网络"));
        ok(SpkError.explain(9999).contains("未归类"));
        ok(!SpkError.explain(9999).isEmpty());

        ok(SpkError.isNativeLoadFailure(null) == false);
        ok(SpkError.isNativeLoadFailure("plain engine message") == false);
        ok(SpkError.isNativeLoadFailure("dlopen failed: libx.so"));
        ok(SpkError.isNativeLoadFailure("UnsatisfiedLinkError"));
        ok(SpkError.isNativeLoadFailure("libspektrafilm_engine.so"));

        driverCodes();
        permissionFailure();
        jniContract();

        System.out.println("SpkErrorCheck: " + checks + " checks passed");
    }

    /** E-1005/1006/1011 name the driver cause instead of falling back to E-9000. */
    private static void driverCodes() {
        ok(SpkError.explain(1005).contains("Vulkan"));
        ok(SpkError.explain(1005).contains("1.1"));
        ok(SpkError.explain(1006).contains("驱动能力"));
        ok(SpkError.explain(1011).contains("Vulkan 设备建立失败"));
        ok(SpkError.explain(1011).contains("重启"));
        ok(SpkError.explain(1304).contains("权限"));
        ok(!SpkError.explain(1005).equals(SpkError.explain(1006)));
        ok(!SpkError.explain(1006).equals(SpkError.explain(1011)));
        ok(!SpkError.explain(1005).equals(SpkError.explain(1011)));
    }

    /** A revoked picker grant is E-1304, not a nameless E-9000. */
    private static void permissionFailure() {
        final String denial = "java.lang.SecurityException: Permission Denial: opening provider "
                + "com.android.providers.media.MediaDocumentsProvider from ProcessRecord";
        ok(SpkError.isPermissionFailure(denial));
        ok(SpkError.isPermissionFailure("that is not exported from UID 10234"));
        ok(!SpkError.isPermissionFailure(null));
        ok(!SpkError.isPermissionFailure("plain render failure"));

        final String formatted = SpkError.format(denial);
        ok(formatted.startsWith("[E-1304] "));
        ok(formatted.contains("重新选择"));
        ok(formatted.contains(denial));
        eq("[E-1304] 照片权限已失效，请重新导入", SpkError.shortForm(denial));

        // The native-load branch keeps priority over the generic E-9000 wording.
        ok(SpkError.format("dlopen failed: libspektrafilm_engine.so").contains("引擎库没有加载成功"));
    }

    /**
     * The JNI diagnostic strings are the only signal a bug report carries:
     * engine-create must name the engine's own reason, and the canvas crop must
     * cover the top-left case (a buffer wider *and* taller than the canvas).
     * Read the source because there is no JVM seam over a C++ diagnostic.
     */
    private static void jniContract() {
        final java.nio.file.Path source = java.nio.file.Path.of(
                "app/src/main/cpp/spektralab_jni.cpp");
        final String jni;
        try {
            jni = java.nio.file.Files.readString(source);
        } catch (java.io.IOException e) {
            throw new AssertionError("cannot read " + source + ": " + e);
        }
        ok(jni.contains("spk_engine_create failed: %s"));
        ok(jni.contains("spk_last_error(nullptr)"));
        ok(jni.contains("throw_runtime(env, \"%s\", msg)"));
        ok(!jni.contains("throw_runtime(env, msg)"));
        ok(jni.contains("if (cw <= long(r.width) && ch <= long(r.height))"));
        ok(jni.contains("geometry: cropped to the canvas"));
        ok(!jni.contains("geometry: trimmed to the canvas"));
    }

    private static void eq(String expected, String actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void ok(boolean value) {
        checks++;
        if (!value) throw new AssertionError("expected true");
    }
}

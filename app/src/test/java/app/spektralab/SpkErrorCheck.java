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

        System.out.println("SpkErrorCheck: " + checks + " checks passed");
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

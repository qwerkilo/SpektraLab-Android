package app.spektralab;

import java.lang.reflect.Constructor;

import app.spektralab.engine.SpektraLabEngine;

/**
 * A closed session must refuse before it reaches native code.
 *
 * <p>The engine dereferences the session pointer, so a method that passed a
 * zero handle would die with SIGSEGV in {@code spk_export_source} instead of
 * raising a Java exception. That is the signature recorded while a RAW import
 * closed a session under a preview worker.
 */
public final class SessionCloseCheck {

    private static int checks;

    private SessionCloseCheck() {
    }

    public static void main(String[] args) throws Exception {
        final Constructor<SpektraLabEngine.Session> ctor =
                SpektraLabEngine.Session.class.getDeclaredConstructor(long.class, int.class, int.class);
        ctor.setAccessible(true);
        final SpektraLabEngine.Session closed = ctor.newInstance(0L, 0, 0);

        check(closed.isClosed(), "a zero handle reads as closed");
        checkThrows(() -> closed.exportSource("live"), "exportSource");
        checkThrows(() -> closed.copyArgbRange(new int[1], 0, 1), "copyArgbRange");
        checkThrows(() -> closed.renderBegin("live"), "renderBegin");
        checkThrows(() -> closed.renderBegin("live", true), "renderBegin reprint");
        checkThrows(() -> closed.solve("both"), "solve");
        checkThrows(() -> closed.setParams("{}"), "setParams");
        checkThrows(() -> closed.getParams(), "getParams");
        checkThrows(() -> closed.overscanGeometry(), "overscanGeometry");
        checkThrows(() -> closed.sourceExifJson(), "sourceExifJson");
        checkThrows(() -> closed.suspend(), "suspend");
        checkThrows(() -> closed.pinAutoExposureEv(1.0), "pinAutoExposureEv");
        checkThrows(() -> closed.clearPinAutoExposureEv(), "clearPinAutoExposureEv");
        checkThrows(() -> closed.getDimensions(), "getDimensions");
        checkThrows(() -> closed.progressJson(), "progressJson");
        checkThrows(() -> closed.render("live", 1, 1), "render");
        checkThrows(() -> closed.exportJpeg("x", 90, 0, "live"), "exportJpeg");
        checkThrows(() -> closed.exportTiff("x", "live"), "exportTiff");
        checkThrows(() -> closed.exportDng("x", "live"), "exportDng");
        check(closed.stageSnapshot() == null, "stageSnapshot is null when closed");

        closed.close();
        check(closed.isClosed(), "close on a closed session is idempotent");

        System.out.println("SessionCloseCheck: " + checks + " checks passed");
    }

    private static void check(boolean ok, String name) {
        ++checks;
        if (!ok) {
            throw new AssertionError(name);
        }
    }

    private static void checkThrows(Runnable call, String name) {
        ++checks;
        try {
            call.run();
        } catch (IllegalStateException expected) {
            return;
        } catch (Throwable other) {
            throw new AssertionError(name + " threw " + other, other);
        }
        throw new AssertionError(name + " reached native code on a closed session");
    }
}

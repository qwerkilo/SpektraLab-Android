package app.spektralab;

import app.spektralab.core.RawOpenPlan;
import app.spektralab.engine.SpektraLabEngine;
import app.spektralab.importer.RawOpener;
import app.spektralab.importer.RawSolver;

/** The RAW open plan and the injected engine call. */
public final class RawOpenCheck {

    private static int checks;

    private RawOpenCheck() {
    }

    public static void main(String[] args) {
        final RawOpenPlan capped = RawOpenPlan.of(false, 55_200_000, 12000);
        check(capped.maxPixels == 55_200_000, "capped pixels");
        check(capped.longEdge == 12000, "capped edge");

        final RawOpenPlan nativeSize = RawOpenPlan.of(true, 55_200_000, 12000);
        check(nativeSize.maxPixels == 0, "native pixels");
        check(nativeSize.longEdge == 0, "native edge");

        final int[] seen = new int[2];
        final SpektraLabEngine.Session session = RawOpener.open((path, params, maxPixels, longEdge) -> {
            seen[0] = maxPixels;
            seen[1] = longEdge;
            return null;
        }, "frame.dng", "{}", capped);
        check(session == null, "fake session");
        check(seen[0] == 55_200_000, "engine saw pixels");
        check(seen[1] == 12000, "engine saw edge");

        RawOpener.open((path, params, maxPixels, longEdge) -> {
            seen[0] = maxPixels;
            seen[1] = longEdge;
            return null;
        }, "frame.dng", "{}", nativeSize);
        check(seen[0] == 0 && seen[1] == 0, "engine saw native size");

        final String[] mode = new String[1];
        RawSolver.solve(value -> {
            mode[0] = value;
            return "solved";
        }, "both");
        check("both".equals(mode[0]), "solver mode");

        RawSolver.solve(value -> {
            throw new IllegalStateException("engine refused");
        }, "both");
        check(true, "a solve failure does not escape");

        System.out.println("RawOpenCheck: " + checks + " checks passed");
    }

    private static void check(boolean ok, String name) {
        ++checks;
        if (!ok) {
            throw new AssertionError(name);
        }
    }
}

package app.spektralab.importer;

import app.spektralab.DebugSink;

/** Solve the RAW print exposure/filter pack once, never failing the import. */
public final class RawSolver {

    /** The engine call, injected so the failure policy is testable. */
    public interface Solver {
        String solve(String mode);
    }

    private RawSolver() {
    }

    /**
     * A solve failure must not fail the import: the first preview still renders
     * with the session's defaults.
     */
    public static void solve(Solver solver, String mode) {
        try {
            final String solved = solver.solve(mode);
            DebugSink.text("solve", solved == null ? "(null)" : solved);
        } catch (Throwable se) {
            DebugSink.text("solve-failed", String.valueOf(se));
        }
    }
}

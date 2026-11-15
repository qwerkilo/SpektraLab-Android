package app.spektralab;

import app.spektralab.core.ImportGate;

/** One RAW import at a time. */
public final class ImportGateCheck {

    private static int checks;

    private ImportGateCheck() {
    }

    public static void main(String[] args) {
        final ImportGate gate = new ImportGate();
        check(!gate.isRunning(), "starts idle");
        check(gate.tryStart(), "first start wins");
        check(gate.isRunning(), "running after start");
        check(!gate.tryStart(), "second start refused");
        gate.finish();
        check(!gate.isRunning(), "idle after finish");
        check(gate.tryStart(), "start after finish wins");
        gate.finish();
        gate.finish();
        check(!gate.isRunning(), "finish is idempotent");

        System.out.println("ImportGateCheck: " + checks + " checks passed");
    }

    private static void check(boolean ok, String name) {
        ++checks;
        if (!ok) {
            throw new AssertionError(name);
        }
    }
}

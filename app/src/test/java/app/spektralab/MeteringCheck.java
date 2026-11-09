package app.spektralab;

import app.spektralab.core.Metering;

/** Metering ids, labels and sanitisation. */
public final class MeteringCheck {

    private static int checks;

    private MeteringCheck() {
    }

    public static void main(String[] args) {
        ok("off is valid", Metering.sanitize("").isEmpty());
        ok("balanced survives", "balanced".equals(Metering.sanitize("balanced")));
        ok("unknown falls back to balanced",
                "balanced".equals(Metering.sanitize("spot")));
        ok("null is off", Metering.sanitize(null).isEmpty());
        ok("the first row is off", Metering.indexOf("") == 0);
        ok("protect highlights has an index",
                Metering.indexOf("protect_highlights") > 0);
        ok("index and id agree",
                "protect_shadows".equals(Metering.IDS[Metering.indexOf("protect_shadows")]));
        ok("the label follows the id",
                "保护高光".equals(Metering.name("protect_highlights")));
        ok("off has a label", "关闭".equals(Metering.name("")));
        ok("every id has a name", Metering.IDS.length == Metering.NAMES.length);
        ok("every id has a note", Metering.IDS.length == Metering.NOTES.length);
        ok("unknown names read as balanced",
                "平衡".equals(Metering.name("SPOT")));

        System.out.println("MeteringCheck: " + checks + " checks passed");
    }

    private static void ok(String what, boolean value) {
        ++checks;
        if (!value) {
            throw new AssertionError(what);
        }
    }
}

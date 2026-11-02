package app.spektralab;

/**
 * Pure JVM contract for the Scale/Edge seam: every shipped Gate carries the
 * long edge that belongs to it, a Scale always selects that Gate, and a pair
 * that disagrees is rejected instead of quietly rendered.
 */
public final class GateScaleCheck {
    private static int checks = 0;

    public static void main(String[] args) {
        shippedGates();
        everyShippedScaleSelectsItsGate();
        mismatchedPairsAreRejected();
        legacyScaleValuesNormaliseToMeasuredGate();
        frameAspectFiltersGates();

        System.out.println("GateScaleCheck: " + checks + " checks passed");
    }

    private static void shippedGates() {
        final java.util.List<GateScale.Gate> gates = GateScale.gates();
        require("the engine ships ten gates", gates.size() == 10);
        require("the first gate is 135", "135".equals(gates.get(0).id));
        require("the last gate is 6x17", "120_6x17".equals(gates.get(9).id));
        require("6x8 is one of the shipped gates", GateScale.byId("120_6x8") != null);
        for (GateScale.Gate gate : gates) {
            require("gate " + gate.id + " has an id", gate.id != null && gate.id.length() > 0);
            require("gate " + gate.id + " has a label", gate.label != null && gate.label.length() > 0);
            require("gate " + gate.id + " has a long edge", gate.longEdgeMm > 0f);
            require("gate " + gate.id + " has an aspect", gate.aspect > 0.0);
            require("gate " + gate.id + " resolves by id", GateScale.byId(gate.id) == gate);
        }
    }

    /** Every Scale choice is a shipped Gate, and selecting it selects that Gate. */
    private static void everyShippedScaleSelectsItsGate() {
        for (GateScale.Gate gate : GateScale.gates()) {
            require("scale " + gate.longEdgeMm + "mm belongs to " + gate.id,
                    GateScale.pairIsValid(gate.id, gate.longEdgeMm));
            final GateScale.Gate resolved = GateScale.resolve(gate.id, gate.longEdgeMm);
            require("scale " + gate.longEdgeMm + "mm selects " + gate.id,
                    resolved != null && gate.id.equals(resolved.id));
            require("scale " + gate.longEdgeMm + "mm keeps its gate's own edge",
                    Math.abs(resolved.longEdgeMm - gate.longEdgeMm) < 0.001f);
            final GateScale.Gate byEdge = GateScale.forLongEdge(gate.longEdgeMm);
            require("scale " + gate.longEdgeMm + "mm finds a gate with that edge",
                    byEdge != null && Math.abs(byEdge.longEdgeMm - gate.longEdgeMm) < 0.001f);
        }
    }

    private static void mismatchedPairsAreRejected() {
        require("6x17 is not a 36 mm gate", !GateScale.pairIsValid("120_6x17", 36f));
        require("135 is not a 56 mm gate", !GateScale.pairIsValid("135", 56f));
        require("an unknown gate is not a pair", !GateScale.pairIsValid("120_made_up", 36f));
        require("a missing gate falls back to the edge's gate",
                "135".equals(GateScale.resolve("120_6x17", 36f).id));
        require("a null gate falls back to the edge's gate",
                "135".equals(GateScale.resolve(null, 36f).id));
    }

    private static void legacyScaleValuesNormaliseToMeasuredGate() {
        // The old Scale page rounded these; the Gate's measured edge wins.
        require("6x7's measured edge accepts the old 70 mm choice",
                GateScale.pairIsValid("120_6x7", 70f));
        final GateScale.Gate sixBySeven = GateScale.resolve("120_6x7", 70f);
        require("6x7 resolves to its measured 69.5 mm edge",
                "120_6x7".equals(sixBySeven.id) && Math.abs(sixBySeven.longEdgeMm - 69.5f) < 0.001f);
        require("6x12's 120 mm choice is not its measured edge",
                !GateScale.pairIsValid("120_6x12", 120f));
        final GateScale.Gate sixByTwelve = GateScale.resolve("120_6x12", 120f);
        require("6x12 still resolves to 6x12", "120_6x12".equals(sixByTwelve.id));
        require("6x12 normalises to 112 mm", Math.abs(sixByTwelve.longEdgeMm - 112f) < 0.001f);
        require("6x17's 170 mm choice is not its measured edge",
                !GateScale.pairIsValid("120_6x17", 170f));
        require("6x17 normalises to 168 mm",
                Math.abs(GateScale.resolve("120_6x17", 170f).longEdgeMm - 168f) < 0.001f);
    }

    private static void frameAspectFiltersGates() {
        require("the full catalogue is offered before a Frame exists",
                GateScale.available(0, 0).size() == 10);

        final java.util.List<GateScale.Gate> square = GateScale.available(1000, 1000);
        require("a square Frame offers one gate", square.size() == 1);
        require("a square Frame offers 6x6", "120_6x6".equals(square.get(0).id));

        final java.util.List<GateScale.Gate> sixBySeventeen = GateScale.available(6000, 2000);
        require("a 3:1 Frame offers one gate", sixBySeventeen.size() == 1);
        require("a 3:1 Frame offers 6x17", "120_6x17".equals(sixBySeventeen.get(0).id));

        final java.util.List<GateScale.Gate> sixByFortyFive = GateScale.available(5600, 4150);
        require("a 645 Frame offers 645", contains(sixByFortyFive, "120_645"));
        // 135 half is 1.333 and 645 is 1.349; the engine's 5 % rule accepts both.
        require("a 645 Frame also fits 135 half", contains(sixByFortyFive, "135_half"));
        require("a 645 Frame does not become a square", !contains(sixByFortyFive, "120_6x6"));

        final java.util.List<GateScale.Gate> threeByTwo = GateScale.available(6000, 4000);
        require("a 3:2 Frame offers 135", contains(threeByTwo, "135"));
        require("a 3:2 Frame offers 6x9", contains(threeByTwo, "120_6x9"));
        require("a 3:2 Frame does not offer 6x6", !contains(threeByTwo, "120_6x6"));
    }

    private static boolean contains(java.util.List<GateScale.Gate> gates, String id) {
        for (GateScale.Gate gate : gates) if (id.equals(gate.id)) return true;
        return false;
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

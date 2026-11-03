package app.spektralab;

/**
 * Pure JVM contract for the Rebate page's own control set.
 *
 * <p>The page, the Look, and the engine have to agree about which mode, gate,
 * holes, and carrier ids exist, what the sliders' ranges are, and which Frames
 * can carry a rebate at all. None of that may live only inside a view, because
 * a theme rebuild or a device check cannot then prove it.
 */
public final class RebateControlsCheck {
    private static int checks;

    public static void main(String[] args) {
        choices();
        labelFallback();
        seedAndRangeClamps();
        gateEligibility();

        System.out.println("RebateControlsCheck: " + checks + " checks passed");
    }

    private static void choices() {
        require("two rebate modes", RebateControls.MODES.size() == 2);
        require("strip is a mode id", RebateControls.has(RebateControls.MODES, "strip"));
        require("filed is a mode id", RebateControls.has(RebateControls.MODES, "filed"));

        require("six gate shapes", RebateControls.GATE_STYLES.size() == 6);
        require("auto is a gate shape", RebateControls.has(RebateControls.GATE_STYLES, "auto"));
        require("kicked is a gate shape", RebateControls.has(RebateControls.GATE_STYLES, "kicked"));

        require("two hole choices", RebateControls.HOLES.size() == 2);
        require("white holes are a choice", RebateControls.has(RebateControls.HOLES, "white"));
        require("black holes are a choice", RebateControls.has(RebateControls.HOLES, "black"));

        require("three carrier choices", RebateControls.CARRIERS.size() == 3);
        require("no carrier is a choice", RebateControls.has(RebateControls.CARRIERS, "none"));
        require("open carrier is a choice", RebateControls.has(RebateControls.CARRIERS, "open"));

        // Every default in Look is one of the page's real ids, not a label that
        // only the page happens to understand.
        final Look look = new Look();
        require("default mode is in the mode list",
                RebateControls.has(RebateControls.MODES, look.edgeMode));
        require("default gate shape is in the gate list",
                RebateControls.has(RebateControls.GATE_STYLES, look.edgeGate));
        require("default holes are in the hole list",
                RebateControls.has(RebateControls.HOLES, look.edgeHoles));
        require("default carrier is in the carrier list",
                RebateControls.has(RebateControls.CARRIERS, look.edgeCarrier));

        for (String[] pair : new String[][]{
                ids(RebateControls.MODES), ids(RebateControls.GATE_STYLES),
                ids(RebateControls.HOLES), ids(RebateControls.CARRIERS)}) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String id : pair) {
                require("id is not empty", id != null && id.length() > 0);
                require("id is unique: " + id, seen.add(id));
            }
        }
    }

    private static void labelFallback() {
        require("strip has its label",
                "整条 strip".equals(RebateControls.labelOf(RebateControls.MODES, "strip")));
        // A Look from a newer build may name an id this build does not offer:
        // showing the id beats showing a different mode's label.
        require("an unknown mode shows its own id",
                "future_mode".equals(RebateControls.labelOf(RebateControls.MODES, "future_mode")));
        require("an unknown mode has no index",
                RebateControls.indexOf(RebateControls.MODES, "future_mode") == -1);
        require("a known mode has its catalogue index",
                RebateControls.indexOf(RebateControls.MODES, "filed") == 1);
    }

    private static void seedAndRangeClamps() {
        require("fog below zero clamps", RebateControls.clampFog(-0.5f) == 0f);
        require("fog above four clamps", RebateControls.clampFog(9f) == 4f);
        require("fog keeps a real value", RebateControls.clampFog(2.25f) == 2.25f);

        require("leaks below zero clamps", RebateControls.clampLeaks(-1f) == 0f);
        require("leaks above four clamps", RebateControls.clampLeaks(4.2f) == 4f);
        require("leaks keep a real value", RebateControls.clampLeaks(0.75f) == 0.75f);

        require("frame number below zero clamps", RebateControls.clampFrameNumber(-1f) == 0f);
        require("frame number above ninety-nine clamps",
                RebateControls.clampFrameNumber(120f) == 99f);
        require("frame number is whole", RebateControls.clampFrameNumber(12.4f) == 12f);
        require("frame number rounds up", RebateControls.clampFrameNumber(12.6f) == 13f);

        require("f-number below zero clamps", RebateControls.clampFNumber(-2f) == 0f);
        require("f-number above sixty-four clamps", RebateControls.clampFNumber(100f) == 64f);
        require("5.6 is a real f-number", RebateControls.clampFNumber(5.6f) == 5.6f);

        require("a negative seed clamps to zero", RebateControls.clampSeed(-5L) == 0);
        require("a seed above the int range clamps to the maximum",
                RebateControls.clampSeed(3_000_000_000L) == Integer.MAX_VALUE);
        require("a real seed survives", RebateControls.clampSeed(42L) == 42);
    }

    private static void gateEligibility() {
        require("a 3:2 Frame can carry a rebate", GateScale.canHostRebate(6000, 4000));
        require("a square Frame can carry a rebate", GateScale.canHostRebate(1000, 1000));
        require("before a Frame exists every Gate is offered", GateScale.canHostRebate(0, 0));
        // 4:1 is outside every shipped Gate's 5 % shape rule.
        require("a 4:1 Frame has no Gate", GateScale.available(4000, 1000).isEmpty());
        require("a 4:1 Frame cannot carry a rebate", !GateScale.canHostRebate(4000, 1000));
    }

    private static String[] ids(java.util.List<RebateControls.Choice> choices) {
        String[] out = new String[choices.size()];
        for (int i = 0; i < choices.size(); ++i) out[i] = choices.get(i).id;
        return out;
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

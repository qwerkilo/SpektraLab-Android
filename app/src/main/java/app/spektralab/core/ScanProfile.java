package app.spektralab.core;

/**
 * The scanner preset a Frame's Look is on.
 *
 * <p>The first slice models the Fujifilm Frontier SP-3000 only through the two
 * scanner correction switches the engine already exposes. The preset therefore
 * has no independent state: it is the named combination of those switches, and
 * {@link #of(boolean, boolean)} is the single rule that decides which preset a
 * Look currently matches. A Look that turns one switch off is simply off, not a
 * half-applied SP-3000.
 *
 * <p>The label says model on purpose: no measured SP-3000 lens, LED spectrum or
 * tone curve is shipped, so this must never be presented as a calibration.
 */
public enum ScanProfile {
    OFF("off", "\u5173\u95ed", false, false),
    SP_3000("sp3000", "SP-3000\uff08\u6a21\u578b\uff09", true, true);

    /** Stable id saved with a Look. */
    public final String wire;
    /** The visible name a chip and TalkBack announce. */
    public final String label;
    /** Whether the preset turns the scanner white correction on. */
    public final boolean whiteCorrection;
    /** Whether the preset turns the scanner black correction on. */
    public final boolean blackCorrection;

    ScanProfile(String wire, String label, boolean whiteCorrection, boolean blackCorrection) {
        this.wire = wire;
        this.label = label;
        this.whiteCorrection = whiteCorrection;
        this.blackCorrection = blackCorrection;
    }

    /** The preset a saved id names; an unknown id is {@link #OFF}. */
    public static ScanProfile fromWire(String wire) {
        if (wire != null) {
            for (ScanProfile profile : values()) {
                if (profile.wire.equals(wire)) {
                    return profile;
                }
            }
        }
        return OFF;
    }

    /** The preset those two switches currently match. */
    public static ScanProfile of(boolean whiteOn, boolean blackOn) {
        return whiteOn && blackOn ? SP_3000 : OFF;
    }
}

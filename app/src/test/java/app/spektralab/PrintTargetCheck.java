package app.spektralab;

import app.spektralab.core.PrintTarget;

import java.util.Arrays;
import java.util.List;

/**
 * Pure JVM contract for the catalogue's declared paper.
 *
 * <p>A film record names the paper it prints on (`targetPrint`). Selecting that
 * film moves the Look onto the paper; the profile's legacy `target_print` is
 * the same fact under the older spelling. A declaration the catalogue does not
 * carry, or an empty one, must leave the user's paper alone.
 */
public final class PrintTargetCheck {
    private static int checks;

    private static final List<String> PRINTS = Arrays.asList(
            "kodak_2383", "kodak_portra_endura", "ilford_multigrade_rc");

    public static void main(String[] args) {
        declared();
        choose();

        System.out.println("PrintTargetCheck: " + checks + " checks passed");
    }

    /** The record's own name wins; the legacy key is the fallback. */
    private static void declared() {
        eq("kodak_2383", PrintTarget.declared("kodak_2383", "kodak_portra_endura"));
        eq("kodak_2383", PrintTarget.declared(null, "kodak_2383"));
        eq("kodak_2383", PrintTarget.declared("", "kodak_2383"));
        eq("kodak_2383", PrintTarget.declared("  ", " kodak_2383 "));
        eq(null, PrintTarget.declared(null, null));
        eq(null, PrintTarget.declared("", ""));
        eq(null, PrintTarget.declared("   ", "\t"));
    }

    /** A film selection moves the paper to the declared one, when it exists. */
    private static void choose() {
        // CineStill 800T and the Vision3 aliases: the record declares 2383.
        eq("kodak_2383", PrintTarget.choose("kodak_portra_endura", "kodak_2383", PRINTS));
        // An explicit paper survives a film whose profile declares nothing.
        eq("ilford_multigrade_rc", PrintTarget.choose(
                "ilford_multigrade_rc", null, PRINTS));
        // A declaration the shipped catalogue has no record for is not applied.
        eq("kodak_portra_endura", PrintTarget.choose(
                "kodak_portra_endura", "kodak_paper_missing", PRINTS));
        // No catalogue yet: the paper the user already has is the honest answer.
        eq("kodak_portra_endura", PrintTarget.choose(
                "kodak_portra_endura", "kodak_2383", null));
        // No selection yet: a declared paper is still a valid start.
        eq("kodak_2383", PrintTarget.choose(null, "kodak_2383", PRINTS));
    }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }
}

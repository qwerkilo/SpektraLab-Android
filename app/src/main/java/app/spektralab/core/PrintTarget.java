package app.spektralab.core;

import java.util.Collection;

/**
 * The paper a Film stock declares it prints on.
 *
 * <p>The catalogue names it `targetPrint`; a profile may also carry the older
 * `target_print` spelling, which the generator copies through. CineStill 800T
 * and the Vision3 negatives are aliases whose profile is a stub, so their
 * record is the only place the declaration exists.
 *
 * <p>Precedence, following upstream's `selectFilmStock`: a Film the user picks
 * moves the paper to that film's declared paper, overriding the paper that was
 * there. A paper the user picks afterwards stays until the next Film selection.
 * Restoring a Frame's Look restores its stored pair and does not re-apply the
 * declaration, so A → B → A gives back A's paper.
 */
public final class PrintTarget {
    private PrintTarget() { }

    /** The record's declaration: `targetPrint`, then the legacy spelling. */
    public static String declared(String targetPrint, String legacyTargetPrint) {
        final String current = trimmed(targetPrint);
        return current != null ? current : trimmed(legacyTargetPrint);
    }

    /**
     * The paper a Film selection should leave the Look on: the declared paper
     * when the shipped catalogue has a record for it, otherwise the paper the
     * user already had.
     */
    public static String choose(String currentPrint, String declared,
                                Collection<String> printIds) {
        if (declared == null || declared.isEmpty()) {
            return currentPrint;
        }
        if (printIds == null || !printIds.contains(declared)) {
            return currentPrint;
        }
        return declared;
    }

    private static String trimmed(String value) {
        if (value == null) {
            return null;
        }
        final String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}

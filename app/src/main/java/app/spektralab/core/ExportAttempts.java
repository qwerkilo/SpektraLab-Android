package app.spektralab.core;

/**
 * The export retry policy: render once, and retry once when the written JPEG
 * does not correlate with the canvas.
 *
 * <p>TIFF and DNG skip the check because the comparator only reads JPEG. The
 * temp file is discarded only between attempts, never after the final one.
 */
public final class ExportAttempts {

    /** One engine render and the temp file it wrote. */
    public interface Render {
        Rendered render();

        void discard();
    }

    /** Canvas correlation for the file the last render wrote; -1 unreadable. */
    public interface Ncc {
        double score();
    }

    /** Optional diagnostics; the policy never formats UI text. */
    public interface Observer {
        default void accepted(double ncc) {
        }

        default void suspect(double ncc, int attempt, Rendered rendered) {
        }
    }

    /** The engine's size/render-time triple and the canvas it drew. */
    public static final class Rendered {
        public final int[] info;
        public final String canvas;

        public Rendered(int[] info, String canvas) {
            this.info = info;
            this.canvas = canvas;
        }
    }

    /** What the caller reports. */
    public static final class Outcome {
        public final Rendered rendered;
        public final boolean suspect;
        public final int attempts;

        Outcome(Rendered rendered, boolean suspect, int attempts) {
            this.rendered = rendered;
            this.suspect = suspect;
            this.attempts = attempts;
        }
    }

    private ExportAttempts() {
    }

    public static Outcome run(boolean tiff, boolean dng, Render render, Ncc ncc,
                              Observer observer) {
        Outcome outcome = null;
        for (int attempt = 0; attempt < 2; ++attempt) {
            final Rendered rendered = render.render();
            outcome = new Outcome(rendered, false, attempt + 1);
            if (tiff || dng) {
                return outcome;
            }
            final double score = ncc.score();
            if (!ExportMatch.suspect(score)) {
                if (observer != null) {
                    observer.accepted(score);
                }
                return outcome;
            }
            outcome = new Outcome(rendered, true, attempt + 1);
            if (observer != null) {
                observer.suspect(score, attempt, rendered);
            }
            if (attempt == 1) {
                return outcome;
            }
            render.discard();
        }
        return outcome;
    }
}

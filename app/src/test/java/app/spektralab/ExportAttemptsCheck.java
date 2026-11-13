package app.spektralab;

import java.util.ArrayList;
import java.util.List;

import app.spektralab.core.ExportAttempts;

/** Retry/suspect policy for the export self-check. */
public final class ExportAttemptsCheck {

    private static int checks;

    private ExportAttemptsCheck() {
    }

    public static void main(String[] args) {
        final List<String> calls = new ArrayList<>();
        final ExportAttempts.Rendered first =
                new ExportAttempts.Rendered(new int[]{100, 50, 7}, "100x50");

        final ExportAttempts.Outcome clean = ExportAttempts.run(false, false,
                renderer(calls, first), () -> 0.9, null);
        require("clean first attempt", !clean.suspect && clean.attempts == 1);
        require("clean does not discard", calls.equals(List.of("render")));
        require("clean keeps info", clean.rendered.info[0] == 100
                && "100x50".equals(clean.rendered.canvas));

        calls.clear();
        final List<Double> scores = List.of(0.10, 0.90);
        final List<Double> accepted = new ArrayList<>();
        final List<Integer> suspects = new ArrayList<>();
        final ExportAttempts.Outcome retried = ExportAttempts.run(false, false,
                renderer(calls, first), sequence(scores),
                new ExportAttempts.Observer() {
                    @Override public void accepted(double ncc) {
                        accepted.add(ncc);
                    }

                    @Override public void suspect(double ncc, int attempt,
                                                  ExportAttempts.Rendered rendered) {
                        suspects.add(attempt);
                    }
                });
        require("retry ends clean", !retried.suspect && retried.attempts == 2);
        require("retry discards once", calls.equals(List.of("render", "discard", "render")));
        require("only the second score is accepted", accepted.equals(List.of(0.90)));
        require("first attempt is named suspect", suspects.equals(List.of(0)));

        calls.clear();
        final ExportAttempts.Outcome twice = ExportAttempts.run(false, false,
                renderer(calls, first), () -> 0.10, null);
        require("two suspects stay suspect", twice.suspect && twice.attempts == 2);
        require("no discard after the final attempt",
                calls.equals(List.of("render", "discard", "render")));

        calls.clear();
        final int[] nccCalls = {0};
        final ExportAttempts.Outcome tiff = ExportAttempts.run(true, false,
                renderer(calls, first), () -> {
                    ++nccCalls[0];
                    return 0.0;
                }, null);
        require("TIFF skips the comparator", !tiff.suspect && nccCalls[0] == 0);
        require("TIFF renders once", calls.equals(List.of("render")));

        calls.clear();
        final ExportAttempts.Outcome dng = ExportAttempts.run(false, true,
                renderer(calls, first), () -> {
                    throw new AssertionError("DNG must not run the NFC check");
                }, null);
        require("DNG skips the comparator", !dng.suspect && dng.attempts == 1);

        System.out.println("ExportAttemptsCheck: " + checks + " checks passed");
    }

    private static ExportAttempts.Render renderer(List<String> calls,
                                                 ExportAttempts.Rendered rendered) {
        return new ExportAttempts.Render() {
            @Override public ExportAttempts.Rendered render() {
                calls.add("render");
                return rendered;
            }

            @Override public void discard() {
                calls.add("discard");
            }
        };
    }

    private static ExportAttempts.Ncc sequence(List<Double> scores) {
        final int[] at = {0};
        return () -> scores.get(at[0]++);
    }

    private static void require(String name, boolean ok) {
        ++checks;
        if (!ok) {
            throw new AssertionError(name);
        }
    }
}

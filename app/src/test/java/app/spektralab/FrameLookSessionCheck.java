package app.spektralab;

/** Contract for the per-Frame live Look and its undo history. */
public final class FrameLookSessionCheck {
    private static int checks;

    public static void main(String[] args) {
        final Look initial = new Look();
        initial.filmStock = "kodak_portra_400";
        final float exposure = initial.get(OpticsDelta.Param.EXPOSURE);

        final FrameLookSession session = new FrameLookSession(initial);
        ok("a new session starts on the supplied Look",
                "kodak_portra_400".equals(session.look().filmStock));
        ok("a new session has no undo step", !session.canUndo());
        eq(1, session.size());

        session.edit(look -> look.set(OpticsDelta.Param.EXPOSURE, exposure + 0.4f));
        near(exposure + 0.4f, session.look().get(OpticsDelta.Param.EXPOSURE));
        ok("a settled edit is undoable", session.canUndo());
        eq(2, session.size());

        final Look restored = session.undo();
        near(exposure, restored.get(OpticsDelta.Param.EXPOSURE));
        near(exposure, session.look().get(OpticsDelta.Param.EXPOSURE));
        ok("the first state is not undoable", !session.canUndo());
        ok("undo past the first state is null", session.undo() == null);

        session.edit(look -> look.set(OpticsDelta.Param.EXPOSURE, exposure + 0.2f));
        final int beforeNoop = session.size();
        session.edit(look -> look.set(OpticsDelta.Param.EXPOSURE, exposure + 0.2f));
        eq(beforeNoop, session.size());

        final Look replacement = new Look();
        replacement.filmStock = "kodak_ektar_100";
        session.reset(replacement);
        eq("kodak_ektar_100", session.look().filmStock);
        ok("reset clears the history", !session.canUndo());
        eq(1, session.size());

        final FrameLookSession other = new FrameLookSession(initial);
        other.edit(look -> look.set(OpticsDelta.Param.EXPOSURE, exposure + 1.0f));
        near(exposure, session.look().get(OpticsDelta.Param.EXPOSURE));
        ok("two Frames do not share a Look",
                !session.look().toJson().toString().equals(other.look().toJson().toString()));

        System.out.println("FrameLookSessionCheck: " + checks + " checks passed");
    }

    private static void eq(Object expected, Object actual) {
        ++checks;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void near(float expected, float actual) {
        ++checks;
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void ok(String what, boolean value) {
        ++checks;
        if (!value) throw new AssertionError(what);
    }
}

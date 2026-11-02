package app.spektralab;

/**
 * Pure JVM contract for the Console's width decision: the phone shell below
 * the tablet breakpoint, two columns at and above it, and a control column
 * that keeps the Frame primary instead of swallowing the window.
 *
 * <p>No JUnit, no Android: run it with the same javac/java that builds the app.
 */
public final class ConsoleLayoutCheck {
    private static int checks = 0;

    public static void main(String[] args) {
        breakpoint();
        phoneWidths();
        wideWidths();
        everyWideWidthFits();
        everyPhoneWidthStaysOneColumn();
        System.out.println("ConsoleLayoutCheck: " + checks + " checks passed");
    }

    /** The ticket names 360, 390, 599, 600, and 800 dp explicitly. */
    private static void breakpoint() {
        require("360 dp is the phone shell", !ConsoleLayout.isWide(360));
        require("390 dp is the phone shell", !ConsoleLayout.isWide(390));
        require("599 dp is still the phone shell", !ConsoleLayout.isWide(599));
        require("600 dp starts the two-column Console", ConsoleLayout.isWide(600));
        require("800 dp is the two-column Console", ConsoleLayout.isWide(800));
    }

    private static void phoneWidths() {
        for (int width : new int[]{320, 360, 390, 480, 599}) {
            require(width + " dp controls use the full width",
                    ConsoleLayout.controlColumnWidthDp(width) == width);
            require(width + " dp stage uses the full width",
                    ConsoleLayout.stageColumnWidthDp(width) == width);
        }
    }

    private static void wideWidths() {
        require("600 dp control column is 300 dp",
                ConsoleLayout.controlColumnWidthDp(600) == 300);
        require("600 dp stage column is 300 dp",
                ConsoleLayout.stageColumnWidthDp(600) == 300);
        // 800 * 0.40 = 320, the middle of the 300..400 clamp.
        require("800 dp control column is 320 dp",
                ConsoleLayout.controlColumnWidthDp(800) == 320);
        require("800 dp stage column is 480 dp",
                ConsoleLayout.stageColumnWidthDp(800) == 480);
        require("960 dp landscape control column is 384 dp",
                ConsoleLayout.controlColumnWidthDp(960) == 384);
        require("960 dp landscape stage column is 576 dp",
                ConsoleLayout.stageColumnWidthDp(960) == 576);
        require("wide column never exceeds its 400 dp cap",
                ConsoleLayout.controlColumnWidthDp(1400) == 400);
    }

    private static void everyWideWidthFits() {
        int previous = -1;
        for (int width = ConsoleLayout.WIDE_BREAKPOINT_DP; width <= 1400; ++width) {
            final int control = ConsoleLayout.controlColumnWidthDp(width);
            final int stage = ConsoleLayout.stageColumnWidthDp(width);
            require(width + " dp two columns sum to the window", control + stage == width);
            require(width + " dp stage keeps its minimum",
                    stage >= ConsoleLayout.STAGE_MIN_DP);
            require(width + " dp controls keep their minimum",
                    control >= ConsoleLayout.CONTROL_MIN_DP);
            require(width + " dp controls stay inside their maximum",
                    control <= ConsoleLayout.CONTROL_MAX_DP);
            require(width + " dp control column does not shrink as the window grows",
                    previous < 0 || control >= previous);
            previous = control;
        }
    }

    private static void everyPhoneWidthStaysOneColumn() {
        for (int width = 1; width < ConsoleLayout.WIDE_BREAKPOINT_DP; ++width) {
            require(width + " dp is one column", !ConsoleLayout.isWide(width));
            require(width + " dp phone controls are full width",
                    ConsoleLayout.controlColumnWidthDp(width) == width);
        }
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

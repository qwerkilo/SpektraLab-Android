package app.spektralab;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Guards every choice list against losing its UI row when a panel builder is split.
 *
 * <p>The governance refactor dropped the Date Back corner and style rows while the
 * summary text still referenced both lists. A plain source search would miss that
 * because the summary keeps the constant alive. This check requires each public
 * choice list to appear inside an {@code edgeChoiceRow(...)} call.
 *
 * <p>It is deliberately source-level: the project has no instrumentation runner,
 * and constructing these panels needs the Android runtime.
 */
public final class ChoiceRowCoverageCheck {
    private static final Class<?>[] CONTAINERS = {DateBack.class, RebateControls.class};
    private static final String SOURCE = "app/src/main/java/app/spektralab/MainActivity.java";

    private ChoiceRowCoverageCheck() {
    }

    public static void main(String[] args) throws Exception {
        final String source = Files.readString(Path.of(SOURCE));
        int checks = 0;
        for (Class<?> container : CONTAINERS) {
            for (Field field : container.getDeclaredFields()) {
                if (!isChoiceList(field)) {
                    continue;
                }
                final String reference = container.getSimpleName() + "." + field.getName();
                final Pattern row = Pattern.compile(
                        "edgeChoiceRow\\([^;]{0,600}?" + Pattern.quote(reference),
                        Pattern.DOTALL);
                if (!row.matcher(source).find()) {
                    throw new AssertionError("no edgeChoiceRow builds " + reference);
                }
                checks++;
            }
        }
        System.out.println("ChoiceRowCoverageCheck: " + checks + " checks passed");
    }

    private static boolean isChoiceList(Field field) {
        final int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers)
                && Modifier.isStatic(modifiers)
                && List.class.isAssignableFrom(field.getType());
    }
}

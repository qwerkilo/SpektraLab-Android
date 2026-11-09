package app.spektralab;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guards the Material 3 control rule at the source level: the composition root
 * must not construct a framework interactive widget where a Material component
 * exists. The scanner is exercised on synthetic source so a broken regex does
 * not silently pass.
 */
public final class M3ComponentCheck {

    private static final Path ACTIVITY =
            Path.of("app", "src", "main", "java", "app", "spektralab", "MainActivity.java");
    private static final Pattern FRAMEWORK_WIDGET = Pattern.compile(
            "\\bnew\\s+(?:android\\.widget\\.)?"
            + "(Button|CheckBox|RadioButton|Switch|SeekBar|EditText|Spinner|"
            + "ImageButton|ToggleButton|RatingBar|NumberPicker|AutoCompleteTextView|"
            + "CheckedTextView|ProgressBar)\\b");
    private static final String MATERIAL_TEXT_FIELD =
            "new com.google.android.material.textfield.TextInputEditText(";

    private static int checks;

    private M3ComponentCheck() {
    }

    public static void main(String[] args) throws Exception {
        final String source = Files.readString(ACTIVITY, StandardCharsets.UTF_8);
        final List<String> violations = violations(source);
        ok("MainActivity constructs no framework interactive widget"
                + (violations.isEmpty() ? "" : ": " + violations), violations.isEmpty());
        ok("the scanner catches a framework Button",
                !violations("new android.widget.Button(c);").isEmpty());
        ok("the scanner catches a framework EditText",
                !violations("new EditText(c);").isEmpty());
        ok("MaterialButton is not a violation",
                violations("new MaterialButton(c);").isEmpty());
        ok("free-text dialogs use TextInputEditText",
                count(source, MATERIAL_TEXT_FIELD) >= 2);

        System.out.println("M3ComponentCheck: " + checks + " checks passed");
    }

    static List<String> violations(String source) {
        final List<String> found = new ArrayList<>();
        final Matcher matcher = FRAMEWORK_WIDGET.matcher(source);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    private static int count(String source, String needle) {
        int count = 0;
        int at = source.indexOf(needle);
        while (at >= 0) {
            ++count;
            at = source.indexOf(needle, at + needle.length());
        }
        return count;
    }

    private static void ok(String what, boolean value) {
        ++checks;
        if (!value) {
            throw new AssertionError(what);
        }
    }
}

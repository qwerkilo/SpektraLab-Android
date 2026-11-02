package app.spektralab;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Pure JVM contract for the Material 3 control tokens.
 *
 * <p>It proves three things the JVM can see without a device: every required
 * M3 color role is defined for both Theme modes (the brand fallback is complete
 * when dynamic colour is unavailable), the fallback pairs clear their contrast
 * thresholds, and neither the stage nor the spectral R/G/B is part of the M3
 * scheme. The values are read from the actual {@code colors.xml} and
 * {@code themes.xml}, so the check guards what the app ships, not a second copy.
 *
 * <p>No JUnit, no Android: run it through {@code ./gradlew checkJvm} from the
 * repository root.
 */
public final class M3TokenCheck {
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        final File res = resourceDir();
        final Map<String, Integer> colors = parseColors(new File(res, "values/colors.xml"));
        final Map<String, Map<String, String>> styles =
                parseStyles(new File(res, "values/themes.xml"));

        final Map<M3Tokens.ColorRole, Integer> light =
                requireTheme("AppTheme", styles.get("AppTheme"), colors);
        final Map<M3Tokens.ColorRole, Integer> dark =
                requireTheme("AppTheme.Dark", styles.get("AppTheme.Dark"), colors);

        requiredRoles();
        contrast(light, "light");
        contrast(dark, "dark");
        distinctModes(light, dark);
        fixedStageAndSpectrum(light, dark);
        typeScale();
        shapes();
        stateLayers();

        System.out.println("M3TokenCheck: " + checks + " checks passed");
    }

    /** Locate the staged source resources from the repository root. */
    private static File resourceDir() {
        final String override = System.getProperty("spektralab.resDir");
        if (override != null && !override.isEmpty()) return new File(override);
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (dir != null) {
            final File candidate = new File(dir, "app/src/main/res");
            if (new File(candidate, "values/themes.xml").isFile()) return candidate;
            dir = dir.getParentFile();
        }
        throw new AssertionError("app/src/main/res not found from "
                + System.getProperty("user.dir") + "; run from the repository root");
    }

    private static void requiredRoles() {
        require("the token table names every M3 role once",
                M3Tokens.ColorRole.values().length == 37);
        for (M3Tokens.ColorRole role : M3Tokens.ColorRole.values()) {
            require(role + " has an M3 attribute name",
                    role.attr.startsWith("color") || role.attr.startsWith("android:color"));
            require(role + " is not a stage or spectrum attribute",
                    !M3Tokens.isExcludedColorAttribute(role.attr));
        }
        require("a stage attribute is recognised as excluded",
                M3Tokens.isExcludedColorAttribute("colorStage"));
        require("a spectrum attribute is recognised as excluded",
                M3Tokens.isExcludedColorAttribute("colorSpectrumRed"));
        require("a control attribute is not excluded",
                !M3Tokens.isExcludedColorAttribute("colorPrimary"));
        require("surface is a surface role", M3Tokens.isSurface(M3Tokens.ColorRole.SURFACE));
        require("primary is not a surface role",
                !M3Tokens.isSurface(M3Tokens.ColorRole.PRIMARY));
    }

    private static Map<M3Tokens.ColorRole, Integer> requireTheme(String name,
            Map<String, String> style, Map<String, Integer> colors) {
        require(name + " style exists", style != null);
        require(name + " style is not empty", style != null && !style.isEmpty());

        final Map<M3Tokens.ColorRole, Integer> resolved =
                new EnumMap<>(M3Tokens.ColorRole.class);
        for (String attr : style.keySet()) {
            require(name + " does not define a stage/spectrum color",
                    !M3Tokens.isExcludedColorAttribute(attr));
        }
        for (M3Tokens.ColorRole role : M3Tokens.ColorRole.values()) {
            final String value = style.get(role.attr);
            require(name + " defines " + role.attr, value != null);
            final int color = resolve(name + " " + role.attr, value, colors);
            require(name + " " + role.attr + " is opaque",
                    ((color >>> 24) & 0xFF) == 0xFF);
            resolved.put(role, color);
        }
        return resolved;
    }

    private static void contrast(Map<M3Tokens.ColorRole, Integer> c, String mode) {
        pair(c, mode, M3Tokens.ColorRole.ON_PRIMARY,
                M3Tokens.ColorRole.PRIMARY, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_PRIMARY_CONTAINER,
                M3Tokens.ColorRole.PRIMARY_CONTAINER, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_SECONDARY,
                M3Tokens.ColorRole.SECONDARY, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_SECONDARY_CONTAINER,
                M3Tokens.ColorRole.SECONDARY_CONTAINER, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_TERTIARY,
                M3Tokens.ColorRole.TERTIARY, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_TERTIARY_CONTAINER,
                M3Tokens.ColorRole.TERTIARY_CONTAINER, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_ERROR,
                M3Tokens.ColorRole.ERROR, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_ERROR_CONTAINER,
                M3Tokens.ColorRole.ERROR_CONTAINER, M3Tokens.BODY_CONTRAST);

        pair(c, mode, M3Tokens.ColorRole.ON_SURFACE,
                M3Tokens.ColorRole.SURFACE, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_SURFACE,
                M3Tokens.ColorRole.SURFACE_CONTAINER, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_SURFACE,
                M3Tokens.ColorRole.SURFACE_CONTAINER_HIGHEST, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_SURFACE_VARIANT,
                M3Tokens.ColorRole.SURFACE, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_SURFACE_VARIANT,
                M3Tokens.ColorRole.SURFACE_CONTAINER, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_BACKGROUND,
                M3Tokens.ColorRole.BACKGROUND, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ON_SURFACE_INVERSE,
                M3Tokens.ColorRole.SURFACE_INVERSE, M3Tokens.BODY_CONTRAST);

        // The selected tab label and the top app bar's action text.
        pair(c, mode, M3Tokens.ColorRole.PRIMARY,
                M3Tokens.ColorRole.SURFACE, M3Tokens.BODY_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.PRIMARY_INVERSE,
                M3Tokens.ColorRole.SURFACE_INVERSE, M3Tokens.BODY_CONTRAST);
        // Composite boundaries are not text; M3 asks for 3:1.
        pair(c, mode, M3Tokens.ColorRole.OUTLINE,
                M3Tokens.ColorRole.SURFACE, M3Tokens.LARGE_CONTRAST);
        pair(c, mode, M3Tokens.ColorRole.ERROR,
                M3Tokens.ColorRole.SURFACE, M3Tokens.LARGE_CONTRAST);
    }

    private static void pair(Map<M3Tokens.ColorRole, Integer> c, String mode,
            M3Tokens.ColorRole fg, M3Tokens.ColorRole bg, double minimum) {
        final double ratio = Palette.contrastRatio(c.get(fg), c.get(bg));
        require(mode + " " + fg + " on " + bg + " is " + format(ratio)
                        + ":1, needs " + format(minimum) + ":1",
                ratio >= minimum);
    }

    private static void distinctModes(Map<M3Tokens.ColorRole, Integer> light,
            Map<M3Tokens.ColorRole, Integer> dark) {
        require("light and dark surfaces differ",
                !light.get(M3Tokens.ColorRole.SURFACE)
                        .equals(dark.get(M3Tokens.ColorRole.SURFACE)));
        require("light and dark primaries differ",
                !light.get(M3Tokens.ColorRole.PRIMARY)
                        .equals(dark.get(M3Tokens.ColorRole.PRIMARY)));
        require("light and dark on-surfaces differ",
                !light.get(M3Tokens.ColorRole.ON_SURFACE)
                        .equals(dark.get(M3Tokens.ColorRole.ON_SURFACE)));
        require("light and dark backgrounds differ",
                !light.get(M3Tokens.ColorRole.BACKGROUND)
                        .equals(dark.get(M3Tokens.ColorRole.BACKGROUND)));
    }

    /**
     * The photograph and the spectrum are measured values, not scheme slots.
     * Their exact values are pinned and no M3 role may resolve to one of them.
     */
    private static void fixedStageAndSpectrum(Map<M3Tokens.ColorRole, Integer> light,
            Map<M3Tokens.ColorRole, Integer> dark) {
        final Palette lightPalette = Palette.light();
        final Palette darkPalette = Palette.dark();
        require("light stage is the neutral surround",
                lightPalette.color(Palette.Role.STAGE) == 0xFF121614);
        require("dark stage is the neutral surround",
                darkPalette.color(Palette.Role.STAGE) == 0xFF070A08);
        require("light spectral red is measured", lightPalette.color(
                Palette.Role.SPECTRUM_RED) == 0xFFA93226);
        require("light spectral green is measured", lightPalette.color(
                Palette.Role.SPECTRUM_GREEN) == 0xFF2E6B3A);
        require("light spectral blue is measured", lightPalette.color(
                Palette.Role.SPECTRUM_BLUE) == 0xFF24479A);
        require("dark spectral red is measured", darkPalette.color(
                Palette.Role.SPECTRUM_RED) == 0xFFE08A7A);
        require("dark spectral green is measured", darkPalette.color(
                Palette.Role.SPECTRUM_GREEN) == 0xFF9BD3A4);
        require("dark spectral blue is measured", darkPalette.color(
                Palette.Role.SPECTRUM_BLUE) == 0xFF9BB8EE);

        final int[] fixed = {
                lightPalette.color(Palette.Role.STAGE),
                darkPalette.color(Palette.Role.STAGE),
                lightPalette.color(Palette.Role.SPECTRUM_RED),
                lightPalette.color(Palette.Role.SPECTRUM_GREEN),
                lightPalette.color(Palette.Role.SPECTRUM_BLUE),
                darkPalette.color(Palette.Role.SPECTRUM_RED),
                darkPalette.color(Palette.Role.SPECTRUM_GREEN),
                darkPalette.color(Palette.Role.SPECTRUM_BLUE),
        };
        for (M3Tokens.ColorRole role : M3Tokens.ColorRole.values()) {
            require("the M3 role " + role + " is not a stage value",
                    light.get(role) != lightPalette.color(Palette.Role.STAGE)
                            && dark.get(role) != darkPalette.color(Palette.Role.STAGE));
            for (int value : fixed) {
                require("the M3 role " + role + " does not reuse " + hex(value),
                        light.get(role) != value && dark.get(role) != value);
            }
        }
    }

    private static void typeScale() {
        require("the type scale has 15 roles", M3Tokens.TypeRole.values().length == 15);
        for (M3Tokens.TypeRole role : M3Tokens.TypeRole.values()) {
            final float[] expected = expectedType(role);
            require(role + " size is " + expected[0] + "sp",
                    close(role.sizeSp, expected[0]));
            require(role + " line height is " + expected[1] + "sp",
                    close(role.lineHeightSp, expected[1]));
            require(role + " tracking is " + expected[2] + "em",
                    close(role.letterSpacingEm, expected[2]));
            require(role + " weight is " + (int) expected[3],
                    role.weight == (int) expected[3]);
            require(role + " line height clears its size",
                    role.lineHeightSp >= role.sizeSp);
            require(role + " tracking stays inside the M3 range",
                    role.letterSpacingEm >= -0.3f && role.letterSpacingEm <= 0.6f);
        }
        rises(M3Tokens.TypeRole.DISPLAY_LARGE, M3Tokens.TypeRole.DISPLAY_MEDIUM,
                M3Tokens.TypeRole.DISPLAY_SMALL);
        rises(M3Tokens.TypeRole.HEADLINE_LARGE, M3Tokens.TypeRole.HEADLINE_MEDIUM,
                M3Tokens.TypeRole.HEADLINE_SMALL);
        rises(M3Tokens.TypeRole.TITLE_LARGE, M3Tokens.TypeRole.TITLE_MEDIUM,
                M3Tokens.TypeRole.TITLE_SMALL);
        rises(M3Tokens.TypeRole.BODY_LARGE, M3Tokens.TypeRole.BODY_MEDIUM,
                M3Tokens.TypeRole.BODY_SMALL);
        rises(M3Tokens.TypeRole.LABEL_LARGE, M3Tokens.TypeRole.LABEL_MEDIUM,
                M3Tokens.TypeRole.LABEL_SMALL);
    }

    private static void rises(M3Tokens.TypeRole large, M3Tokens.TypeRole medium,
            M3Tokens.TypeRole small) {
        require(large + " is larger than " + medium, large.sizeSp > medium.sizeSp);
        require(medium + " is larger than " + small, medium.sizeSp > small.sizeSp);
    }

    private static float[] expectedType(M3Tokens.TypeRole role) {
        switch (role) {
            case DISPLAY_LARGE: return new float[]{57f, 64f, -0.25f, 400};
            case DISPLAY_MEDIUM: return new float[]{45f, 52f, 0f, 400};
            case DISPLAY_SMALL: return new float[]{36f, 44f, 0f, 400};
            case HEADLINE_LARGE: return new float[]{32f, 40f, 0f, 400};
            case HEADLINE_MEDIUM: return new float[]{28f, 36f, 0f, 400};
            case HEADLINE_SMALL: return new float[]{24f, 32f, 0f, 400};
            case TITLE_LARGE: return new float[]{22f, 28f, 0f, 400};
            case TITLE_MEDIUM: return new float[]{16f, 24f, 0.15f, 500};
            case TITLE_SMALL: return new float[]{14f, 20f, 0.1f, 500};
            case BODY_LARGE: return new float[]{16f, 24f, 0.5f, 400};
            case BODY_MEDIUM: return new float[]{14f, 20f, 0.25f, 400};
            case BODY_SMALL: return new float[]{12f, 16f, 0.4f, 400};
            case LABEL_LARGE: return new float[]{14f, 20f, 0.1f, 500};
            case LABEL_MEDIUM: return new float[]{12f, 16f, 0.5f, 500};
            case LABEL_SMALL: return new float[]{11f, 16f, 0.5f, 500};
            default: throw new AssertionError("unhandled type role " + role);
        }
    }

    private static void shapes() {
        require("extra-small shape is 4dp",
                M3Tokens.ShapeRole.EXTRA_SMALL.radiusDp == 4);
        require("small shape is 8dp", M3Tokens.ShapeRole.SMALL.radiusDp == 8);
        require("medium shape is 12dp", M3Tokens.ShapeRole.MEDIUM.radiusDp == 12);
        require("large shape is 16dp", M3Tokens.ShapeRole.LARGE.radiusDp == 16);
        require("extra-large shape is 28dp",
                M3Tokens.ShapeRole.EXTRA_LARGE.radiusDp == 28);
        require("full shape is a pill", M3Tokens.ShapeRole.FULL.radiusDp >= 100);
        int previous = -1;
        for (M3Tokens.ShapeRole shape : M3Tokens.ShapeRole.values()) {
            require(shape + " is ordered", shape.radiusDp > previous);
            previous = shape.radiusDp;
        }
    }

    private static void stateLayers() {
        require("hover is 8%", close(M3Tokens.StateLayer.HOVER.opacity, 0.08f));
        require("focus is 10%", close(M3Tokens.StateLayer.FOCUS.opacity, 0.10f));
        require("pressed is 10%", close(M3Tokens.StateLayer.PRESSED.opacity, 0.10f));
        require("dragged is 16%", close(M3Tokens.StateLayer.DRAGGED.opacity, 0.16f));
        require("a state layer never hides the role beneath it",
                M3Tokens.StateLayer.DRAGGED.opacity < 0.5f);
    }

    private static Map<String, Integer> parseColors(File file) throws Exception {
        final Document doc = parse(file);
        final Map<String, Integer> colors = new LinkedHashMap<>();
        final NodeList nodes = doc.getElementsByTagName("color");
        for (int i = 0; i < nodes.getLength(); ++i) {
            final Element color = (Element) nodes.item(i);
            final String name = color.getAttribute("name");
            final String value = color.getTextContent().trim();
            if (name.isEmpty() || value.isEmpty()) continue;
            colors.put(name, parseHex(value));
        }
        require("colors.xml defines the brand fallback", colors.size() >= 74);
        return colors;
    }

    private static Map<String, Map<String, String>> parseStyles(File file)
            throws Exception {
        final Document doc = parse(file);
        final Map<String, Map<String, String>> styles = new LinkedHashMap<>();
        final NodeList nodes = doc.getElementsByTagName("style");
        for (int i = 0; i < nodes.getLength(); ++i) {
            final Element style = (Element) nodes.item(i);
            final String name = style.getAttribute("name");
            if (name.isEmpty()) continue;
            final Map<String, String> items = new LinkedHashMap<>();
            final NodeList children = style.getChildNodes();
            for (int j = 0; j < children.getLength(); ++j) {
                if (!(children.item(j) instanceof Element)) continue;
                final Element item = (Element) children.item(j);
                if (!"item".equals(item.getTagName())) continue;
                final String attr = item.getAttribute("name");
                final String value = item.getTextContent().trim();
                if (!attr.isEmpty() && !value.isEmpty()) items.put(attr, value);
            }
            styles.put(name, items);
        }
        return styles;
    }

    private static Document parse(File file) throws Exception {
        require(file.getName() + " exists at " + file, file.isFile());
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
            // The resource files are local and trusted; the feature is a guard.
        }
        return factory.newDocumentBuilder().parse(file);
    }

    private static int resolve(String label, String value, Map<String, Integer> colors) {
        if (value.startsWith("#")) return parseHex(value);
        if (value.startsWith("@color/")) {
            final String name = value.substring("@color/".length());
            final Integer color = colors.get(name);
            require(label + " references a defined color (" + name + ")", color != null);
            return color;
        }
        throw new AssertionError(label + " has an unresolved value: " + value);
    }

    private static int parseHex(String value) {
        final String hex = value.substring(1);
        if (hex.length() == 3) {
            final int r = Integer.parseInt(hex.substring(0, 1), 16) * 17;
            final int g = Integer.parseInt(hex.substring(1, 2), 16) * 17;
            final int b = Integer.parseInt(hex.substring(2, 3), 16) * 17;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        if (hex.length() == 6) return 0xFF000000 | Integer.parseInt(hex, 16);
        if (hex.length() == 8) return (int) Long.parseLong(hex, 16);
        throw new AssertionError("unparseable colour " + value);
    }

    private static String hex(int value) {
        return String.format(java.util.Locale.US, "#%08X", value);
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) < 0.0001f;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static void require(String message, boolean ok) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}

package app.spektralab;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * The launcher icon is a public artifact: a uniform bitmap renders as a black
 * tile on every launcher, regardless of the rest of the app. Guard the resource
 * itself, not the code that draws the UI.
 */
public final class IconCheck {
    private static final String[] DENSITIES = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
    private static int checks;

    public static void main(String[] args) throws Exception {
        final File res = new File("app/src/main/res");
        for (String density : DENSITIES) {
            final File legacy = new File(res, "mipmap-" + density + "/ic_launcher.png");
            final File foreground = new File(res, "mipmap-" + density + "/ic_launcher_foreground.png");
            ok("missing " + legacy, legacy.isFile());
            ok("missing " + foreground, foreground.isFile());
            rich("legacy icon " + density, legacy, 100);
            rich("foreground " + density, foreground, 100);
        }

        final File adaptive = new File(res, "mipmap-anydpi-v26/ic_launcher.xml");
        ok("missing adaptive icon", adaptive.isFile());
        final String xml = new String(Files.readAllBytes(adaptive.toPath()), StandardCharsets.UTF_8);
        ok("adaptive icon root", xml.contains("<adaptive-icon"));
        ok("adaptive background", xml.contains("@color/ic_launcher_background"));
        ok("adaptive foreground", xml.contains("@mipmap/ic_launcher_foreground"));

        System.out.println("IconCheck: " + checks + " checks passed");
    }

    private static void rich(String what, File file, int minimumColors) throws Exception {
        final BufferedImage image = ImageIO.read(file);
        ok("unreadable " + what, image != null);
        ok(what + " is square", image.getWidth() == image.getHeight());
        ok(what + " is at least 48 px", image.getWidth() >= 48);
        final Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                colors.add(image.getRGB(x, y));
            }
        }
        ok(what + " is not a flat tile (" + colors.size() + " colors)",
                colors.size() >= minimumColors);
    }

    private static void ok(String what, boolean value) {
        ++checks;
        if (!value) throw new AssertionError(what);
    }
}

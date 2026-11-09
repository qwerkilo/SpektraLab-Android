package app.spektralab.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** The film-profile hue correction and the stocks whose table is not trusted. */
public final class HueCorrection {

    private static final Set<String> UNSUPPORTED = new HashSet<>(Arrays.asList(
            "kodak_vision3_200t", "kodak_vision3_250d", "kodak_vision3_500t",
            "kodak_vision3_50d", "cinestill_800t", "kodak_verita_200d",
            "fujifilm_pro_400h"));

    private HueCorrection() {
    }

    /** True when the stock's profile fixes the correction, so the user cannot change it. */
    public static boolean fixed(String stock) {
        return stock != null && UNSUPPORTED.contains(stock);
    }

    /** What reaches the engine; an unsupported stock is pinned to 0. */
    public static float wire(String stock, float preference) {
        if (fixed(stock)) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, preference));
    }
}

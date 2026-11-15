package app.spektralab.core;

import java.util.Locale;

/**
 * The engine JSON a RAW session opens with. Kept out of the Activity so the
 * film/paper/colour-space contract can be checked without a device.
 */
public final class RawImportParams {

    private RawImportParams() {
    }

    /**
     * Build the open JSON.
     *
     * @param filmStock null when the frame has no film yet
     */
    public static String build(String filmStock, String printStock, double exposure) {
        final String filmField = filmStock == null ? ""
                : String.format(Locale.US, "\"film_stock\":\"%s\",", filmStock);
        return String.format(Locale.US,
                "{%s\"print_stock\":\"%s\",\"input_color_space\":\"ProPhoto RGB\","
                        + "\"input_cctf_decoding\":false,\"output_color_space\":\"sRGB\","
                        + "\"print_exposure\":%.2f,\"preview_long_edge\":2048}",
                filmField, printStock, exposure);
    }
}

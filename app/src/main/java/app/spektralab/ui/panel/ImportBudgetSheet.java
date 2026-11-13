package app.spektralab.ui.panel;

import android.net.Uri;

import java.util.Locale;

import app.spektralab.core.ImportBudget;

/**
 * The import-budget sheet: what the image is, what this device renders in one
 * go, and the two sizes the user can choose.
 *
 * <p>The import used to scale silently, which reads as "the app ate my
 * resolution" the next time the user exports. Asking is the whole point, so the
 * numbers and the row order live here rather than inline in the Activity.
 */
public final class ImportBudgetSheet {

    /** Everything the sheet needs to describe one oversized import. */
    public static final class Request {
        public final Uri uri;
        public final int width;
        public final int height;
        public final int capPixels;
        public final int longEdge;
        public final int memoryCapPixels;
        public final boolean overscan;

        public Request(Uri uri, int width, int height, int capPixels,
                       int longEdge, int memoryCapPixels, boolean overscan) {
            this.uri = uri;
            this.width = width;
            this.height = height;
            this.capPixels = capPixels;
            this.longEdge = longEdge;
            this.memoryCapPixels = memoryCapPixels;
            this.overscan = overscan;
        }
    }

    /** What the Activity does after a row is picked. */
    public interface Host {
        /** Show the sheet with these rows; the callback gets the row index. */
        void showSheet(String title, String body, String[] rows,
                       java.util.function.IntConsumer onPick);

        /** Import with the advisory budget accepted (the sheet's size). */
        void importFit(Uri uri);

        /** Import at the file's native size, accepting a possible E-1001. */
        void importOriginal(Uri uri);
    }

    private final Host host;
    private Request request;

    public ImportBudgetSheet(Host host) {
        this.host = host;
    }

    /** Fit width and height that the accepted row promises. */
    public static int[] fitSize(Request request) {
        final double scale = ImportBudget.scale(request.width, request.height,
                request.capPixels, request.longEdge, request.memoryCapPixels);
        return new int[]{
                Math.max(1, (int) Math.round(request.width * scale)),
                Math.max(1, (int) Math.round(request.height * scale))
        };
    }

    /** The sheet body, including the current overscan reservation. */
    public static String body(Request request) {
        final int[] fit = fitSize(request);
        return "原图 " + request.width + "×" + request.height + "（"
                + megapixels(request.width, request.height) + " MP），这台机器一次能渲染约 "
                + String.format(Locale.US, "%.1f", request.capPixels / 1e6) + " MP"
                + (request.overscan ? "（已按片边预留画布，片边会把画布再撑大约 1.5 倍）" : "")
                + "。\n\n按 " + fit[0] + "×" + fit[1]
                + " 导入一定出得来；按原尺寸可能在本机渲染失败（E-1001）。";
    }

    /** Row labels in pick order. */
    public static String[] rows(Request request) {
        final int[] fit = fitSize(request);
        return new String[]{"按 " + fit[0] + "×" + fit[1] + " 导入", "按原尺寸", "取消"};
    }

    /** Show the sheet and route the pick. */
    public void show(Request request) {
        this.request = request;
        host.showSheet("这张图超过本机预算", body(request), rows(request), this::pick);
    }

    /** Route one row; zero fits, one goes native, anything else cancels. */
    public void pick(int which) {
        if (request == null) {
            return;
        }
        if (which == 0) {
            host.importFit(request.uri);
        } else if (which == 1) {
            host.importOriginal(request.uri);
        }
    }

    static String megapixels(int width, int height) {
        return String.format(Locale.US, "%.1f", (double) width * (double) height / 1e6);
    }
}

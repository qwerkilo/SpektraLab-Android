package app.spektralab.export;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import app.spektralab.DebugSink;
import app.spektralab.engine.SpektraLabEngine;
import app.spektralab.SpkError;
import app.spektralab.core.ExportAttempts;
import app.spektralab.core.ExportDigest;
import app.spektralab.core.ExportMatch;
import app.spektralab.core.GalleryExport;

/**
 * One export: render to a temp file, verify it, write one gallery row and
 * report. The Activity keeps the Look/Frame state and the UI thread; this
 * module owns the worker, the temp file, the NDK calls and the MediaStore row.
 */
public final class ExportRunner {

    /** Immutable request built on the UI thread from a {@code RenderSnapshot}. */
    public static final class Job {
        public final String format;
        public final String tier;
        public final boolean tiff;
        public final boolean dng;
        public final String ext;
        public final String mime;
        public final String stock;
        public final String params;
        public final double[] viewAdjust;
        public final String rawPath;
        public final java.util.function.IntConsumer onDone;

        public Job(String format, String tier, boolean tiff, boolean dng, String ext,
                   String mime, String stock, String params, double[] viewAdjust,
                   String rawPath, java.util.function.IntConsumer onDone) {
            this.format = format;
            this.tier = tier;
            this.tiff = tiff;
            this.dng = dng;
            this.ext = ext;
            this.mime = mime;
            this.stock = stock;
            this.params = params;
            this.viewAdjust = viewAdjust;
            this.rawPath = rawPath;
            this.onDone = onDone;
        }
    }

    /** Everything the worker needs from the Activity. */
    public interface Host {
        File cacheDir();

        Object renderMutex();

        SpektraLabEngine.Session previewSession();

        /** Open the isolated RAW export session with the import caps this run used. */
        SpektraLabEngine.Session openRawSession(String path, String params);

        /** The bitmap currently on the canvas, or null; used by the self-check. */
        Bitmap canvasBitmap();

        ContentResolver resolver();

        void uiProgress(String text);

        void uiWriting(int width, int height);

        void uiResult(Uri uri, String mime, String canvas, int width, int height,
                      long renderMs, long secs, String sizeLabel, boolean tiff, boolean dng);

        void uiSuspect();

        void uiFailure(String message, int code);

        /** E-1001/E-1009: offer a smaller re-import. */
        void offerSmallerImport(int code);
    }

    private ExportRunner() {
    }

    public static void start(Job job, Host host) {
        new Thread(() -> run(job, host), "export").start();
    }

    private static void run(Job job, Host host) {
        final File tmp = new File(host.cacheDir(),
                "export_" + System.currentTimeMillis() + job.ext);
        try {
            host.uiProgress("导出中：准备全尺寸渲染（分条）...");
            try {
                DebugSink.text("export-params", "film=" + job.stock + " raw="
                        + (job.rawPath != null) + " " + job.params);
            } catch (Throwable ignored) {
            }

            final long t0 = System.currentTimeMillis();
            final java.util.concurrent.atomic.AtomicBoolean spinning =
                    new java.util.concurrent.atomic.AtomicBoolean(true);
            final android.os.Handler ticker = new android.os.Handler(
                    android.os.Looper.getMainLooper());
            final Runnable tick = new Runnable() {
                @Override public void run() {
                    if (!spinning.get()) {
                        return;
                    }
                    final long elapsed = (System.currentTimeMillis() - t0) / 1000;
                    host.uiProgress("导出中（" + job.tier + "）：渲染 " + elapsed + " 秒...");
                    ticker.postDelayed(this, 1000);
                }
            };
            ticker.post(tick);

            final Attempts attempts;
            try {
                attempts = renderAttempts(job, host, tmp);
            } finally {
                spinning.set(false);
            }
            finish(job, host, tmp, attempts, t0);
        } catch (Throwable e) {
            final String message = describe(e);
            try {
                DebugSink.text("export-error", "tier=" + job.tier + " " + message);
            } catch (Throwable ignored) {
            }
            uploadProgress(tmp);
            uploadVkLog(host.cacheDir());
            final int code = SpkError.codeOf(message);
            host.uiFailure(message, code);
            if (job.onDone != null) {
                job.onDone.accept(0);
            }
            if (code == 1001 || code == 1009) {
                host.offerSmallerImport(code);
            }
        } finally {
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    private static final class Attempts {
        final int[] info;
        final String canvas;
        final boolean suspect;

        Attempts(ExportAttempts.Outcome outcome) {
            this.info = outcome.rendered.info;
            this.canvas = outcome.rendered.canvas;
            this.suspect = outcome.suspect;
        }
    }

    private static Attempts renderAttempts(Job job, Host host, File tmp) {
        final ExportAttempts.Outcome outcome = ExportAttempts.run(job.tiff, job.dng,
                new ExportAttempts.Render() {
                    @Override public ExportAttempts.Rendered render() {
                        final Rendered rendered = ExportRunner.render(tmp, job, host);
                        return new ExportAttempts.Rendered(rendered.info, rendered.canvas);
                    }

                    @Override public void discard() {
                        tmp.delete();
                    }
                },
                () -> matchesCanvas(tmp, host.canvasBitmap()),
                new ExportAttempts.Observer() {
                    @Override public void accepted(double ncc) {
                        logNcc(ncc);
                    }

                    @Override public void suspect(double ncc, int attempt,
                                                  ExportAttempts.Rendered rendered) {
                        logCorrupt(ncc, rendered.info, job.tier, attempt);
                    }
                });
        return new Attempts(outcome);
    }

    private static final class Rendered {
        final int[] info;
        final String canvas;

        Rendered(int[] info, String canvas) {
            this.info = info;
            this.canvas = canvas;
        }
    }

    /** Re-state the export Look and render the full-size file. */
    private static Rendered render(File tmp, Job job, Host host) {
        final int[] info;
        final String canvas;
        synchronized (host.renderMutex()) {
            SpektraLabEngine.Session target = host.previewSession();
            SpektraLabEngine.Session rawExport = null;
            try {
                if (job.rawPath != null) {
                    if (job.rawPath.isEmpty()) {
                        throw new RuntimeException("RAW 文件路径不可用，无法导出");
                    }
                    rawExport = host.openRawSession(job.rawPath, job.params);
                    target = rawExport;
                }
                SpektraLabEngine.setViewAdjust(job.viewAdjust);
                target.setParams(job.params);
                try {
                    DebugSink.text("overscan-geom", "tier=" + job.tier + " before "
                            + target.overscanGeometry());
                    DebugSink.text("export-params", "tier=" + job.tier
                            + " raw=" + (rawExport != null) + " " + target.getParams());
                } catch (Throwable ignored) {
                }
                info = job.dng
                        ? target.exportDng(tmp.getAbsolutePath(), job.tier)
                        : job.tiff
                        ? target.exportTiff(tmp.getAbsolutePath(), job.tier)
                        : target.exportJpeg(tmp.getAbsolutePath(), 95, 512, job.tier);
                canvas = overscanCanvasLabel(target);
                try {
                    DebugSink.text("overscan-geom", "tier=" + job.tier + " " + info[0]
                            + "x" + info[1] + " " + target.overscanGeometry());
                } catch (Throwable ignored) {
                }
            } finally {
                if (rawExport != null) {
                    rawExport.close();
                }
            }
        }
        return new Rendered(info, canvas);
    }

    /** The rebate's canvas as `WxH`, or "" when this frame has no rebate. */
    private static String overscanCanvasLabel(SpektraLabEngine.Session source) {
        try {
            final String g = source == null ? null : source.overscanGeometry();
            if (g == null) {
                return "";
            }
            final int wAt = g.indexOf("\"canvas_w\":");
            final int hAt = g.indexOf("\"canvas_h\":");
            if (wAt < 0 || hAt < 0) {
                return "";
            }
            final int w0 = wAt + 11, w1 = g.indexOf(',', w0);
            final int h0 = hAt + 11, h1 = g.indexOf(',', h0);
            if (w1 < 0 || h1 < 0) {
                return "";
            }
            return g.substring(w0, w1) + "x" + g.substring(h0, h1);
        } catch (Throwable ignored) {
            return "";
        }
    }

    /** 48-column grayscale NCC between the file and the canvas. */
    private static double matchesCanvas(File file, Bitmap canvas) {
        if (file == null || canvas == null || canvas.getWidth() <= 0) {
            return -1;
        }
        try {
            final android.graphics.BitmapFactory.Options bounds =
                    new android.graphics.BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return -1;
            }
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 200) {
                sample *= 2;
            }
            final android.graphics.BitmapFactory.Options opts =
                    new android.graphics.BitmapFactory.Options();
            opts.inSampleSize = sample;
            final Bitmap decoded = android.graphics.BitmapFactory.decodeFile(
                    file.getAbsolutePath(), opts);
            if (decoded == null) {
                return -1;
            }
            final int gw = 48;
            final int gh = Math.max(8, Math.round(gw * (float) decoded.getHeight()
                    / decoded.getWidth()));
            final Bitmap a = Bitmap.createScaledBitmap(decoded, gw, gh, true);
            final Bitmap b = Bitmap.createScaledBitmap(canvas, gw, gh, true);
            final int[] pa = new int[gw * gh];
            final int[] pb = new int[gw * gh];
            a.getPixels(pa, 0, gw, 0, 0, gw, gh);
            b.getPixels(pb, 0, gw, 0, 0, gw, gh);
            if (a != decoded) {
                a.recycle();
            }
            if (b != canvas) {
                b.recycle();
            }
            decoded.recycle();
            return ExportMatch.ncc(pa, pb);
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Write the rendered file to the gallery and report on the UI thread. */
    private static void finish(Job job, Host host, File tmp, Attempts attempts, long t0)
            throws Exception {
        uploadProgress(tmp);
        uploadVkLog(host.cacheDir());
        final int w = attempts.info[0];
        final int h = attempts.info[1];
        final long renderMs = attempts.info[2];
        if (w <= 0 || h <= 0 || !tmp.exists() || tmp.length() == 0) {
            throw new RuntimeException("引擎没有输出图像");
        }

        host.uiWriting(w, h);
        final Uri outUri = writeToGallery(job, host, tmp);

        try {
            final long len = tmp.length();
            DebugSink.text("export", "format=" + job.format + " tier=" + job.tier
                    + " w=" + w + " h=" + h + " renderMs=" + renderMs + " bytes=" + len);
            if (len > 0 && len < 30L * 1024 * 1024) {
                final byte[] buf = new byte[(int) len];
                try (InputStream in = new FileInputStream(tmp)) {
                    int off = 0, n;
                    while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) {
                        off += n;
                    }
                }
                DebugSink.bytes("export", "out-" + job.tier + job.ext, buf, job.mime);
            }
        } catch (Throwable ignored) {
        }
        final long secs = Math.max(1, (System.currentTimeMillis() - t0) / 1000);
        final long bytes = tmp.exists() ? tmp.length() : 0;
        final String sizeLabel = bytes > 0
                ? String.format(Locale.getDefault(), "%.1f MB", bytes / 1048576.0) : "?";
        if (attempts.suspect) {
            host.uiSuspect();
        } else {
            host.uiResult(outUri, job.mime, attempts.canvas, w, h, renderMs, secs,
                    sizeLabel, job.tiff, job.dng);
        }
        if (job.onDone != null) {
            job.onDone.accept(attempts.suspect ? 0 : 1);
        }
    }

    private static Uri writeToGallery(Job job, Host host, File tmp) throws Exception {
        final String fileName = "SpektraLab_" + System.currentTimeMillis() + job.ext;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, job.mime);
        values.put(MediaStore.Images.Media.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_PICTURES + "/SpektraLab");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        final ContentResolver resolver = host.resolver();
        final Uri outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (outUri == null) {
            throw new RuntimeException("无法创建媒体文件");
        }
        final ExportDigest.Result copied = GalleryExport.write(tmp, new GalleryExport.Sink() {
            @Override public OutputStream openOutput() throws IOException {
                return resolver.openOutputStream(outUri);
            }

            @Override public InputStream openInput() throws IOException {
                return resolver.openInputStream(outUri);
            }

            @Override public void publish() throws IOException {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                if (resolver.update(outUri, values, null, null) <= 0) {
                    throw new IOException("导出发布失败：媒体行未更新");
                }
            }

            @Override public void discard() {
                try {
                    resolver.delete(outUri, null, null);
                } catch (Throwable ignored) {
                }
            }
        });
        try {
            DebugSink.text("export-verify", "bytes=" + copied.length
                    + " sha256=" + ExportDigest.hex(copied.sha256));
        } catch (Throwable ignored) {
        }
        return outUri;
    }

    private static void uploadProgress(File out) {
        try {
            final File prog = new File(out.getAbsolutePath() + ".prog");
            if (!prog.exists()) {
                return;
            }
            final byte[] buf = new byte[(int) prog.length()];
            try (InputStream in = new FileInputStream(prog)) {
                int off = 0, n;
                while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) {
                    off += n;
                }
            }
            DebugSink.text("export-progress", new String(buf, StandardCharsets.UTF_8));
            prog.delete();
        } catch (Throwable ignored) {
        }
    }

    private static void uploadVkLog(File cacheDir) {
        try {
            final File f = new File(cacheDir, "vk.log");
            if (!f.exists() || f.length() == 0) {
                DebugSink.text("vk-log", "(empty) exists=" + f.exists());
                return;
            }
            final int cap = 220 * 1024;
            final long len = f.length();
            final long start = Math.max(0, len - cap);
            final byte[] buf = new byte[(int) (len - start)];
            try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                raf.seek(start);
                raf.readFully(buf);
            }
            DebugSink.text("vk-log", "bytes=" + len + " from=" + start + "\n"
                    + new String(buf, StandardCharsets.UTF_8));
        } catch (Throwable t) {
            try {
                DebugSink.text("vk-log", "read failed: " + t);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void logNcc(double ncc) {
        if (ncc >= 0) {
            final String line = String.format(Locale.US, "canvas NCC %.3f", ncc);
            android.util.Log.i("SpektraLabExport", line);
            try {
                DebugSink.text("export-selfcheck", line);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void logCorrupt(double ncc, int[] info, String tier, int attempt) {
        final String line = String.format(Locale.US,
                "canvas NCC %.3f at %dx%d tier=%s attempt=%d",
                ncc, info[0], info[1], tier, attempt);
        android.util.Log.w("SpektraLabExport", line);
        try {
            DebugSink.textNow("export-corrupt", line);
        } catch (Throwable ignored) {
        }
    }

    private static String describe(Throwable t) {
        final StringBuilder b = new StringBuilder();
        for (Throwable c = t; c != null && b.length() < 600; c = c.getCause()) {
            if (b.length() > 0) {
                b.append(" <- ");
            }
            b.append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
            if (c.getCause() == c) {
                break;
            }
        }
        return b.toString();
    }
}

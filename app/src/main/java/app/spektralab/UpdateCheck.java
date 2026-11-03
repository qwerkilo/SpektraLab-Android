package app.spektralab;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Looks for a newer build and, if there is one, says so.
 *
 * The announcement lives in two places on purpose. Cloudflare answers fastest
 * where it is reachable at all — workers.dev is refused outright on mainland
 * China networks (TCP to 443 never opens), so the same shape of JSON is also
 * kept in the repository, where jsDelivr and raw.githubusercontent.com serve
 * it. The first endpoint that answers wins; if none does, the check is silent
 * and the app carries on. Nothing about the device is sent with the request:
 * it is a GET of a public file, and the reply is a version number.
 */
final class UpdateCheck {

    private static final String PREFS = "update";
    private static final String KEY_SEEN_CODE = "seen_code";
    private static final String KEY_CHECKED_AT = "checked_at";
    // 30 minutes, not 6 hours: an announcement published while someone has the
    // app open should land on their next launch, not tomorrow.
    private static final long MIN_INTERVAL_MS = 30L * 60 * 1000;

    private UpdateCheck() {}

    /** Silent unless there is something to say, or `manual` was asked for. */
    static void check(final Activity activity, final boolean manual) {
        final SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!manual) {
            final long last = prefs.getLong(KEY_CHECKED_AT, 0);
            if (System.currentTimeMillis() - last < MIN_INTERVAL_MS) return;
        }
        // This build's own channel: a `-m3` pre-release opts into preview.
        final String clientName = versionName(activity);
        new Thread(() -> {
            String body = null;
            String lastError = null;
            for (String endpoint : UpdateGate.endpoints(clientName)) {
                // Provenance is part of the offer: an announcement may only be
                // read from one of the fork's own URLs, never the upstream's.
                if (!UpdateGate.isForkEndpoint(endpoint)) {
                    lastError = endpoint + ": not the fork's update endpoint";
                    continue;
                }
                try {
                    body = fetch(endpoint);
                    if (body != null && !body.isEmpty()) break;
                } catch (Throwable t) {
                    lastError = endpoint + ": " + t;
                }
            }
            prefs.edit().putLong(KEY_CHECKED_AT, System.currentTimeMillis()).apply();

            if (body == null) {
                if (manual) {
                    final String detail = lastError == null ? "所有更新地址都不可用" : lastError;
                    post(activity, () -> toast(activity,
                    "[E-2001] 检查更新失败（网络不可用或公告地址被拦截）\n" + detail));
                }
                return;
            }

            final Announcement a;
            try {
                a = Announcement.parse(body);
            } catch (Throwable t) {
                if (manual) post(activity, () -> toast(activity,
                        "[E-2002] 更新公告格式错误：" + t.getMessage()));
                return;
            }
            if (a == null) return;

            final int current = versionCode(activity);
            if (!UpdateGate.eligible(a.versionCode, a.channel, current, clientName)) {
                if (manual) {
                    if (a.versionCode <= current) {
                        post(activity, () -> toast(activity,
                                "已是最新版本 " + versionName(activity) + "（" + current + "）"));
                    } else {
                        // Newer code, wrong channel: a stable install must not
                        // be pushed onto a preview build by a version compare.
                        post(activity, () -> toast(activity,
                                "发现 " + a.versionName + "，但它在「预览」通道；当前稳定版不接收预览更新"));
                    }
                }
                return;
            }
            if (!manual && prefs.getInt(KEY_SEEN_CODE, 0) == a.versionCode) return;

            post(activity, () -> show(activity, a, prefs));
        }, "update-check").start();
    }

    /** The app owns its messages: never show the Android system Toast. */
    private static void toast(Activity activity, String message) {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).showSnackbar(message);
        }
    }

    private static void show(Activity activity, Announcement a, SharedPreferences prefs) {
        StringBuilder message = new StringBuilder();
        for (String note : a.notes) message.append("· ").append(note).append('\n');
        if (message.length() > 0) message.append('\n');
        message.append("点「去更新」打开下载页（开源免费）");

        if (!(activity instanceof MainActivity)) return;
        final String[] actions = {"去更新", "复制链接", "稍后"};
        final String title = UpdateGate.PREVIEW.equals(a.channel)
                ? "发现预览版 " + a.versionName : "发现新版本 " + a.versionName;
        ((MainActivity) activity).showActionSheet(title,
                message.toString().trim(), actions, which -> {
                    if (which == 0) {
                        prefs.edit().putInt(KEY_SEEN_CODE, a.versionCode).apply();
                        try {
                            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(a.url)));
                        } catch (ActivityNotFoundException e) {
                            toast(activity, "没有可用的浏览器：" + a.url);
                        }
                    } else if (which == 1) {
                        android.content.ClipboardManager cm =
                                (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("SpektraLab", a.url));
                        toast(activity, "链接已复制");
                        prefs.edit().putInt(KEY_SEEN_CODE, a.versionCode).apply();
                    } else {
                        prefs.edit().putInt(KEY_SEEN_CODE, a.versionCode).apply();
                    }
                });
    }

    private static String fetch(String endpoint) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        // 3 s to connect: the fallbacks are a CDN and a raw file, and a
        // blocked host should not hold the check up for longer than that.
        c.setConnectTimeout(3000);
        c.setReadTimeout(5000);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "SpektraLab-Android");
        try {
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream();
                 BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } finally {
            c.disconnect();
        }
    }

    private static void post(Activity activity, Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    static int versionCode(Context c) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return android.os.Build.VERSION.SDK_INT >= 28 ? (int) p.getLongVersionCode() : p.versionCode;
        } catch (Throwable t) {
            return 0;
        }
    }

    static String versionName(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "?";
        }
    }

    /** One announcement: what it is, which channel it is on, where to get it, what changed. */
    static final class Announcement {
        final int versionCode;
        final String versionName;
        final String url;
        final String channel;
        final String[] notes;

        private Announcement(int code, String name, String url, String channel, String[] notes) {
            this.versionCode = code;
            this.versionName = name;
            this.url = url;
            this.channel = channel;
            this.notes = notes;
        }

        static Announcement parse(String json) throws Exception {
            JSONObject o = new JSONObject(json);
            int code = o.optInt("latest_version_code", 0);
            if (code <= 0) return null;
            String name = o.optString("latest_version_name", String.valueOf(code));
            String url = ReleaseUrl.sanitize(o.optString("url", ReleaseUrl.DEFAULT));
            // An announcement that names no channel is stable.
            String channel = UpdateGate.channelOf(o.optString("channel", ""));
            JSONArray arr = o.optJSONArray("notes");
            String[] notes = new String[arr == null ? 0 : arr.length()];
            for (int i = 0; i < notes.length; ++i) notes[i] = arr.optString(i, "");
            return new Announcement(code, name, url, channel, notes);
        }
    }
}

package app.spektralab.core;

/**
 * The engine's metering choices, their labels and their user-facing notes.
 *
 * <p>The same table feeds the picker, the Look JSON and the engine delta, so a
 * value the user can pick is a value the engine can receive.
 */
public final class Metering {

    public static final String[] IDS = {
            "", "balanced", "protect_highlights", "protect_shadows",
            "center", "center_weighted", "average", "median"};
    public static final String[] NAMES = {
            "关闭", "平衡", "保护高光", "保护阴影",
            "中心", "中心加权", "平均", "中位数"};
    public static final String[] NOTES = {
            "不测光，由胶片自己的曝光决定",
            "整体拉到中灰，明亮场景不会被压成一片",
            "平均值压住高光，路灯或窗边的画面不会白成一片",
            "平均值抬起暗部，逆光人像不会黑成剪影",
            "只测画面中心，边缘不参与",
            "按中心高斯加权取平均，接近相机默认",
            "全画面等权平均",
            "取中间亮度，最不容易被极端点带偏"};

    private Metering() {
    }

    /** Empty is a valid "off"; anything unknown becomes balanced. */
    public static String sanitize(String method) {
        if (method == null || method.isEmpty()) {
            return "";
        }
        for (String id : IDS) {
            if (id.equals(method)) {
                return method;
            }
        }
        return "balanced";
    }

    public static String name(String method) {
        final String safe = sanitize(method);
        for (int i = 0; i < IDS.length; ++i) {
            if (IDS[i].equals(safe)) {
                return NAMES[i];
            }
        }
        return safe;
    }

    public static int indexOf(String method) {
        final String safe = sanitize(method);
        for (int i = 0; i < IDS.length; ++i) {
            if (IDS[i].equals(safe)) {
                return i;
            }
        }
        return 0;
    }
}

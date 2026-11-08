package app.spektralab.core;

/**
 * User-facing text for the stable error codes.
 *
 * <p>The engine and the app both report a code; this table is the only place
 * that turns one into a sentence. Keeping it free of Android and engine types
 * lets the wording be checked on a plain JVM, and keeps {@code SpkError} to
 * classification and formatting.
 */
public final class ErrorText {

    /** The picker's grant, gone. Not an engine failure. */
    public static final String IMPORT_PERMISSION =
            "照片的读取权限已失效（系统收回了文件选择器给的临时授权）。请点「导入」重新选择这张照片";

    /** The render succeeded; the gallery write did not. */
    public static final String GALLERY_WRITE =
            "导出已经渲染完成，但写入相册被系统拒绝。请检查相册/存储权限和剩余空间，"
            + "或换一个相册目录后再导出";

    /** The most common E-9000: the engine's own library did not load. */
    public static final String NATIVE_LOAD =
            "引擎库没有加载成功（libspektrafilm_engine.so）。这是安装包里的原生库没有就位，"
            + "不是你操作的问题——重装没用；请点「复制」把下面这段完整发给我，我按名字就能定位";

    /** E-9000 with no Vulkan driver at all. */
    public static final String NO_VULKAN =
            "这台手机没有可用的 Vulkan 驱动，SpektraLab 的渲染全在 GPU 上，没有它跑不起来；"
            + "如果你的机型确实支持 Vulkan，请把这条截图发给我";

    private ErrorText() {
    }

    /** Full explanation for a code, or the unknown-code sentence. */
    public static String explain(int code) {
        String text = gpu(code);
        if (text == null) {
            text = file(code);
        }
        if (text == null) {
            text = render(code);
        }
        if (text == null) {
            text = request(code);
        }
        if (text == null) {
            text = update(code);
        }
        return text != null ? text : "未归类的错误，请把这条截图发给开发者";
    }

    /** The compact status-line form, or null when the code has no short form. */
    public static String shortForm(int code) {
        switch (code) {
            case 1304: return "[E-1304] 照片权限已失效，请重新导入";
            case 1305: return "[E-1305] 片门与照片比例不符";
            case 1306: return "[E-1306] 写入相册失败，请检查存储权限";
            case 9000: return "[E-9000] 引擎库没有加载成功";
            case 1010: return "[E-1010] 画布超过单帧上限（约 42 MP）";
            case 1108: return "[E-1108] 解码未授权：请把相机改成「无损压缩」";
            case 1109: return "[E-1109] 缺少解码器：请先转成普通 DNG";
            case 1110: return "[E-1110] 机型未收录：请把原始文件发我";
            default: return null;
        }
    }

    /** 1xxx — the GPU, or the frame it was asked to take. */
    private static String gpu(int code) {
        switch (code) {
            case 1001: return "这张图超过了本机 GPU 的单缓冲上限，请用较小的尺寸导入或导出";
            case 1002: return "渲染调度超出本机上限（图像太大）";
            case 1003: return "显存不足";
            case 1004: return "GPU 驱动被重置（过热或单次运算过久），请等机器凉下来再试";
            case 1005: return "本机没有可用的 Vulkan 驱动，或驱动版本低于引擎要求"
                             + "（需要 Vulkan 1.1 以上的计算管线）。"
                             + "换个系统版本更新的机器试试；同一台机器升级系统也可能解决";
            case 1006: return "本机驱动能力低于本程序的最低要求";
            case 1007: return "该效果在当前图形后端上尚未实现";
            case 1008: return "图形资源创建失败";
            // 1009 is what the engine reports when a Vulkan call itself failed
            // and the message did not name a limit. On a big frame that is
            // nearly always the device refusing to allocate the planes, so the
            // advice is the same as 1001's rather than "internal error", which
            // tells the user nothing and the developer less.
            case 1009: return "图形后端调用失败（大图多为显存不足）：可关掉片边、或按较小尺寸重新导入后重试";
            // The rebate's canvas, which is a frame of its own: a source that
            // fits can still lay out a canvas that cannot be rendered.
            case 1010: return "开了片边之后，画布超过了安卓端冲洗引擎的单帧上限（约 42 MP）："
                             + "请关掉片边，或按更小的尺寸重新导入";
            case 1011: return "渲染引擎没能在这台设备上启动：Vulkan 设备建立失败，"
                             + "多半是驱动不完整或显存被别的应用占着。"
                             + "重启手机再试一次；仍然不行就是这台机器的驱动跑不了这个引擎";
            default: return null;
        }
    }

    /** 11xx — the file. */
    private static String file(int code) {
        switch (code) {
            case 1101: return "RAW 打开失败：格式或压缩方式不受支持";
            case 1102: return "RAW 解包失败：文件确实损坏或没有下载完整（换一份重传试试）";
            case 1103: return "RAW 显影失败";
            case 1104: return "RAW 输出异常";
            case 1105: return "RAW 高光扩展失败";
            case 1106: return "RAW 解码失败";
            // Not damage, and not something a smaller frame or a re-download
            // fixes: Nikon's high-efficiency NEF is JPEG-XS, and the decoder
            // for it is Nikon's to license. The cure is one camera setting, so
            // the sentence is the setting.
            case 1108: return "解码未授权：这是尼康高效率压缩（HE/HE★）RAW，"
                             + "解 JPEG-XS 的授权不在本程序里。请改相机设置——"
                             + "照片拍摄菜单 → RAW 记录 → 无损压缩；"
                             + "已经拍好的这张，请用 NX Studio 或 Adobe DNG Converter 转成 DNG 再导入";
            // No decoder in this build, and no way to add one without the
            // codec's own library. A different converter for each, so two
            // sentences; one code, because the cure is always "convert".
            case 1109: return "这个文件用的是本机没有解码器的压缩（DNG 的 JPEG XL，"
                             + "或 GoPro GPR 的 VC-5）。请先用 Adobe DNG Converter 转成普通 DNG"
                             + "（选无损压缩），GoPro 的文件请先用 GoPro Player 导出 DNG 或 TIFF";
            // The decoder has tables for specific bodies. A new one is not a
            // broken file, and saying "corrupt" would send the user to their
            // card reader for nothing.
            case 1110: return "这台相机还没有收录进解码器的机型表，这一档压缩暂时解不了。"
                             + "请把这张原始文件（不要压缩、不要截图）发我，我加上机型支持";
            default: return null;
        }
    }

    /** 12xx — the render graph. */
    private static String render(int code) {
        switch (code) {
            case 1201: return "该效果在当前图形后端上尚未实现";
            case 1202: return "渲染没有输出像素";
            default: return null;
        }
    }

    /** 13xx — the request. */
    private static String request(int code) {
        switch (code) {
            case 1301: return "胶片或相纸名称无效";
            case 1302: return "参数超出允许范围";
            case 1303: return "会话状态异常，请重新导入照片";
            case 1304: return IMPORT_PERMISSION;
            // The gate and the picture disagree about their shape. A crop is
            // the cure, and there are two ways to make one, so name both.
            case 1305: return "片门与照片比例不符：到「边框」页换一个同比例的片门，"
                             + "或到「构图」页裁成片门的比例";
            // App-level: the export rendered, only the gallery write failed.
            // Kept distinct from the engine's 1305 gate mismatch.
            case 1306: return GALLERY_WRITE;
            default: return null;
        }
    }

    /** 2xxx — the update channel. */
    private static String update(int code) {
        switch (code) {
            case 2001: return "检查更新失败：网络不可用，或公告地址被当前网络拦截";
            case 2002: return "更新公告格式错误";
            default: return null;
        }
    }
}

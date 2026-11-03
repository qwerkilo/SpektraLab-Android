package app.spektralab;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Everything one Frame's Look owns: film stock, print stock, all 22 optical
 * parameters, the camera filter and protection norm, and the Rebate/Date
 * back settings.
 *
 * <p>The parameter contract itself lives in {@link OpticsDelta.Param}: its
 * wire name is the JSON key, its default is the value an older Look gets when
 * the field is absent, and its range is what the slider clamps to. This class
 * only owns the current values, so the strip, the reset, the JSON, and the
 * export cannot disagree about what a parameter is.
 *
 * <p>Older Looks are read, never reset: a field that is present keeps its
 * value even if the app no longer offers it on a control, and only absent
 * fields take their documented default. Switching Frames therefore never
 * carries one photograph's optics onto another, and upgrading never rewrites
 * a saved Look.
 */
public final class Look {
    /** The paper a Look starts on when it names none. */
    public static final String DEFAULT_PRINT_STOCK = "kodak_portra_endura";
    /** The 135 gate a Look starts on when it names none. */
    public static final String DEFAULT_OVERSCAN_FORMAT = "135";

    String filmStock;
    String printStock = DEFAULT_PRINT_STOCK;

    float printExposure = OpticsDelta.EXPOSURE_DEFAULT;
    float grainAmount = OpticsDelta.GRAIN_DEFAULT;
    float halationAmount = OpticsDelta.HALATION_DEFAULT;
    float glareAmount = OpticsDelta.GLARE_DEFAULT;
    float filmFormatMm = 36f;

    /** RFC-023's highlight room, in stops; 0 leaves the stage off. */
    float highlightRoom;
    /** RFC-023's shadow side: lift the toe instead of the shoulder. */
    float shadowRoom;
    /** RFC-025: stops added to the highlights *before* the halo is drawn. */
    float halationBoost;
    /** RFC-025: the in-emulsion scatter, a mix weight (0..1). */
    float halationScatter = 1f;
    /** Lens blur in micrometres, before the film sees anything. */
    float lensBlur;
    /** Enlarger dichroic filters: -1 is 30 CC of the opposite colour. */
    float yFilter, mFilter;
    /** CC value those two shifts are scaled to. */
    float filterScale = 1f;
    /** Pre-flash, as a fraction of the clear-base exposure (0..1). */
    float preflash;
    /** Rebate: base fog and light leaks. */
    float edgeFog = 1f, edgeLeaks, frameNumber;
    /** Switches: 1 is on. Kept as floats so one knob can drive them. */
    float halationOn = 1f, grainOn = 1f, couplersOn = 1f, antihalationRemoved = 0f;
    float scannerWhite = 0f, scannerBlack = 0f, edrOn = 0f;
    /** RFC-023's remaining curve shape. */
    float shadowRolloff = 2f, maxLift = 4f;
    /** Choices: lens filter, the norm, and the rebate/date layout. */
    String cameraFilter = "";
    String sceneNorm = "power";
    String edgeMode = "strip", edgeGate = "auto", edgeHoles = "white", edgeCarrier = "none";
    String datePlacement = "frame", dateCorner = "br", dateStyle = "lcd";
    /** mm from the gate's edge to the text, frame placement only. */
    float dateInsetX = 3f, dateInsetY = 2.4f;
    float edgeFNumber = 0f, dateSize = 1f, dateEv = 3.5f;
    /** The manufacturer's edge print: its own switch, because it is the
     *  one part of the rebate that has been landing inside the picture. */
    boolean edgeTextOn = true;
    /** Free text: the shooting data, and a pair's second frame's date. */
    String dataText = "", dateTextB = "";
    /**
     * The Date back's editable imprint text. Empty means "use the Frame's own
     * EXIF date"; the resolved default is supplied by the caller, not stored
     * here, so a Frame without a typed date keeps following its file.
     */
    String dateText = "";
    /** The rebate's own draws. */
    int cameraSeed = 1, frameSeed = 1;
    boolean overscanOn, dateOn;
    String overscanFormat = DEFAULT_OVERSCAN_FORMAT;

    /** Current value for one contract parameter. */
    float get(OpticsDelta.Param param) {
        switch (param) {
            case EXPOSURE: return printExposure;
            case HIGHLIGHT_ROOM: return highlightRoom;
            case SHADOW_ROOM: return shadowRoom;
            case GRAIN: return grainAmount;
            case HALATION: return halationAmount;
            case HALATION_BOOST: return halationBoost;
            case HALATION_SCATTER: return halationScatter;
            case GLARE: return glareAmount;
            case LENS_BLUR: return lensBlur;
            case Y_FILTER: return yFilter;
            case M_FILTER: return mFilter;
            case FILTER_SCALE: return filterScale;
            case PREFLASH: return preflash;
            case SHADOW_ROLLOFF: return shadowRolloff;
            case MAX_LIFT: return maxLift;
            case HALATION_ON: return halationOn;
            case ANTIHALATION_REMOVED: return antihalationRemoved;
            case GRAIN_ON: return grainOn;
            case COUPLERS_ON: return couplersOn;
            case SCANNER_WHITE: return scannerWhite;
            case SCANNER_BLACK: return scannerBlack;
            case EDR_ON: return edrOn;
            default: throw new IllegalArgumentException("unknown parameter " + param);
        }
    }

    /** Store one contract parameter, unchanged. The slider clamps to the
     *  contract range before calling; reading an older Look must not rewrite a
     *  value the engine still understands. */
    void set(OpticsDelta.Param param, float value) {
        switch (param) {
            case EXPOSURE: printExposure = value; break;
            case HIGHLIGHT_ROOM: highlightRoom = value; break;
            case SHADOW_ROOM: shadowRoom = value; break;
            case GRAIN: grainAmount = value; break;
            case HALATION: halationAmount = value; break;
            case HALATION_BOOST: halationBoost = value; break;
            case HALATION_SCATTER: halationScatter = value; break;
            case GLARE: glareAmount = value; break;
            case LENS_BLUR: lensBlur = value; break;
            case Y_FILTER: yFilter = value; break;
            case M_FILTER: mFilter = value; break;
            case FILTER_SCALE: filterScale = value; break;
            case PREFLASH: preflash = value; break;
            case SHADOW_ROLLOFF: shadowRolloff = value; break;
            case MAX_LIFT: maxLift = value; break;
            case HALATION_ON: halationOn = value; break;
            case ANTIHALATION_REMOVED: antihalationRemoved = value; break;
            case GRAIN_ON: grainOn = value; break;
            case COUPLERS_ON: couplersOn = value; break;
            case SCANNER_WHITE: scannerWhite = value; break;
            case SCANNER_BLACK: scannerBlack = value; break;
            case EDR_ON: edrOn = value; break;
            default: throw new IllegalArgumentException("unknown parameter " + param);
        }
    }

    /** Return one parameter to its contract default; every other value stays. */
    void reset(OpticsDelta.Param param) {
        set(param, param.defaultValue);
    }

    /** A detached copy of this Look, with no value normalised on the way. */
    Look copy() {
        return fromJson(toJson());
    }

    /** The Look as the keys the app has always saved; every field is present. */
    JSONObject toJson() {
        final JSONObject o = new JSONObject();
        try {
            o.put("film", filmStock == null ? "" : filmStock);
            o.put("print", printStock == null ? DEFAULT_PRINT_STOCK : printStock);
            for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
                o.put(param.wire, (double) get(param));
            }
            o.put("cfilter", cameraFilter == null ? "" : cameraFilter);
            o.put("snorm", sceneNorm == null ? "power" : sceneNorm);
            o.put("emode", edgeMode == null ? "strip" : edgeMode);
            o.put("egate", edgeGate == null ? "auto" : edgeGate);
            o.put("eholes", edgeHoles == null ? "white" : edgeHoles);
            o.put("ecarrier", edgeCarrier == null ? "none" : edgeCarrier);
            o.put("dplace", datePlacement == null ? "frame" : datePlacement);
            o.put("dcorn", dateCorner == null ? "br" : dateCorner);
            o.put("dstyle", dateStyle == null ? "lcd" : dateStyle);
            o.put("dinsx", (double) dateInsetX);
            o.put("dinsy", (double) dateInsetY);
            o.put("etext", edgeTextOn);
            o.put("efnum", (double) edgeFNumber);
            o.put("dsize", (double) dateSize);
            o.put("dev", (double) dateEv);
            o.put("dtext", dataText == null ? "" : dataText);
            o.put("dtextb", dateTextB == null ? "" : dateTextB);
            o.put("dtextm", dateText == null ? "" : dateText);
            o.put("fog", (double) edgeFog);
            o.put("leaks", (double) edgeLeaks);
            o.put("frameno", (double) frameNumber);
            o.put("cseed", cameraSeed);
            o.put("fseed", frameSeed);
            o.put("mm", (double) filmFormatMm);
            o.put("overscan", overscanOn);
            o.put("format", overscanFormat == null ? DEFAULT_OVERSCAN_FORMAT : overscanFormat);
            o.put("date", dateOn);
        } catch (JSONException ignored) { }
        return o;
    }

    /** Read a Look, defaulting only the fields the object does not carry. */
    static Look fromJson(JSONObject o) {
        final Look l = new Look();
        if (o == null) return l;
        final String film = o.optString("film", "");
        l.filmStock = film.isEmpty() ? null : film;
        final String paper = o.optString("print", "");
        l.printStock = paper.isEmpty() ? DEFAULT_PRINT_STOCK : paper;
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            l.set(param, (float) o.optDouble(param.wire, param.defaultValue));
        }
        l.cameraFilter = o.optString("cfilter", "");
        l.sceneNorm = o.optString("snorm", "power");
        l.edgeMode = o.optString("emode", "strip");
        l.edgeGate = o.optString("egate", "auto");
        l.edgeHoles = o.optString("eholes", "white");
        l.edgeCarrier = o.optString("ecarrier", "none");
        l.datePlacement = o.optString("dplace", "frame");
        l.dateCorner = o.optString("dcorn", "br");
        l.dateStyle = o.optString("dstyle", "lcd");
        l.dateInsetX = (float) o.optDouble("dinsx", 3.0);
        l.dateInsetY = (float) o.optDouble("dinsy", 2.4);
        l.edgeTextOn = o.optBoolean("etext", true);
        l.edgeFNumber = (float) o.optDouble("efnum", 0.0);
        l.dateSize = (float) o.optDouble("dsize", 1.0);
        l.dateEv = (float) o.optDouble("dev", 3.5);
        l.dataText = o.optString("dtext", "");
        l.dateTextB = o.optString("dtextb", "");
        l.dateText = o.optString("dtextm", "");
        l.edgeFog = (float) o.optDouble("fog", 1.0);
        l.edgeLeaks = (float) o.optDouble("leaks", 0.0);
        l.frameNumber = (float) o.optDouble("frameno", 0.0);
        l.cameraSeed = o.optInt("cseed", 1);
        l.frameSeed = o.optInt("fseed", 1);
        l.filmFormatMm = (float) o.optDouble("mm", 36.0);
        l.overscanOn = o.optBoolean("overscan", false);
        final String format = o.optString("format", DEFAULT_OVERSCAN_FORMAT);
        l.overscanFormat = format.isEmpty() ? DEFAULT_OVERSCAN_FORMAT : format;
        l.dateOn = o.optBoolean("date", false);
        return l;
    }
}

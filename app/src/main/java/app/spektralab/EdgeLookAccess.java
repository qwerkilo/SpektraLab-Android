package app.spektralab;

/**
 * Typed access to the edge and date fields of one live Look.
 *
 * <p>{@link Look}'s fields stay package-private so no panel can keep its own
 * copy; this borrowed view is created from the Activity's current Look on the
 * call that needs it and must not be stored across a Frame switch.
 */
public final class EdgeLookAccess {
    private final Look look;

    public EdgeLookAccess(Look look) {
        this.look = look;
    }

    public boolean overscanOn() {
        return look.overscanOn;
    }

    public void overscanOn(boolean value) {
        look.overscanOn = value;
    }

    public boolean edgeTextOn() {
        return look.edgeTextOn;
    }

    public void edgeTextOn(boolean value) {
        look.edgeTextOn = value;
    }

    public float edgeFog() {
        return look.edgeFog;
    }

    public void edgeFog(float value) {
        look.edgeFog = value;
    }

    public float edgeLeaks() {
        return look.edgeLeaks;
    }

    public void edgeLeaks(float value) {
        look.edgeLeaks = value;
    }

    public float frameNumber() {
        return look.frameNumber;
    }

    public void frameNumber(float value) {
        look.frameNumber = value;
    }

    public float edgeFNumber() {
        return look.edgeFNumber;
    }

    public void edgeFNumber(float value) {
        look.edgeFNumber = value;
    }

    public String edgeMode() {
        return look.edgeMode;
    }

    public void edgeMode(String value) {
        look.edgeMode = value;
    }

    public String edgeGate() {
        return look.edgeGate;
    }

    public void edgeGate(String value) {
        look.edgeGate = value;
    }

    public String edgeHoles() {
        return look.edgeHoles;
    }

    public void edgeHoles(String value) {
        look.edgeHoles = value;
    }

    public String edgeCarrier() {
        return look.edgeCarrier;
    }

    public void edgeCarrier(String value) {
        look.edgeCarrier = value;
    }

    public int cameraSeed() {
        return look.cameraSeed;
    }

    public void cameraSeed(int value) {
        look.cameraSeed = value;
    }

    public int frameSeed() {
        return look.frameSeed;
    }

    public void frameSeed(int value) {
        look.frameSeed = value;
    }

    public boolean dateOn() {
        return look.dateOn;
    }

    public void dateOn(boolean value) {
        look.dateOn = value;
    }

    public String dateText() {
        return look.dateText;
    }

    public void dateText(String value) {
        look.dateText = value;
    }

    public String dateTextB() {
        return look.dateTextB;
    }

    public void dateTextB(String value) {
        look.dateTextB = value;
    }

    public String dataText() {
        return look.dataText;
    }

    public void dataText(String value) {
        look.dataText = value;
    }

    public float dateInsetX() {
        return look.dateInsetX;
    }

    public void dateInsetX(float value) {
        look.dateInsetX = value;
    }

    public float dateInsetY() {
        return look.dateInsetY;
    }

    public void dateInsetY(float value) {
        look.dateInsetY = value;
    }

    public float dateSize() {
        return look.dateSize;
    }

    public void dateSize(float value) {
        look.dateSize = value;
    }

    public float dateEv() {
        return look.dateEv;
    }

    public void dateEv(float value) {
        look.dateEv = value;
    }

    public String datePlacement() {
        return look.datePlacement;
    }

    public void datePlacement(String value) {
        look.datePlacement = value;
    }

    public String dateCorner() {
        return look.dateCorner;
    }

    public void dateCorner(String value) {
        look.dateCorner = value;
    }

    public String dateStyle() {
        return look.dateStyle;
    }

    public void dateStyle(String value) {
        look.dateStyle = value;
    }
}

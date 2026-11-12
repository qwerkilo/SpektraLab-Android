package app.spektralab.ui;

/**
 * Presentation state that survives a theme rebuild.
 *
 * <p>It holds no Look, Frame or engine value; those are restored from
 * their own sessions.
 */
public final class UiState {
    private int edgePageIndex = -1;
    private int filmScrollX;
    private int stageMode;
    private int activeTab;
    private boolean panelCollapsed;
    private boolean opticsUnlocked;
    private boolean showingOriginal;
    private boolean busy;
    private boolean progress;
    private boolean exportProgress;
    private boolean shareVisible;
    private String fileName;
    private String rawNameText;
    private String status;
    private float scanFraction;
    private boolean scanVisible;

    public int stageMode() {
        return stageMode;
    }

    public void setStageMode(int value) {
        stageMode = value;
    }

    public int activeTab() {
        return activeTab;
    }

    public void setActiveTab(int value) {
        activeTab = value;
    }

    public boolean panelCollapsed() {
        return panelCollapsed;
    }

    public void setPanelCollapsed(boolean value) {
        panelCollapsed = value;
    }

    public boolean opticsUnlocked() {
        return opticsUnlocked;
    }

    public void setOpticsUnlocked(boolean value) {
        opticsUnlocked = value;
    }

    public boolean showingOriginal() {
        return showingOriginal;
    }

    public void setShowingOriginal(boolean value) {
        showingOriginal = value;
    }

    public boolean busy() {
        return busy;
    }

    public void setBusy(boolean value) {
        busy = value;
    }

    public boolean progress() {
        return progress;
    }

    public void setProgress(boolean value) {
        progress = value;
    }

    public boolean exportProgress() {
        return exportProgress;
    }

    public void setExportProgress(boolean value) {
        exportProgress = value;
    }

    public boolean shareVisible() {
        return shareVisible;
    }

    public void setShareVisible(boolean value) {
        shareVisible = value;
    }

    public String fileName() {
        return fileName;
    }

    public void setFileName(String value) {
        fileName = value;
    }

    public String rawNameText() {
        return rawNameText;
    }

    public void setRawNameText(String value) {
        rawNameText = value;
    }

    public String status() {
        return status;
    }

    public void setStatus(String value) {
        status = value;
    }

    public float scanFraction() {
        return scanFraction;
    }

    public void setScanFraction(float value) {
        scanFraction = value;
    }

    public boolean scanVisible() {
        return scanVisible;
    }

    public void setScanVisible(boolean value) {
        scanVisible = value;
    }

    public int edgePageIndex() {
        return edgePageIndex;
    }

    public void setEdgePageIndex(int value) {
        edgePageIndex = value;
    }

    public int filmScrollX() {
        return filmScrollX;
    }

    public void setFilmScrollX(int value) {
        filmScrollX = value;
    }
}

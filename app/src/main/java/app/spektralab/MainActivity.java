package app.spektralab;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import app.spektralab.engine.SpektraLabEngine;
import app.spektralab.core.FilmBrand;
import app.spektralab.core.HueCorrection;
import app.spektralab.core.ImageFormat;
import app.spektralab.core.LivePreviewGate;
import app.spektralab.core.Metering;
import app.spektralab.core.SafeFileName;
import app.spektralab.core.PrintTarget;
import app.spektralab.core.RenderEpoch;
import app.spektralab.core.ReopenDecision;
import app.spektralab.core.ScanProfile;
import app.spektralab.core.ScanSample;
import app.spektralab.core.SessionDocument;
import app.spektralab.core.ExportDigest;
import app.spektralab.core.ExportMatch;
import app.spektralab.core.GalleryExport;
import app.spektralab.core.SpectrumCurve;
import app.spektralab.core.SpectrumSamples;
import app.spektralab.core.StockNames;
import app.spektralab.ui.ConsoleTheme;
import app.spektralab.ui.ConsoleType;
import app.spektralab.ui.panel.FilmPanel;
import app.spektralab.ui.panel.OpticsPanel;
import app.spektralab.ui.panel.EdgeHomePanel;
import app.spektralab.ui.panel.EdgePagesPanel;
import app.spektralab.ui.panel.ExportSheet;
import app.spektralab.ui.panel.ImportBudgetSheet;
import app.spektralab.ui.panel.ScalePanel;
import app.spektralab.ui.panel.StagePanel;
import app.spektralab.ui.M3Chrome;
import app.spektralab.core.ImportBudget;
import app.spektralab.model.FilmStock;
import app.spektralab.ui.GlassStyle;
import app.spektralab.ui.ThemeMode;
import app.spektralab.ui.UiState;
import app.spektralab.model.Frame;
import app.spektralab.ui.widget.ArrowView;
import app.spektralab.ui.widget.CompareView;
import app.spektralab.ui.widget.EdgeBackdrop;
import app.spektralab.ui.widget.GuideView;
import app.spektralab.ui.widget.HistogramView;
import app.spektralab.ui.widget.ScanOverlayView;
import app.spektralab.ui.widget.SnackbarHost;
import app.spektralab.ui.widget.SpectrumCurveView;
import app.spektralab.ui.widget.SpectralRail;
import app.spektralab.ui.widget.ToolGlyph;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Console's composition root.
 *
 * <p>It owns the engine, the opened Frames and the programmatic view tree. A
 * Frame's Look lives in its {@link FrameLookSession}; this class holds only
 * the current session and never a second copy of a Look value. Preview and
 * export both start from a {@link RenderSnapshot}, and all engine renders are
 * serialized by {@code renderMutex}.
 *
 * <p>A theme rebuild replaces the view tree without loading a Frame or starting
 * a render; the restored state comes from {@link app.spektralab.ui.UiState}.
 */
public class MainActivity extends AppCompatActivity {
    private SpektraLabEngine engine;
    /** This GPU's limits, read once per engine. Drives the import cap and the export tiers. */
    private DeviceProbe probe = DeviceProbe.parse("{}");
    /** The canary runs once per process, on its own thread. */
    private boolean canaryStarted = false;
    /** Set by the long-press on 导入: keep the decoder's own size and say it may fail. */
    private boolean importAtNativeSize = false;
    /** A cap the last E-1001 offered, good for exactly one import. */
    private int pendingCapPixels = 0;
    /** The user has already said yes to this frame's downscale. One import. */
    private boolean importBudgetAccepted = false;
    /** Set while a chip is re-opening a frame the user already agreed to
     *  shrink. The decision was made the first time; asking again on every tap
     *  turned the strip into a dialog cannon. */
    private boolean suppressBudgetDialog = false;
    /** A "按原尺寸重来" retry decodes the same file again. It must not add a
     *  second chip for a photograph the strip already carries. */
    private boolean retryingNativeSize = false;
    /** One retry offer per import; a dialog loop would be worse than the error. */
    private boolean e1001Offered = false;
    /** A render is in flight: one native result buffer, one renderer. */
    private volatile boolean rendering = false;
    private volatile SpektraLabEngine.Session session;
    private boolean isRawFile = false;
    private String rawFilePath = null;
    /** The caps the current RAW was opened with: an export re-decodes the
     *  same frame in its own session instead of a differently capped one. */
    private volatile int rawImportMaxPixels = 0;
    private volatile int rawImportLongEdge = 0;
    // The last file that reached the gallery, so it can be handed straight to
    // another app. A picker that filters by MIME will not show a TIFF on a ROM
    // whose picture list does not know the type; an explicit ACTION_SEND does,
    // because the type travels in the intent instead of being guessed.
    private Uri lastExportUri = null;
    private String lastExportMime = null;
    /** Shown only after a TIFF export: the one format the picker hides. */
    private TextView sharePill = null;
    private Bitmap sourceBitmap;
    private Uri sourceUri;
    /** The pending current-frame restore, consumed by the next imported Frame. */
    private SessionDocument pendingSession;
    /** The restored session was showing Original, so its first import must not render. */
    private boolean restoringOriginal;
    private Bitmap lastRenderedBitmap;
    private ByteBuffer sourcePixels;
    private int sourceWidth, sourceHeight;
    private CompareView compare;
    private HistogramView histogram;
    /** What the import put on the stage: the frame before any film. */
    private Bitmap beforeBitmap;
    private TextView statusText;
    private LinearProgressIndicator progressBar;
    private LinearProgressIndicator exportProgressBar;
    private final List<StockCatalogue.Stock> filmCatalogue = new ArrayList<>();
    private final List<StockCatalogue.Stock> printCatalogue = new ArrayList<>();
    private final List<String> filmIds = stockIds(filmCatalogue);
    private final List<String> filmLabels = stockLabels(filmCatalogue);
    private final List<String> filmTypes = stockTypes(filmCatalogue);
    private final List<String> printIds = stockIds(printCatalogue);
    private final List<String> printLabels = stockLabels(printCatalogue);
    /** A read-only id view over the catalogue; the catalogue is the only store. */
    private static List<String> stockIds(final List<StockCatalogue.Stock> source) {
        return new java.util.AbstractList<String>() {
            @Override public String get(int index) { return source.get(index).id; }
            @Override public int size() { return source.size(); }
        };
    }

    /** A read-only label view over the same catalogue. */
    private static List<String> stockLabels(final List<StockCatalogue.Stock> source) {
        return new java.util.AbstractList<String>() {
            @Override public String get(int index) { return source.get(index).label; }
            @Override public int size() { return source.size(); }
        };
    }

    /** A read-only type view over the same catalogue. */
    private static List<String> stockTypes(final List<StockCatalogue.Stock> source) {
        return new java.util.AbstractList<String>() {
            @Override public String get(int index) { return source.get(index).type; }
            @Override public int size() { return source.size(); }
        };
    }

    /** The catalogue's film records, keyed by stock id: the shipped spectral
     *  series, its declared source, and the real scalar `info` fields. An
     *  absent record is the only truthful "unavailable"; it is never filled
     *  with a placeholder or a curve. */
    private final java.util.Map<String, FilmStock> filmStocks = new java.util.HashMap<>();
    /** null until the user picks a film: a fresh launch must not look like
     *  a Portra 400 session, and must not render one behind their back. */
    private boolean uiReady = false;
    private String lastImportError = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Before super.onCreate: the window is built from this style, so a
        // dark session never starts on a light launch surface.
        applyLaunchTheme();
        super.onCreate(savedInstanceState);
        installCrashLogger();
        applyWindowPalette();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            android.util.Log.e("SpektraLab", "Uncaught exception in " + t.getName(), e);
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                DebugSink.text("crash", "thread=" + t.getName() + "\n" + sw);
            } catch (Throwable ignored) {}
        });
        // The native side, before anything reads it: every object the engine
        // is linked against is loaded by path, and a failure is kept (see
        // SpektraLabEngine.ensureLoaded) so that the import reports the real
        // reason instead of a NoClassDefFoundError on the class itself.
        String nativeDir = null;
        try { nativeDir = getApplicationInfo().nativeLibraryDir; } catch (Throwable ignored) {}
        try { SpektraLabEngine.prepare(nativeDir); } catch (Throwable e) { lastImportError = describe(e); }
        try { DebugSink.text("app-start", "buildInfo=" + SpektraLabEngine.buildInfo()); } catch (Throwable ignored) {}

        previewSpace = getSharedPreferences("console", MODE_PRIVATE)
                .getString("preview_space", "sRGB");
        buildUi();
        uiReady = true;
        restoreSessionIfAny();

        // Silent, and late enough not to compete with the first import. It only
        // speaks when there is a newer build than this one.
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> UpdateCheck.check(this, false), 3000);
        // Once per build, and late enough that the canvas has settled first.
        maybeShowFeedbackGroup();
    }

    // ---------------------------------------------------------------------
    // The interface.
    //
    // /sdcard/Download/SpektraLab/index.html is the reference. It is drawn at
    // a 390 px viewport, so its CSS px are this app's dp, and every number
    // below is quoted from it. Three deliberate departures:
    //   * the page's 12 px outer frame is dropped -- a phone screen is the app,
    //     not a sheet of paper floating on a page;
    //   * the preview *fits* the frame into itself (FIT_CENTER) where the
    //     page's last pass says `object-fit: cover`, which would crop away the
    //     edge of the photograph the user is about to render;
    //   * the ranges stay inside the engine's schema: grain stops at 2 (the
    //     page says 4; params.cpp refuses anything over 2) and exposure starts
    //     at 0.25 (below it the print is black and the schema's floor is 0.05).
    // ---------------------------------------------------------------------
    /**
     * Theme mode is read before the view tree is built, so the first frame is
     * already the stored design. It is presentation state only: never a Look,
     * a Frame, or an engine parameter.
     */
    private static final String THEME_PREFS = "theme";
    private ThemeMode themeMode = ThemeMode.LIGHT;
    /** The preview bitmap's declared colour space; export always uses sRGB. */
    private String previewSpace = "sRGB";
    private Palette palette = Palette.light();
    /** Which stage surface is showing, captured so a theme rebuild can restore it. */
    private int stageMode = 0;
    /**
     * Scan inspect is presentation-only: it samples the preview and never
     * touches a Look or the engine. Both values are saved per Frame and
     * captured across a theme rebuild.
     */
    private float scanFraction = ScanSample.DEFAULT_FRACTION;
    private boolean scanVisible = false;
    private ScanOverlayView scanOverlay;
    private Chip scanToggleChip;
    private TextView scanReadoutRgb, scanReadoutLambda;
    private MaterialCardView scanReadout;

    private Typeface tfDisplay;      // Inter 300 — numbers and the wordmark
    private Typeface tfBody;         // Inter 400 — Latin labels
    private Typeface tfHan;          // OPPO Sans — Chinese, and anything mixed
    private ConsoleType type;
    private ConsoleTheme theme;
    private View emptyState, rawState;
    private TextView fileLabel, rawName, busyLabel, originalLabel, paperValue, edgeValue, formatFootValue;
    /**
     * The live Optics values for the current Frame: all 22 contract
     * parameters plus the camera filter and protection norm. The Look owns
     * them; the strip and the one slider only read and write this object, so a
     * capture, a preview, and a restored JSON cannot drift apart.
     */
    private Look optics = new Look();
    private FrameLookSession currentSession;
    private MaterialButton undoButton;
    private boolean restoringUndo;
    private int activeOptics = 0;
    /** True while the slider is being parked on a new parameter; no commit. */
    private boolean syncingOptics = false;
    /** True between a real touch-down and touch-up; mid-drag never commits. */
    private boolean trackingOpticsTouch = false;
    private HorizontalScrollView opticsStripScroll;
    private final List<Chip> opticsChips = new ArrayList<>();
    private Slider opticsSlider;
    private final android.os.Handler viewAdjustHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable viewAdjustCommit = this::commitViewAdjust;
    private TextView opticsName, opticsValue, opticsUnit, opticsDelta, opticsRange;
    private MaterialButton opticsReset, opticsFilterButton, opticsNormButton;
    private LinearLayout opticsPanel;
    private TextView opticsLockNote;
    private boolean opticsUnlocked = false;
    private final Runnable opticsCommitRunnable = this::commitOptics;

    // The Console shell's instrument readouts. The strip under the stage is
    // built from the same Look the panels edit, never from a second copy.
    private TextView stageMeta, stageExposureValue;
    private TextView readoutFilm, readoutPaper, readoutExposure;
    private TextView readoutSpectrumValue, readoutSpectrumUnit, readoutSpectrumDetail;
    private View readoutSpectrumCell;
    private SpectralRail spectralRail;

    // RFC-032/031, the two things the phone build never exposed: the rebate the
    // frame was cut from, and the date a point-and-shoot burned into it.
    /** A typed Date back text; null/empty means "follow the Frame's EXIF date". */
    /** The current file's own imprint date, cached until another image opens. */
    private String defaultDateText = null;
    /** The Rebate page's own controls. Dimming while off is presentation only:
     *  these values stay live so re-enabling or switching Frame finds them. */
    /** Date-back values; the page lands in a later ticket, the Look already
     *  carries them and switching Frame must not drop them. */
    /** -1 = the two Edge entry cards, 0 = Rebate, 1 = Date back. */
    private int edgePageIndex = -1;
    /** Each chosen row repaints its value and description from the live field. */
    private final List<Runnable> rebateRefreshers = new ArrayList<>();
    /** True while a Look restore sets switch state; the listener must not render. */
    private boolean syncingToggles = false;

    /** 片边: the Gates this Frame's shape can actually carry, in catalogue order. */
    private final List<String> edgeLabels = new ArrayList<>(), edgeIds = new ArrayList<>();

    private static String gateLabel(String id) {
        final GateScale.Gate gate = GateScale.byId(id);
        return gate == null ? String.valueOf(id) : gate.label;
    }

    /** "69.5 mm" keeps a measured edge exact; a whole millimetre stays whole. */
    private static String mmText(float mm) {
        return Math.abs(mm - Math.round(mm)) < 0.01f
                ? String.format(java.util.Locale.US, "%.0f", mm)
                : String.format(java.util.Locale.US, "%.1f", mm);
    }

    /**
     * Move the Scale page and the Edge Gate onto one shipped Gate. The two
     * controls are one piece of film geometry: there is no state in which the
     * Scale says 36 mm while the Gate says 6x17.
     */
    private void selectGate(String id) {
        final GateScale.Gate gate = GateScale.byId(id);
        if (gate == null) {
            return;
        }
        live().overscanFormat = gate.id;
        live().filmFormatMm = gate.longEdgeMm;
        updateEdgeValue();
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(gate.longEdgeMm) + " mm");
        }
        rebuildFormatCards();
        if (edgePagesPanel != null) {
            edgePagesPanel.syncRebate();
        }
    }
    /** The shell: `body` stacks or splits into `mainCol` and `controls`. */
    private LinearLayout body, mainCol, controls, panelHolder;
    private ScalePanel scalePanel;
    private EdgeHomePanel edgeHomePanel;
    private EdgePagesPanel edgePagesPanel;
    private View stageView;
    private StagePanel stagePanel;
    private FilmPanel filmPanel;
    private OpticsPanel opticsPanelView;
    /** True while the window is at the two-column Console width. */
    private boolean wideLayout = false;

    /** The Gate the Scale/Edge readout is showing, or the honest empty state. */
    private void updateEdgeValue() {
        if (edgeValue == null) {
            return;
        }
        final int selected = edgeIds.indexOf(live().overscanFormat);
        if (selected >= 0) {
            edgeValue.setText(edgeLabels.get(selected));
        } else {
            edgeValue.setText(edgeIds.isEmpty() ? "无可用片门" : gateLabel(live().overscanFormat));
        }
    }

    /** The Gate a reset lands on: 135 when this Frame fits it, else its closest fit. */
    private String defaultGateId() {
        if (edgeIds.isEmpty() || edgeIds.contains("135")) {
            return "135";
        }
        final double aspect = (sourceWidth > 0 && sourceHeight > 0)
                ? (double) Math.max(sourceWidth, sourceHeight) / Math.min(sourceWidth, sourceHeight)
                : 0.0;
        String bestId = edgeIds.get(0);
        double bestError = Double.MAX_VALUE;
        for (String id : edgeIds) {
            final GateScale.Gate gate = GateScale.byId(id);
            if (gate == null) {
                continue;
            }
            final double error = Math.abs(aspect / gate.aspect - 1.0);
            if (error < bestError) { bestError = error; bestId = id; }
        }
        return bestId;
    }
            private final java.util.List<Frame> frames = new java.util.ArrayList<>();
    private boolean batchImportRunning;
    private boolean batchExportRunning;
    private boolean batchExportAbort;
    private LinearLayout sourceBar, sourceRow, thumbStrip;
    /** The folded strip: a short stack of prints in the canvas's lower-left. */
    private View shelfHandle;
    private ImageView shelfHandleImage;
    private HorizontalScrollView thumbScroll;
    private boolean shelfCollapsed = false;
    /** Set while a chip is re-opening a frame, so the import does not append a
     *  second copy of a photograph the strip already has. */
    private boolean reopeningFrame = false;
    private int currentFrame = -1;
    private LinearLayout originalAction;
    private ToolGlyph originalGlyph;
    private int activeTabIndex = 0;
    private boolean panelCollapsed = false;
    /** The film panel opens on 原图, never on a stock the user did not pick. */
    private boolean showingOriginal = true;
    /** The shelf's brand facet; reset on a new import, not on a theme rebuild. */
    private String brandFilter = FilmBrand.ALL;
    private int formatIndex = 0;
    /** The film panel's own views: brands, chips, curve, measured values, note. They
     *  read the current Look on every refresh; no field here is a second copy
     *  of `live().filmStock`. */
    private ChipGroup brandRow;
    private LinearLayout filmChipRow, filmMetricsRow;
    private HorizontalScrollView filmChipScroll;
    private SpectrumCurveView filmCurve;
    private MaterialCardView filmCurveCard;
    /** The last curve this composition has already revealed; a rebuild must
     *  not replay it. The curve object is stable per stock, so identity is
     *  enough and the check never mistakes a render-state refresh for a new
     *  curve. */
    private SpectrumCurve revealedCurve = SpectrumCurve.unavailable();
    private TextView filmNote;
    /** One render at a time, whole sequence -- the engine's lock only covers a
     *  single call, so a queued render could otherwise swap parameters
     *  between the user's renderBegin and its copyArgbRange. */
    private final Object renderMutex = new Object();
    /** A stock change that arrived while a render held the session. Folded into
     *  one follow-up render when the current one lands. */
    private volatile boolean pendingRender = false;
    /** Bumped whenever the canvas is replaced; stale callbacks cannot paint. */
    private final RenderEpoch renderEpoch = new RenderEpoch();
    private int activeRenderEpoch;
    private int activeRenderFrame = -1;
    private static final int PREVIEW_LONG_EDGE = 2048;
    /** Live frames are small enough to finish inside a drag; full render follows. */
    private static final int LIVE_LONG_EDGE = 800;
    /** One live frame at most this often; requests in between coalesce. */
    private static final long LIVE_MIN_GAP_MS = 90;
    private final LivePreviewGate liveGate = new LivePreviewGate();
    private final Object liveLock = new Object();
    private volatile RenderSnapshot liveSnapshot;
    private volatile boolean liveWanted;
    private volatile boolean liveStopped;
    private Thread liveThread;
    private long liveLastStart;
    private int[] livePixels;
    private static final int TOP_APP_BAR_HEIGHT_DP = 64;
    /** The floating Prism top bar sits 8 dp below the status bar. */
    private static final int TOP_BAR_MARGIN_DP = 8;
    /** Stage readouts start below the floating bar, never under it. */
    private static final int STAGE_TOP_CLEARANCE_DP = 78;
    private static final int READOUT_HEIGHT_DP = 52;
    private static final int SOURCE_HEIGHT_DP = 54;
    private static final int TABS_HEIGHT_DP = 48;
    private static final int SECTION_HEAD_HEIGHT_DP = 48;
    /** 48 dp sliders plus their M3 label/line-height rows need the panel's
     *  height; the section head, paper row, and brand row are all 48 dp targets.
     *  336 leaves 8 dp for the panel's bottom breathing room. */
    private static final int PANEL_HEIGHT_DP = 336;
    private static final int CONTROL_SHELL_PAD_DP = 8;
    private static final int PAPER_ROW_HEIGHT_DP = 48;
    private static final int FILM_METRICS_HEIGHT_DP = 36;
    /** Breathing room above the gesture bar; the root also carries the inset. */
    private static final int PANEL_BOTTOM_DP = 8;
    private static final int CONTROLS_OPEN_DP =
            PANEL_HEIGHT_DP + TABS_HEIGHT_DP + CONTROL_SHELL_PAD_DP;
    private static final int CONTROLS_CLOSED_DP = TABS_HEIGHT_DP + CONTROL_SHELL_PAD_DP;
    /** Android's minimum touch target for a primary control. */
    private static final int MIN_TOUCH_DP = 48;
    private static final int SLIDER_HEIGHT_DP = 48;
    /** The film shelf's M3 card: two M3 label lines fit without clipping. */
    private static final int FILM_CARD_WIDTH_DP = 120;
    private static final int FILM_CARD_HEIGHT_DP = 52;
    private static final int FILM_CARD_GAP_DP = 8;
    /** The curve's M3 surface; 76 dp keeps the panel's fixed 336 dp budget. */
    private static final int FILM_CURVE_HEIGHT_DP = 76;
    /** The plotter reveal for a genuinely new curve, unless motion is reduced. */
    private static final int FILM_CURVE_REVEAL_MS = 520;
    /** Wait this long after a keyboard or TalkBack change before one render. */
    private static final int OPTICS_COMMIT_DELAY_MS = 180;
    /** 反馈群：the one place a user can reach a human. Shown once per build. */
    private static final String QQ_GROUP = "1134320449";
    private static final String PROMO_PREFS = "promo";
    private static final String PROMO_SEEN_CODE = "qq_group_seen_code";
    private static final String PROMO_MUTED = "qq_group_muted";
    private static final String PRESET_PREFS = "look_presets";
    private static final String PRESET_KEY = "list";
    private static final int PRESET_LIMIT = 12;

    private static final String[] TAB_NAMES = {"胶片", "光学", "尺度", "边框"};
    private static final String[] TAB_ENGLISH = {"FILM", "OPTICS", "SCALE", "EDGE"};
    /** The rail menu ids, in the same order as the phone tabs. */
    private static final int[] TAB_ITEM_IDS = {
            R.id.nav_film, R.id.nav_optics, R.id.nav_scale, R.id.nav_edge};
    private TabLayout tabLayout;
    /** The tablet navigation rail; it and the phone tabs share `activeTabIndex`. */
    private NavigationRailView navigationRail;
    /** True while a rebuild or a rail/tab sync selects a workspace; the
     *  listener must not treat that as a user gesture. */
    private boolean suppressWorkspaceSelection = false;
    /** When the last real touch landed on a tab. A layout pass can dispatch a
     *  reselect while the window changes size; that is not a collapse gesture. */
    private long lastTabTouchAt = 0L;
    private android.widget.FrameLayout uiRoot;
    /** The M3 snackbar host. It lives inside the inset-padded root, so feedback
     *  clears the gesture bar instead of sitting under it. */
    private CoordinatorLayout snackbarHost;
    private Snackbar currentSnackbar;
    /** The one open bottom sheet. Opening a second one dismisses this first. */
    private BottomSheetDialog openSheet;
    private final View[] panels = new View[4];

    private void loadUiFonts() {
        try {
            tfDisplay = Typeface.createFromAsset(getAssets(), "fonts/Inter-Light.ttf");
            tfBody = Typeface.createFromAsset(getAssets(), "fonts/Inter-Regular.ttf");
        } catch (Throwable t) {
            DebugSink.text("font", "inter: " + t);
            tfDisplay = Typeface.create("sans-serif-light", Typeface.NORMAL);
            tfBody = Typeface.create("sans-serif", Typeface.NORMAL);
        }
        try {
            tfHan = Typeface.createFromAsset(getAssets(), "fonts/OPPOSans.ttf");
        } catch (Throwable t) {
            DebugSink.text("font", "oppo: " + t);
            tfHan = Typeface.create("sans-serif", Typeface.NORMAL);
        }
        type = new ConsoleType(this, tfBody, tfHan);
    }

    /**
     * Resolve the stored Theme mode before the Activity window is built. The
     * theme is applied here and again on every in-place theme switch; dynamic
     * colour is re-applied after each setTheme so the overlay follows the
     * Console's own light/dark choice, not the system's night mode.
     */
    private void applyLaunchTheme() {
        try {
            themeMode = ThemeMode.fromStored(getSharedPreferences(THEME_PREFS, MODE_PRIVATE)
                    .getString(ThemeMode.PREF_KEY, null));
        } catch (Throwable t) {
            themeMode = ThemeMode.LIGHT;
        }
        palette = themeMode.palette();
        applyTheme(themeMode);
    }

    private void applyTheme(ThemeMode mode) {
        try {
            setTheme(mode == ThemeMode.DARK ? R.style.AppTheme_Dark : R.style.AppTheme);
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                DynamicColors.applyToActivityIfAvailable(this);
            }
        } catch (Throwable t) {
            DebugSink.text("theme", "apply style: " + t);
        }
    }

    /** The window behind the first frame, so the launch never flashes paper in dark. */
    private void applyWindowPalette() {
        try {
            getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    theme.background()));
        } catch (Throwable t) {
            DebugSink.text("theme", "window: " + t);
        }
    }

    /** System bars and their icon polarity follow the active palette. */
    private void applySystemBars() {
        try {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // The M3 top app bar owns the status-bar edge; the navigation bar
            // follows the control surface too.
            getWindow().setStatusBarColor(theme.surface());
            getWindow().setNavigationBarColor(theme.surface());
            final boolean lightBars = !palette.isDark();
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.view.WindowInsetsController c = getWindow().getInsetsController();
                if (c != null) {
                    final int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    c.setSystemBarsAppearance(lightBars ? light : 0, light);
                }
            } else if (android.os.Build.VERSION.SDK_INT >= 26) {
                getWindow().getDecorView().setSystemUiVisibility(lightBars
                        ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        : 0);
            }
        } catch (Throwable t) {
            DebugSink.text("bars", String.valueOf(t));
        }
    }

    /** Write the next Theme mode, then rebuild through the same composition root. */
    private void toggleTheme() {
        final ThemeMode next = themeMode.toggled();
        themeMode = next;
        palette = next.palette();
        try {
            getSharedPreferences(THEME_PREFS, MODE_PRIVATE).edit()
                    .putString(ThemeMode.PREF_KEY, next.storedValue()).apply();
        } catch (Throwable t) {
            DebugSink.text("theme", "save: " + t);
        }
        applyTheme(next);
        rebuildForTheme();
    }

    /**
     * Rebuild the view tree in place. Activity recreation is deliberately
     * avoided: it would drop the native session, the loaded Frame, and its Look.
     * This path must never call a render.
     */
    private void rebuildForTheme() {
        if (isFinishing()) {
            return;
        }
        final UiState state = captureUiState();
        dismissOpenSheet();
        currentSnackbar = null;
        snackbarHost = null;
        buildUi();
        restoreUiState(state);
        if (uiRoot != null) {
            final int duration = motionDuration(180);
            uiRoot.setAlpha(duration > 0 ? 0f : 1f);
            if (duration > 0) {
                uiRoot.animate().alpha(1f).setDuration(duration).start();
            }
        }
    }

    /** Presentation that the rebuild has to put back by hand. */
    private UiState captureUiState() {
        final UiState s = new UiState();
        s.setStageMode(stageMode);
        s.setActiveTab(activeTabIndex);
        s.setPanelCollapsed(panelCollapsed);
        s.setOpticsUnlocked(opticsUnlocked);
        s.setShowingOriginal(showingOriginal);
        s.setBusy(busyLabel != null && busyLabel.getVisibility() == View.VISIBLE);
        s.setProgress(progressBar != null && progressBar.getVisibility() == View.VISIBLE);
        s.setExportProgress(exportProgressBar != null && exportProgressBar.getVisibility() == View.VISIBLE);
        s.setShareVisible(sharePill != null && sharePill.getVisibility() == View.VISIBLE);
        s.setFileName(fileLabel == null ? null : String.valueOf(fileLabel.getText()));
        s.setRawNameText(rawName == null ? null : String.valueOf(rawName.getText()));
        s.setStatus(statusText == null ? null : String.valueOf(statusText.getText()));
        s.setScanFraction(scanFraction);
        s.setScanVisible(scanVisible);
        s.setEdgePageIndex(edgePageIndex);
        s.setFilmScrollX(filmChipScroll == null ? 0 : filmChipScroll.getScrollX());
        return s;
    }

    private void restoreUiState(UiState s) {
        if (s == null) {
            return;
        }
        setStage(s.stageMode(), null);
        restoreCompare(s);
        restoreLabels(s);
        showingOriginal = s.showingOriginal();
        setOriginalVisual(showingOriginal);
        restoreProgressIndicators(s);
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(live().filmFormatMm) + " mm");
        }
        setTab(s.activeTab());
        setPanelCollapsed(s.panelCollapsed());
        setOpticsUnlocked(s.opticsUnlocked());
        edgePageIndex = s.edgePageIndex();
        openEdgePage(edgePageIndex);
        scanFraction = s.scanFraction();
        scanVisible = s.scanVisible();
        refreshFilmPanel();
        if (filmChipScroll != null) {
            filmChipScroll.post(() -> filmChipScroll.scrollTo(s.filmScrollX(), 0));
        }
    }

    private void restoreCompare(UiState s) {
        if (compare != null) {
            compare.setBefore(beforeBitmap);
            compare.setAfter(s.showingOriginal() ? null : lastRenderedBitmap);
        }
    }

    private void restoreLabels(UiState s) {
        if (s.fileName() != null && fileLabel != null) {
            fileLabel.setText(s.fileName());
        }
        if (s.rawNameText() != null && rawName != null) {
            rawName.setText(s.rawNameText());
        }
        if (s.status() != null && statusText != null) {
            statusText.setText(s.status());
        }
    }

    private void restoreProgressIndicators(UiState s) {
        if (busyLabel != null) {
            busyLabel.setVisibility(s.busy() ? View.VISIBLE : View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(s.progress() ? View.VISIBLE : View.GONE);
        }
        if (exportProgressBar != null) {
            exportProgressBar.setVisibility(s.exportProgress() ? View.VISIBLE : View.GONE);
        }
        if (sharePill != null) {
            sharePill.setVisibility(s.shareVisible() ? View.VISIBLE : View.GONE);
        }
    }

    /** A zero system animation scale means non-essential motion is off. */
    private boolean reducedMotion() {
        try {
            return android.provider.Settings.Global.getFloat(getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Zero when the system asks for reduced motion; the final state still
     *  appears at once. Theme rebuilds, tab switches, and panel collapse are
     *  already immediate; the toast is the only transition left to gate. */
    private int motionDuration(int millis) {
        return reducedMotion() ? 0 : millis;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int currentWidthDp() {
        return getResources().getConfiguration().screenWidthDp;
    }

    /**
     * Reflow the existing view tree between the phone column and the
     * two-column Console. The stage, panels, and their state are kept: only
     * the body's orientation and the two columns' size rules change, so a
     * foldable resize or a rotation does not rebuild the Frame or its Look.
     */
    private void applyResponsiveLayout(boolean wide, int widthDp) {
        if (body == null || mainCol == null || controls == null || panelHolder == null
                || navigationRail == null) {
            return;
        }
        wideLayout = wide;
        final boolean twoColumn = ConsoleLayout.isTwoColumn(widthDp);
        body.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        applyRailLayout(wide, widthDp);
        applyColumnWidths(wide, twoColumn, widthDp);
        // On a wide window the control column is a distinct instrument rail:
        // the page colour would otherwise run on below the panel and read as
        // an unfinished half-column. The film curve then takes the rest of the
        // rail on the default tab instead of leaving its lower half empty.
        controls.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        panelHolder.setBackground(theme.glassSurface(GlassStyle.SHEET_RADIUS_DP));
        panelHolder.setClipToOutline(true);
        panelHolder.setElevation(dp(GlassStyle.SHEET_ELEVATION_DP));
        // On a wide window the floating bar spans the page; keep the control
        // column's own heading below it. On the phone the sheet is already
        // below the stage, so it only needs its normal breathing room.
        panelHolder.setPadding(dp(16), dp(twoColumn ? TOP_APP_BAR_HEIGHT_DP + 8 : 8),
                dp(16), dp(PANEL_BOTTOM_DP));
        applyFilmCurveHeight(wide);
    }

    /** The leading navigation rail replaces the phone tabs on a wide window. */
    private void applyRailLayout(boolean wide, int widthDp) {
        LinearLayout.LayoutParams railLp =
                (LinearLayout.LayoutParams) navigationRail.getLayoutParams();
        railLp.width = dp(ConsoleLayout.railWidthDp(widthDp));
        railLp.height = -1;
        railLp.weight = 0f;
        navigationRail.setLayoutParams(railLp);
        navigationRail.setVisibility(wide ? View.VISIBLE : View.GONE);
        if (tabLayout != null) {
            tabLayout.setVisibility(wide ? View.GONE : View.VISIBLE);
        }
    }

    /** The stage column against the control column. */
    private void applyColumnWidths(boolean wide, boolean twoColumn, int widthDp) {
        final int controlHeight = dp(wide && !twoColumn
                ? CONTROLS_OPEN_DP - TABS_HEIGHT_DP
                : (panelCollapsed ? CONTROLS_CLOSED_DP : CONTROLS_OPEN_DP));
        final android.view.ViewGroup controlParent =
                (android.view.ViewGroup) controls.getParent();
        final android.view.ViewGroup target = wide && !twoColumn ? mainCol : body;
        if (controlParent != target) {
            if (controlParent != null) {
                controlParent.removeView(controls);
            }
            target.addView(controls, new LinearLayout.LayoutParams(-1, controlHeight));
        }

        LinearLayout.LayoutParams mainLp = (LinearLayout.LayoutParams) mainCol.getLayoutParams();
        mainLp.width = wide ? 0 : -1;
        mainLp.height = wide ? -1 : 0;
        mainLp.weight = 1f;
        mainCol.setLayoutParams(mainLp);

        LinearLayout.LayoutParams controlLp =
                (LinearLayout.LayoutParams) controls.getLayoutParams();
        controlLp.width = twoColumn ? dp(ConsoleLayout.controlColumnWidthDp(widthDp)) : -1;
        controlLp.height = twoColumn ? -1 : controlHeight;
        controlLp.weight = 0f;
        controls.setLayoutParams(controlLp);

        LinearLayout.LayoutParams panelLp =
                (LinearLayout.LayoutParams) panelHolder.getLayoutParams();
        panelLp.height = twoColumn ? 0 : dp(PANEL_HEIGHT_DP);
        panelLp.weight = twoColumn ? 1f : 0f;
        panelHolder.setLayoutParams(panelLp);
    }

    /** Let the spectral curve's M3 surface own the film panel's spare height. */
    private void applyFilmCurveHeight(boolean wide) {
        final View surface = filmCurveCard != null ? filmCurveCard : filmCurve;
        if (surface == null) {
            return;
        }
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) surface.getLayoutParams();
        if (lp == null) {
            return;
        }
        lp.height = wide ? 0 : dp(FILM_CURVE_HEIGHT_DP);
        lp.weight = wide ? 1f : 0f;
        surface.setLayoutParams(lp);
    }

    /** Keep the status line inside the stage when the stage is not full width. */
        /** Rotations and foldable resizes arrive here because the manifest keeps
     *  the Activity alive; no Frame, Look, or render is touched. */
    /** System Back leaves a secondary page before it leaves the Console. */
    @Override
    public void onBackPressed() {
        if (batchExportRunning) {
            batchExportAbort = true;
            showSnackbar("批量导出会在当前这张结束后停止");
            return;
        }
        if (activeTabIndex == 3 && edgePageIndex >= 0) {
            openEdgePage(-1);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyResponsiveLayout(ConsoleLayout.isWide(newConfig.screenWidthDp),
                newConfig.screenWidthDp);
    }

    /** A flat fill with a quiet press ripple; the shape never shifts bounds. */
    private android.graphics.drawable.Drawable pressableFill(Palette.Role fill) {
        final android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();
        shape.setColor(palette.color(fill));
        shape.setCornerRadius(dp(2));
        final int ripple = (palette.color(Palette.Role.ACCENT) & 0x00FFFFFF) | 0x33000000;
        return new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(ripple), shape, null);
    }

    private View hairline(int color) {
        View v = new View(this);
        v.setBackgroundColor(color);
        return v;
    }

    private void applySystemInsets(final View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            final int left, top, right, bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                                | android.view.WindowInsets.Type.displayCutout());
                left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft(); top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight(); bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top, right, bottom);
            // Android 15 paints edge-to-edge and ignores setStatusBarColor.
            // The inset is part of this view's background, so the page colour
            // has to live there as well.
            v.setBackground(new EdgeBackdrop(theme.background()));
            return insets;
        });
    }

    /** The shelf handle in the stage's bottom-left corner. */
    private void addShelfHandle(android.widget.FrameLayout stage) {
shelfHandle = buildShelfHandle();
android.widget.FrameLayout.LayoutParams handleLp = new android.widget.FrameLayout.LayoutParams(
        dp(62), dp(62), android.view.Gravity.BOTTOM | android.view.Gravity.START);
handleLp.leftMargin = dp(12);
handleLp.bottomMargin = dp(12);
stage.addView(shelfHandle, handleLp);
    }

    /** The preview and export progress bars. */
        /** Keep TalkBack on the scan line after its toggle. */
        /** Build the four-tab control deck that owns the panel. */
    private void buildControlDeck() {
controls = new LinearLayout(this);
controls.setOrientation(LinearLayout.VERTICAL);
controls.setBackgroundColor(android.graphics.Color.TRANSPARENT);
controls.setPadding(dp(8), 0, dp(8), dp(CONTROL_SHELL_PAD_DP));

panelHolder = new LinearLayout(this);
panelHolder.setOrientation(LinearLayout.VERTICAL);
panelHolder.setBackground(theme.glassSurface(GlassStyle.SHEET_RADIUS_DP));
panelHolder.setClipToOutline(true);
panelHolder.setElevation(dp(GlassStyle.SHEET_ELEVATION_DP));
panelHolder.setPadding(dp(16), dp(8), dp(16), dp(PANEL_BOTTOM_DP));
panelHolder.addView(scrollablePanel(buildFilmPanel()),
        new LinearLayout.LayoutParams(-1, -1));
panelHolder.addView(scrollablePanel(buildOpticsPanel()),
        new LinearLayout.LayoutParams(-1, -1));
panelHolder.addView(scrollablePanel(buildFormatPanel()),
        new LinearLayout.LayoutParams(-1, -1));
panelHolder.addView(scrollablePanel(buildEdgePanel()),
        new LinearLayout.LayoutParams(-1, -1));
for (int i = 0; i < 4; ++i) {
    panels[i] = panelHolder.getChildAt(i);
}

// The Prism Table dock sits at the bottom, under the sheet, so the panel
// comes first in the column. setTab() still owns the panel swap.
controls.addView(panelHolder, new LinearLayout.LayoutParams(-1, dp(PANEL_HEIGHT_DP)));
controls.addView(buildTabs(), new LinearLayout.LayoutParams(-1, dp(TABS_HEIGHT_DP)));
body.addView(controls, new LinearLayout.LayoutParams(-1, dp(CONTROLS_OPEN_DP)));
    }

           private void buildUi() {
        if (tfBody == null) {
            loadUiFonts();
        }
        if (theme == null) {
            theme = new ConsoleTheme(this, palette);
        } else {
            theme.setPalette(palette);
        }
        applyWindowPalette();
        applySystemBars();

        final android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(theme.background());
        applySystemInsets(root);
        setContentView(root);
        uiRoot = root;

        LinearLayout app = new LinearLayout(this);
        app.setOrientation(LinearLayout.VERTICAL);
        app.setBackgroundColor(theme.background());
        root.addView(app, new android.widget.FrameLayout.LayoutParams(-1, -1));

        // Feedback is an overlay at the bottom of the inset-padded root. The
        // root's padding owns the gesture bar, so the snackbar cannot land on
        // top of it, and the bottom edge is below the photograph's centre.
        snackbarHost = new SnackbarHost(this);
        android.widget.FrameLayout.LayoutParams snackbarLp =
                new android.widget.FrameLayout.LayoutParams(-1, -2,
                        android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
        snackbarLp.bottomMargin = dp(8);
        root.addView(snackbarHost, snackbarLp);

        // The Console shell: the photograph is the page, with the floating
        // Prism top bar over it. Below the tablet breakpoint the stage and
        // controls stack; at or above it they become two columns.
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(theme.background());
        app.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        navigationRail = buildNavigationRail();
        body.addView(navigationRail, new LinearLayout.LayoutParams(
                dp(ConsoleLayout.RAIL_WIDTH_DP), -1));

        mainCol = new LinearLayout(this);
        mainCol.setOrientation(LinearLayout.VERTICAL);
        mainCol.setBackgroundColor(theme.background());
        body.addView(mainCol, new LinearLayout.LayoutParams(-1, 0, 1f));

        final android.widget.FrameLayout stage = buildStagePanel();
        mainCol.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1f));

        final LinearLayout.LayoutParams readoutLp =
                new LinearLayout.LayoutParams(-1, -2);
        readoutLp.setMargins(dp(10), dp(8), dp(10), 0);
        mainCol.addView(buildReadoutStrip(), readoutLp);

        final LinearLayout.LayoutParams sourceLp =
                new LinearLayout.LayoutParams(-1, dp(SOURCE_HEIGHT_DP));
        sourceLp.setMargins(dp(10), dp(6), dp(10), dp(8));
        mainCol.addView(buildSourceBar(), sourceLp);

        buildControlDeck();
        attachFloatingTopBar(root);

        applyResponsiveLayout(ConsoleLayout.isWide(currentWidthDp()), currentWidthDp());
        finishUiBuild();
    }

    /** Build the stage and re-point the Activity aliases at its children. */
    private android.widget.FrameLayout buildStagePanel() {
        stagePanel = new StagePanel(this, type, theme, palette, tfDisplay, tfBody, tfHan,
                new StagePanel.Host() {
                    @Override public Bitmap displayedBitmap() {
                        return MainActivity.this.displayedBitmap();
                    }

                    @Override public Bitmap scanBitmap() {
                        return MainActivity.this.scanBitmap();
                    }

                    @Override public boolean sourcePresent() {
                        return sourceUri != null;
                    }

                    @Override public void onImportMenu() {
                        showImportMenu();
                    }

                    @Override public void onToggleScan() {
                        toggleScan();
                    }

                    @Override public void onScanSample(ScanSample sample, float fraction) {
                        MainActivity.this.onScanSample(sample, fraction);
                    }
                }, veryLargeText());
        final android.widget.FrameLayout stage = stagePanel.frame();
        stageView = stage;
        compare = stagePanel.compare();
        scanOverlay = stagePanel.scanOverlay();
        histogram = stagePanel.histogram();
        emptyState = stagePanel.emptyState();
        rawState = stagePanel.rawState();
        rawName = stagePanel.rawName();
        busyLabel = (TextView) stagePanel.busyLabel();
        fileLabel = stagePanel.fileLabel();
        stageMeta = stagePanel.stageMeta();
        stageExposureValue = stagePanel.stageExposureValue();
        statusText = stagePanel.statusText();
        spectralRail = stagePanel.spectralRail();
        scanToggleChip = stagePanel.scanToggleChip();
        scanReadout = stagePanel.scanReadout();
        scanReadoutRgb = stagePanel.scanReadoutRgb();
        scanReadoutLambda = stagePanel.scanReadoutLambda();
        stagePanel.wireScanTraversal();
        addShelfHandle(stage);
        stagePanel.addProgress();
        progressBar = stagePanel.progressBar();
        exportProgressBar = stagePanel.exportProgressBar();
        return stage;
    }

    /** Add the Prism top bar above the stage without consuming a page row. */
    private void attachFloatingTopBar(android.widget.FrameLayout root) {
        final View topBar = buildTopAppBar();
        final android.widget.FrameLayout.LayoutParams lp =
                new android.widget.FrameLayout.LayoutParams(
                        -1, dp(TOP_APP_BAR_HEIGHT_DP),
                        android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
        lp.leftMargin = dp(TOP_BAR_MARGIN_DP);
        lp.rightMargin = dp(TOP_BAR_MARGIN_DP);
        lp.topMargin = dp(TOP_BAR_MARGIN_DP);
        root.addView(topBar, lp);
    }

    /** Load or restyle the catalogue and land on the first tab. */
    private void finishUiBuild() {
        // A theme rebuild must not re-read the catalogue while a render may be
        // running. Only the first build loads it; later builds restyle the
        // panel from the records already in memory.
        if (filmIds.isEmpty()) {
            loadStockCatalog();
        } else {
            refreshFilmPanel();
            refreshPaperLabel();
        }
        refreshEdgeFormats();
        setTab(0);
        setPanelCollapsed(false);
        setOpticsUnlocked(false);
        rebuildFrameChips();
        refreshReadoutStrip();
    }

    /** A Java-level crash never reaches `DebugSink`'s queue -- the process is
     *  gone first. This posts synchronously, from the dying thread, before the
     *  default handler takes over and the app disappears. */
    private void installCrashLogger() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                final java.io.StringWriter text = new java.io.StringWriter();
                error.printStackTrace(new java.io.PrintWriter(text));
                DebugSink.textNow("crash", thread.getName() + " | " + text);
            } catch (Throwable ignored) { }
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        });
    }

    /** All in-app messages use a real M3 snackbar, never Android's Toast. */
    void showSnackbar(final CharSequence message) {
        showSnackbar(message, Snackbar.LENGTH_LONG);
    }

    private void showSnackbar(final CharSequence message, final int duration) {
        showSnackbar(message, null, null, duration);
    }

    private void showSnackbar(final CharSequence message, final String action,
                              final View.OnClickListener onAction, final int duration) {
        if (message == null || message.length() == 0) {
            return;
        }
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            runOnUiThread(() -> showSnackbar(message, action, onAction, duration));
            return;
        }
        if (snackbarHost == null) {
            return;
        }
        if (currentSnackbar != null) {
            currentSnackbar.dismiss();
        }
        final Snackbar bar = Snackbar.make(snackbarHost, message, duration);
        final TextView text = bar.getView().findViewById(
                com.google.android.material.R.id.snackbar_text);
        if (text != null) {
            text.setMaxLines(4);
            text.setEllipsize(android.text.TextUtils.TruncateAt.END);
            text.setTypeface(tfHan);
        }
        if (action != null && onAction != null) {
            bar.setAction(action, onAction);
            final TextView actionView = bar.getView().findViewById(
                    com.google.android.material.R.id.snackbar_action);
            if (actionView != null) {
                actionView.setTypeface(tfHan);
            }
        }
        bar.addCallback(new Snackbar.Callback() {
            @Override public void onDismissed(Snackbar dismissed, int event) {
                if (currentSnackbar == dismissed) {
                    currentSnackbar = null;
                }
            }
        });
        bar.show();
        currentSnackbar = bar;
    }

    /**
     * A failure's first line is the snackbar; the full, copyable report stays
     * behind the 详情 action. That keeps feedback over the controls without
     * losing the detail that made E-9000 unfixable for two releases.
     */
    private void showErrorSnackbar(String title, String body) {
        final String shownTitle = (title == null || title.isEmpty()) ? "错误" : title;
        final String shownBody = (body == null || body.isEmpty()) ? "[E-0000] 未知错误" : body;
        // An error with a copyable report must not time out before it can be
        // read; swiping it away is the dismissal.
        final String first = firstLine(shownBody).trim();
        showSnackbar(first.isEmpty() ? shownTitle : shownTitle + " · " + first, "详情",
                v -> showErrorDialog(shownTitle, shownBody), Snackbar.LENGTH_INDEFINITE);
    }

    private static String firstLine(String text) {
        final int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }

    /**
     * The M3 small top app bar: the wordmark, Theme mode, and Export. It owns
     * the control scheme's surface; the photograph and its readouts stay below
     * on the Palette stage.
     */
    private View buildTopAppBar() {
        final boolean dark = themeMode == ThemeMode.DARK;

        final AppBarLayout appBar = new AppBarLayout(this);
        appBar.setElevation(0);
        appBar.setLiftOnScroll(false);
        appBar.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        final MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setBackground(theme.glassSurface(GlassStyle.TOP_BAR_RADIUS_DP));
        toolbar.setElevation(dp(GlassStyle.TOP_BAR_ELEVATION_DP));
        toolbar.setClipToOutline(true);
        toolbar.setTitle(type.typed("SpektraLab", tfDisplay));
        // At very large text the wordmark and the two actions compete for one
        // 48 dp row; the subtitle is the first thing to go so the name and
        // the actions both stay legible. At 1.3× the tagline still fits.
        if (!veryLargeText()) {
            toolbar.setSubtitle(type.mixedText("/ CONSOLE"));
        }
        toolbar.setLongClickable(true);
        toolbar.setOnLongClickListener(v -> { showAbout(); return true; });

        final LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.CENTER_VERTICAL);

        sharePill = actionButton(type.mixedText("分享"), "分享最近一次导出");
        sharePill.setVisibility(View.GONE);
        sharePill.setOnClickListener(v -> shareLastExport());
        actions.addView(sharePill, actionButtonParams());

        undoButton = actionButton(type.mixedText(veryLargeText() ? "撤" : "撤销"),
                "撤销上一次调整");
        undoButton.setVisibility(View.GONE);
        undoButton.setOnClickListener(v -> undoLook());
        actions.addView(undoButton, actionButtonParams());

        final MaterialButton themeButton = actionButton(
                type.mixedText(veryLargeText()
                        ? (dark ? "暗" : "亮")
                        : (dark ? "暗 DARK" : "亮 LIGHT")),
                dark ? "当前深色主题，点击切换到浅色" : "当前浅色主题，点击切换到深色");
        themeButton.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(
                    View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Switch.class.getName());
                info.setCheckable(true);
                info.setChecked(dark);
            }
        });
        themeButton.setOnClickListener(v -> toggleTheme());
        actions.addView(themeButton, actionButtonParams());

        final MaterialButton export = actionButton(type.mixedText("导出"), "导出 EXPORT 照片");
        export.setOnClickListener(v -> showExportDialog());
        actions.addView(export, actionButtonParams());

        toolbar.addView(actions, new Toolbar.LayoutParams(
                -2, dp(48), android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL));
        appBar.addView(toolbar, new AppBarLayout.LayoutParams(-1, -1));
        return appBar;
    }

    /** The M3 undo action is visible only while a previous Look exists. */
    private void updateUndoAction() {
        if (undoButton == null) {
            return;
        }
        final boolean can = currentSession != null && currentSession.canUndo();
        undoButton.setVisibility(can ? View.VISIBLE : View.GONE);
        undoButton.setContentDescription(can
                ? "撤销上一次调整，共 " + (currentSession.size() - 1) + " 步"
                : "没有可撤销的调整");
    }

    /** Restore the previous settled Look for this Frame and repaint it. */
    private void undoLook() {
        if (rendering) {
            showSnackbar("正在渲染，完成后才能撤销…");
            return;
        }
        final Look previous = currentSession == null ? null : currentSession.undo();
        if (previous == null) {
            updateUndoAction();
            return;
        }
        optics = currentSession.look();
        restoringUndo = true;
        try {
            applyLook(previous);
            renderImage();
        } finally {
            restoringUndo = false;
        }
        updateUndoAction();
    }

    /** A top app bar action: the real M3 text-button style, 48 dp tall. */
    private MaterialButton actionButton(CharSequence text, String description) {
        final MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setGravity(android.view.Gravity.CENTER);
        button.setSingleLine(true);
        button.setContentDescription(description);
        return button;
    }

    private LinearLayout.LayoutParams actionButtonParams() {
        return new LinearLayout.LayoutParams(-2, dp(48));
    }

    /**
     * A section action as a real M3 text button: its state layer, ripple, and
     * 48 dp target replace the underlined bare TextView the review found.
     */
    private MaterialButton m3TextButton(CharSequence text, String description,
                                        int color, Runnable action) {
        final MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setGravity(android.view.Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextColor(color);
        button.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        button.setRippleColor(ColorStateList.valueOf(
                M3Chrome.withOpacity(color, M3Tokens.StateLayer.PRESSED.opacity)));
        button.setElevation(0f);
        button.setStateListAnimator(null);
        button.setContentDescription(description);
        type.applyTypeRole(button, M3Tokens.TypeRole.LABEL_LARGE, tfHan);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    /** Text inside a Material component: one M3 type role, brand families. */
    private void applyTypeface(android.view.View root, Typeface face) {
        if (root instanceof TextView) {
            ((TextView) root).setTypeface(face);
            return;
        }
        if (root instanceof android.view.ViewGroup) {
            final android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); ++i) {
                applyTypeface(group.getChildAt(i), face);
            }
        }
    }

    /**
     * The Readout strip: film stock, print stock, exposure, and spectrum. It
     * reads the Look fields directly; it is not a second copy of them.
     */
    private View buildReadoutStrip() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(theme.glassSurface(GlassStyle.READOUT_RADIUS_DP));
        outer.setClipToOutline(true);
        outer.setElevation(dp(GlassStyle.CARD_ELEVATION_DP));
        outer.setPadding(dp(2), dp(2), dp(2), dp(2));
        outer.setMinimumHeight(dp(READOUT_HEIGHT_DP));
        outer.addView(hairline(theme.outlineVariant()),
                new LinearLayout.LayoutParams(-1, dp(1)));

        // At 2.0x the bilingual titles wrap to two lines in every cell and the
        // table eats the stage; the Chinese name alone still identifies each
        // reading, and the values stay bilingual.
        final String[] titles = veryLargeText()
                ? new String[] {"胶片", "相纸", "曝光", "光谱"}
                : new String[] {"FILM / 胶片", "PAPER / 相纸",
                        "EXPOSURE / 曝光", "SPECTRUM / 光谱"};
        // Two columns at every width. Four columns at 400 dp clip the section
        // titles and the longer stock names even at the default font scale;
        // the readout is an instrument strip, not a race to fit four cells.
        final int columns = 2;
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int rowIndex = 0; rowIndex * columns < titles.length; ++rowIndex) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setMinimumHeight(dp(READOUT_HEIGHT_DP));
            for (int column = 0; column < columns; ++column) {
                final int i = rowIndex * columns + column;
                if (i >= titles.length) {
                    break;
                }
                if (column > 0) {
                    row.addView(hairline(theme.outlineVariant()),
                            new LinearLayout.LayoutParams(dp(1), -1));
                }
                row.addView(readoutCell(i, titles[i]),
                        new LinearLayout.LayoutParams(0, -2, 1f));
            }
            grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            if ((rowIndex + 1) * columns < titles.length) {
                grid.addView(hairline(theme.outlineVariant()),
                        new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        outer.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        return outer;
    }

    /** A sheet body scrolls when large text makes its rows taller than the window. */
    private NestedScrollView scrollableSheet(View content) {
        final NestedScrollView scroll = new NestedScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(content, new android.widget.FrameLayout.LayoutParams(-1, -2));
        return scroll;
    }

    /** A panel's content scrolls inside the fixed panel height when text grows. */
    private View scrollablePanel(View content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.addView(content, new android.widget.FrameLayout.LayoutParams(-1, -2));
        return scroll;
    }

    private View readoutCell(int i, String titleText) {
        final LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(dp(9), dp(3), dp(6), dp(3));

        TextView title = type.m3Label(titleText, M3Tokens.TypeRole.LABEL_SMALL,
                theme.onSurfaceVariant(), tfHan);
        // At 2.0× the bilingual title is wider than half the phone; let it
        // wrap instead of ending in an ellipsis that hides the parameter name.
        if (veryLargeText()) {
            title.setSingleLine(false);
            title.setMaxLines(2);
        } else {
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }
        cell.addView(title, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(android.view.Gravity.BOTTOM);
        TextView value = type.m3Label("—", M3Tokens.TypeRole.LABEL_MEDIUM,
                theme.onSurface(), i == 2 ? tfDisplay : tfHan);
        value.setSingleLine(true);
        value.setEllipsize(android.text.TextUtils.TruncateAt.END);
        valueRow.addView(value, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView unit = type.m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                theme.onSurfaceVariant(), tfBody);
        LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(-2, -2);
        unitLp.leftMargin = dp(2);
        unitLp.bottomMargin = dp(1);
        valueRow.addView(unit, unitLp);
        LinearLayout.LayoutParams valueRowLp = new LinearLayout.LayoutParams(-1, -2);
        valueRowLp.topMargin = dp(1);
        cell.addView(valueRow, valueRowLp);

        TextView detail = type.m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                theme.onSurfaceVariant(), tfBody);
        detail.setSingleLine(false);
        detail.setMaxLines(2);
        detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.addView(detail, new LinearLayout.LayoutParams(-1, -2));

        if (i == 0) {
            readoutFilm = value;
        }
        if (i == 1) {
            readoutPaper = value;
        }
        if (i == 2) {
            readoutExposure = value;
        }
        if (i == 3) {
            readoutSpectrumValue = value;
            readoutSpectrumUnit = unit;
            readoutSpectrumDetail = detail;
            readoutSpectrumCell = cell;
            cell.setClickable(true);
            cell.setFocusable(true);
            cell.setContentDescription("光谱读数，点按显示或隐藏直方图");
            cell.setOnClickListener(v -> toggleHistogram());
            syncHistogramAccessibility();
        }
        return cell;
    }

    /** True when the system font scale needs the wider film cards and stacked heads. */
    private boolean largeText() {
        return getResources().getConfiguration().fontScale >= 1.15f;
    }

    /** True when the 48 dp top app bar must drop its tagline and English theme word. */
    private boolean veryLargeText() {
        return getResources().getConfiguration().fontScale >= 1.6f;
    }

    /**
     * Keep the strip and the stage's exposure readout on the current Look.
     * Film, print, and exposure are the three values the ticket names; the
     * spectrum cell only reports samples the catalogue actually carries.
     */
    /** Declare the preview's colour space; sRGB is what an untagged bitmap is. */
    private void tagPreviewSpace(Bitmap bitmap) {
        if (bitmap == null || android.os.Build.VERSION.SDK_INT < 29
                || !"Display P3".equals(previewSpace)) {
            return;
        }
        try {
            bitmap.setColorSpace(android.graphics.ColorSpace.get(
                    android.graphics.ColorSpace.Named.DISPLAY_P3));
        } catch (Throwable ignored) { }
    }

    /** Keep the optional histogram tied to what the canvas is showing. */
    private void updateHistogram(Bitmap bitmap) {
        if (stagePanel != null) {
            stagePanel.updateHistogram(bitmap);
        }
    }

    /** Keep the spectrum cell's accessible label in sync with the overlay state. */
    private void syncHistogramAccessibility() {
        if (readoutSpectrumCell == null) {
            return;
        }
        final boolean on = stagePanel != null && stagePanel.histogramOn();
        readoutSpectrumCell.setContentDescription("光谱读数，直方图"
                + (on ? "已显示" : "已隐藏") + "，点按切换");
    }

    /** Show or hide the stage histogram; it never triggers a render. */
    private void toggleHistogram() {
        if (stagePanel == null) {
            return;
        }
        stagePanel.toggleHistogram();
        syncHistogramAccessibility();
        showSnackbar(stagePanel.histogramOn() ? "直方图已显示" : "直方图已隐藏",
                Snackbar.LENGTH_SHORT);
    }

    private void refreshReadoutStrip() {
        final String exposure = OpticsDelta.valueText(
                optics.get(OpticsDelta.Param.EXPOSURE));
        if (stageExposureValue != null) {
            stageExposureValue.setText(exposure);
        }
        if (readoutExposure == null) {
            return;
        }
        readoutExposure.setText(exposure);
        readoutFilm.setText(stockLabel(live().filmStock, filmIds, filmLabels));
        readoutPaper.setText(isPositiveFilm(live().filmStock)
                ? "DIRECT SCAN" : stockLabel(live().printStock, printIds, printLabels));

        final FilmStock stock = currentFilmStock();
        final SpectrumSamples spectrum = stock == null
                ? SpectrumSamples.unavailable() : stock.samples();
        readoutSpectrumValue.setText(spectrum.valueText());
        readoutSpectrumUnit.setText(spectrum.unitText());
        readoutSpectrumDetail.setText(spectrum.detailText());
        if (readoutSpectrumCell != null) {
            final String description;
            if (live().filmStock == null) {
                description = "光谱：未选择胶片";
            } else if (spectrum.available()) {
                description = "光谱：" + spectrum.sampleCount() + " 个采样，"
                        + spectrum.minNm() + " 至 " + spectrum.maxNm() + " 纳米";
            } else {
                description = "光谱：所选胶片没有随附采样";
            }
            readoutSpectrumCell.setContentDescription(description);
        }
    }

    /** The strip's short name for a stock, or the em dash when none is chosen. */
    private String stockLabel(String id, List<String> ids, List<String> labels) {
        if (id == null || id.isEmpty()) {
            return "\u2014";
        }
        final int i = ids.indexOf(id);
        if (i < 0 || i >= labels.size()) {
            return id;
        }
        return StockNames.readout(labels.get(i));
    }

    /** The picker's full name for the same record. */
    private String fullStockLabel(String id, List<String> ids, List<String> labels) {
        if (id == null || id.isEmpty()) {
            return "\u2014";
        }
        final int i = ids.indexOf(id);
        if (i < 0 || i >= labels.size()) {
            return id;
        }
        return StockNames.full(labels.get(i));
    }

    /**
     * Corner readouts over the full-bleed photograph: file and source at the
     * top left, print exposure at the top right, live state at the lower
     * right. The Spectral rail takes the right edge.
     */
            /**
     * Scan inspect's two controls: a checkable M3 chip under the file readout,
     * and an M3 filled-card readout above the shelf handle. The readout is
     * deliberately away from the print-exposure corner, the status line, and
     * the folded shelf so the three never share a corner.
     */
                /** Turn Scan inspect on or off. It never renders and never edits a Look. */
    private void toggleScan() {
        if (scanBitmap() == null) {
            showSnackbar("先导入一张照片");
            return;
        }
        scanVisible = !scanVisible;
        syncScanAvailability();
        if (scanVisible && scanOverlay != null) {
            scanOverlay.requestFocus();
        }
        saveScanStateIntoFrame();
        try {
            DebugSink.text("scan", "visible=" + scanVisible + " fraction=" + scanFraction);
        } catch (Throwable ignored) { }
    }

    /**
     * The picture Scan inspect is allowed to read: the Original while 原图 is
     * showing, the last rendered preview otherwise. No bitmap means no scan —
     * never a mock and never a placeholder number.
     */
    /** The bitmap the stage is showing: the Original or the last preview. */
    private Bitmap displayedBitmap() {
        return showingOriginal ? beforeBitmap : lastRenderedBitmap;
    }

    private Bitmap scanBitmap() {
        if (stageMode != 1) {
            return null;
        }
        return showingOriginal ? beforeBitmap : lastRenderedBitmap;
    }

    /** Put the overlay, its readout, and the chip on one scan state. */
    private void syncScanAvailability() {
        if (stagePanel == null) {
            return;
        }
        final Bitmap bitmap = scanBitmap();
        stagePanel.syncScan(scanVisible, scanFraction, bitmap, bitmap != null);
    }

    /** The overlay reports every move; the readout and the curve follow it. */
    private void onScanSample(ScanSample sample, float fraction) {
        scanFraction = fraction;
        if (scanReadoutRgb != null) {
            scanReadoutRgb.setText(sample.rgbReadoutText());
        }
        if (scanReadoutLambda != null) {
            scanReadoutLambda.setText(sample.lambdaText());
        }
        if (scanReadout != null) {
            scanReadout.setContentDescription(sample.description()
                    + "，位置 " + Math.round(fraction * 100f) + "%");
        }
        if (filmCurve != null) {
            filmCurve.setScanMark(scanVisible && scanBitmap() != null, sample);
        }
    }

    private void saveScanStateIntoFrame() {
        if (currentFrame < 0 || currentFrame >= frames.size()) {
            return;
        }
        final Frame frame = frames.get(currentFrame);
        frame.setScanFraction(scanFraction);
        frame.setScanVisible(scanVisible);
    }

    private void restoreScanStateFromFrame(Frame frame) {
        scanFraction = frame == null ? ScanSample.DEFAULT_FRACTION : frame.scanFraction();
        scanVisible = frame != null && frame.scanVisible();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        // When the scan line has focus its own onKeyDown consumes the key; this
        // path keeps the arrows working while another control has focus, but a
        // focused slider keeps its own arrow keys.
        final View focus = getCurrentFocus();
        if (scanVisible && scanOverlay != null
                && !(focus instanceof Slider)
                && scanOverlay.handleKey(keyCode, event.isShiftPressed())) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /** Solid paper shelf outside the canvas: ＋, one chip per photograph opened
     *  this session, and a way to fold the whole row away.
     *
     *  <p>There is no Original switch here. The film panel's first chip is the
     *  same toggle; two controls for one state, four dp apart, read as two
     *  different things.
     */
    private View buildSourceBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackground(theme.glassSurface(GlassStyle.SOURCE_RADIUS_DP));
        bar.setClipToOutline(true);
        bar.setElevation(dp(GlassStyle.CARD_ELEVATION_DP));
        sourceBar = bar;
        bar.addView(hairline(theme.outlineVariant()),
                new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(4), 0);
        sourceRow = row;

        TextView add = type.m3Label("\uff0b", M3Tokens.TypeRole.TITLE_LARGE,
                theme.primary(), tfDisplay);
        add.setGravity(android.view.Gravity.CENTER);
        add.setFocusable(true);
        add.setContentDescription("导入照片");
        add.setOnClickListener(v -> showImportMenu());
        row.addView(add, new LinearLayout.LayoutParams(
                dp(MIN_TOUCH_DP), dp(MIN_TOUCH_DP)));

        // The strip. Empty until a photograph is opened: no placeholder tile,
        // because an empty shelf is not a missing control.
        thumbScroll = new HorizontalScrollView(this);
        thumbScroll.setHorizontalScrollBarEnabled(false);
        thumbScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        thumbStrip = new LinearLayout(this);
        thumbStrip.setOrientation(LinearLayout.HORIZONTAL);
        thumbStrip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        thumbScroll.addView(thumbStrip, new android.widget.FrameLayout.LayoutParams(-2, -1));
        row.addView(thumbScroll, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView fold = type.m3Label("\u2039", M3Tokens.TypeRole.TITLE_SMALL,
                theme.onSurfaceVariant(), tfHan);
        fold.setGravity(android.view.Gravity.CENTER);
        fold.setFocusable(true);
        fold.setContentDescription("收起胶片条");
        fold.setOnClickListener(v -> setShelfCollapsed(true));
        row.addView(fold, new LinearLayout.LayoutParams(
                dp(MIN_TOUCH_DP), dp(MIN_TOUCH_DP)));

        bar.addView(row, new LinearLayout.LayoutParams(-1, 0, 1f));
        return bar;
    }

    /** Folded: three cards stacked at the canvas's lower-left corner, the way a
     *  short stack of prints looks -- one picture, two edges behind it, and a
     *  chevron that says the rest of the strip is that way. */
    private View buildShelfHandle() {
        final int card = dp(52), step = dp(4);
        android.widget.FrameLayout handle = new android.widget.FrameLayout(this);
        handle.setVisibility(View.GONE);

        for (int i = 2; i >= 1; --i) {
            View edge = new View(this);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(palette.color(i == 2
                    ? Palette.Role.SURFACE_SUNKEN : Palette.Role.SURFACE_ELEVATED));
            bg.setCornerRadius(dp(7));
            bg.setStroke(Math.max(1, dp(1)), palette.color(Palette.Role.LINE));
            edge.setBackground(bg);
            android.widget.FrameLayout.LayoutParams lp =
                    new android.widget.FrameLayout.LayoutParams(card, card);
            lp.leftMargin = step * i;
            lp.topMargin = step * i;
            handle.addView(edge, lp);
        }

        shelfHandleImage = new ImageView(this);
        shelfHandleImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        shelfHandleImage.setClipToOutline(true);
        shelfHandleImage.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(7));
            }
        });
        handle.addView(shelfHandleImage, new android.widget.FrameLayout.LayoutParams(card, card));

        TextView chevron = type.label("\u203a", 14, palette.color(Palette.Role.STAGE_INK), tfDisplay);
        chevron.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        chevron.setPadding(0, 0, dp(5), 0);
        chevron.setShadowLayer(dp(2), 0, 0, palette.color(Palette.Role.STAGE_SHADOW));
        handle.addView(chevron, new android.widget.FrameLayout.LayoutParams(card, card));

        handle.setFocusable(true);
        handle.setContentDescription("展开胶片条");
        handle.setOnClickListener(v -> setShelfCollapsed(false));
        return handle;
    }

    /** Make one Frame's session the live one; the Activity keeps no Look copy. */
    private void attachSession(FrameLookSession session) {
        currentSession = session;
        optics = session == null ? new Look() : session.look();
    }

    /** The live Look: the current Frame's session, or the idle default before one exists. */
    private Look live() {
        return currentSession != null ? currentSession.look() : optics;
    }

    /**
     * Capture the live controls into the current Frame's Look. The session is
     * the source; the legacy Film/Scale/Edge fields are written back into it
     * here, so this is the one transitional bridge until those panels move too.
     * Nothing is clamped on the way in.
     */
    private Look captureLook() {
        final Look look = currentSession != null ? currentSession.look() : new Look();
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            look.set(param, optics.get(param));
        }
        look.cameraFilter = optics.cameraFilter;
        look.sceneNorm = optics.sceneNorm;
        look.view = optics.view.copy();
        return look.copy();
    }

    /** Put a frame's own look back, controls included: a slider that says 1.00
     *  while the render used 1.40 is worse than no slider. The stored value is
     *  restored as-is; the slider only clamps the thumb it can reach, so a
     *  glare above its 12.0 cap still reaches the engine. */
    private void applyLook(Look look) {
        if (look == null) {
            return;
        }
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            optics.set(param, look.get(param));
        }
        // Unknown choice ids stay on the Look so a later save does not erase
        // them; the page shows the documented fallback until the user picks.
        optics.cameraFilter = look.cameraFilter;
        optics.sceneNorm = look.sceneNorm;
        optics.view = look.view.copy();
        final GateScale.Gate pair = GateScale.pair(look.overscanFormat, look.filmFormatMm);
        // One piece of film geometry: a stored Gate and Scale that disagree are
        // pulled onto the Gate's measured edge before either control can draw.
        live().overscanFormat = pair.id;
        live().filmFormatMm = pair.longEdgeMm;
        if (opticsSlider != null) {
            bindActiveOptics();
        }
        syncOpticsChoices();
        syncToggleRows();
        syncEdgeDateDetail();
        if (edgeValue != null) {
            edgeValue.setText(pair.label);
        }
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(pair.longEdgeMm) + " mm");
        }
        refreshFilmPanel();
        refreshPaperLabel();
    }

    /** Park the live controls on the frame that owns them, before leaving it. */
    /** What a brand-new photograph arrives with. Not "whatever was on screen":
     *  a frame that has never been developed has no look, and inheriting the
     *  previous one made an untouched import come back with somebody else's
     *  grain and rebate. */
    private Look defaultLook() {
        final Look look = new Look();
        look.filmStock = null;
        look.printStock = Look.DEFAULT_PRINT_STOCK;
        look.filmFormatMm = GateScale.defaultGate().longEdgeMm;
        look.overscanOn = false;
        look.overscanFormat = GateScale.defaultGate().id;
        look.dateOn = false;
        return look;
    }

    private void saveLookIntoFrames() {
        if (currentFrame < 0 || currentFrame >= frames.size()) {
            return;
        }
        // The current Frame owns the live session already; captureLook writes
        // the legacy Film/Scale/Edge fields into it before the switch.
        captureLook();
        frames.get(currentFrame).setOriginal(showingOriginal);
    }

    private void updateShelfHandle() {
        if (shelfHandleImage == null) {
            return;
        }
        Bitmap thumb = (currentFrame >= 0 && currentFrame < frames.size())
                ? frames.get(currentFrame).thumb() : null;
        shelfHandleImage.setImageBitmap(thumb);
    }

    /** Fold or unfold the photograph strip. Unfolded it costs 62 dp above the
     *  panels; folded it costs 20, and the canvas takes the difference -- the
     *  stage is the weighted child of the column, so nothing else has to move. */
    private void setShelfCollapsed(boolean collapsed) {
        if (shelfCollapsed == collapsed) {
            return;
        }
        // Nothing to fold into: with no photographs the handle would not be
        // drawn, and ＋ would go with the row -- a one-way trip.
        if (collapsed && frames.isEmpty()) {
            return;
        }
        shelfCollapsed = collapsed;
        updateShelfVisibility();
        if (collapsed) {
            updateShelfHandle();
        }
    }

    /** The strip is furniture: it exists only when there is something on it.
     *  Before the first import it is not folded, it is absent -- the canvas's
     *  own 选择照片 button is the way in, and a 62 dp row of nothing above the
     *  panels is just a gap. */
    private void updateShelfVisibility() {
        if (sourceBar == null) {
            return;
        }
        final boolean show = !frames.isEmpty() && !shelfCollapsed;
        sourceBar.setVisibility(show ? View.VISIBLE : View.GONE);
        android.view.ViewGroup.LayoutParams lp = sourceBar.getLayoutParams();
        if (lp != null) {
            lp.height = dp(show ? SOURCE_HEIGHT_DP : 0);
            sourceBar.setLayoutParams(lp);
        }
        if (shelfHandle != null) {
            shelfHandle.setVisibility(shelfCollapsed && !frames.isEmpty()
                    ? View.VISIBLE : View.GONE);
        }
    }

    /** Restore the one current Frame left by the last process, if its source is still readable. */
    private void restoreSessionIfAny() {
        final java.io.File file = SessionDocument.file(getNoBackupFilesDir());
        final SessionDocument document = SessionDocument.read(file);
        if (document == null) {
            if (file.exists()) {
                file.delete();
                showSnackbar("上次的会话记录已损坏，已清除恢复记录");
            }
            return;
        }
        final Uri uri = Uri.parse(document.uri());
        if (!sourceReadable(uri)) {
            file.delete();
            showSnackbar("上次的照片已不可用，已清除恢复记录");
            return;
        }
        pendingSession = document;
        restoringOriginal = document.original();
        setTab(document.tab());
        loadImage(uri);
    }

    /** Check the restored source without decoding it or asking the picker. */
    private boolean sourceReadable(Uri uri) {
        if (uri == null) {
            return false;
        }
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            return in != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Put the restored Look on the live session before any decode starts. */
    private void applyRestoredLook() {
        if (pendingSession == null || currentSession == null) {
            return;
        }
        currentSession.reset(Look.fromJson(pendingSession.look()));
        optics = currentSession.look();
        applyLook(optics);
    }

    /** Finish a still or RAW import: develop a restored/selected Look or stay on Original. */
    private void finishImportRender() {
        refreshFilmPanel();
        final boolean shouldRender = ReopenDecision.rendersOnReopen(
                restoringOriginal, live().filmStock != null);
        if (shouldRender) {
            showingOriginal = false;
            refreshFilmPanel();
            renderImage();
        } else {
            showingOriginal = true;
            statusText.setText(restoringOriginal
                    ? "已恢复 · 原图" : "未渲染 · 选择胶片开始");
        }
        restoringOriginal = false;
    }

    /** Attach the one restored Look and its Scan state to the Frame being filed. */
    private void applyPendingSession(Frame frame) {
        if (pendingSession == null || frame == null) {
            return;
        }
        frame.session().reset(Look.fromJson(pendingSession.look()));
        frame.setScanFraction(pendingSession.scanFraction());
        frame.setScanVisible(pendingSession.scanVisible());
        frame.setOriginal(pendingSession.original());
        pendingSession = null;
    }

    /** Save the current source, Look and page for the next process. */
    private void saveSessionIfAny() {
        if (!uiReady || currentSession == null) {
            return;
        }
        Uri uri = sourceUri;
        if (currentFrame >= 0 && currentFrame < frames.size()) {
            uri = frames.get(currentFrame).uri();
        }
        if (uri == null) {
            return;
        }
        try {
            final SessionDocument document = new SessionDocument(
                    uri.toString(), getFileName(uri), isRawFile, activeTabIndex,
                    scanFraction, scanVisible, showingOriginal, captureLook().toJson());
            document.write(SessionDocument.file(getNoBackupFilesDir()));
        } catch (Throwable ignored) {
            // A failed session write must never break the edit or the export.
        }
    }

    /** File the frame that just finished importing, once. */
    private void rememberFrame(Uri uri) {
        if (uri == null) {
            return;
        }
        String name = getFileName(uri);
        if (retryingNativeSize) {
            // Same photograph, decoded again at its own size. The chip is
            // already there; a second one would read as two files.
            retryingNativeSize = false;
            return;
        }
        if (reopeningFrame) {
            // A chip asked for this frame. It is already in the list; only the
            // highlight and the freshly decoded pixels changed.
            reopeningFrame = false;
            suppressBudgetDialog = false;
            rebuildFrameChips();
            return;
        }
        final Frame frame = new Frame(uri, isRawFile, name, captureLook());
        frame.setScanFraction(scanFraction);
        frame.setScanVisible(scanVisible);
        applyPendingSession(frame);
        if (beforeBitmap != null) {
            try {
                final int w = Math.max(1, dp(42));
                final int h = Math.max(1, Math.round(
                        w * beforeBitmap.getHeight() / (float) beforeBitmap.getWidth()));
                frame.setThumb(Bitmap.createScaledBitmap(beforeBitmap, w, h, true));
            } catch (Throwable ignored) { }
        }
        frames.add(frame);
        currentFrame = frames.size() - 1;
        attachSession(frame.session());
        restoreScanStateFromFrame(frame);
        syncScanAvailability();
        applyLook(optics);
        rebuildFrameChips();
    }

    /** File a batch selection as Frames, then open the first one. */
    private void startBatchImport(final java.util.List<Uri> uris) {
        if (batchImportRunning) {
            showSnackbar("批量导入已经在跑");
            return;
        }
        if (uris == null || uris.isEmpty()) {
            return;
        }
        if (uris.size() == 1) {
            loadImage(uris.get(0));
            return;
        }
        batchImportRunning = true;
        final int total = uris.size();
        statusText.setText("正在加入 " + total + " 张照片…");
        new Thread(() -> {
            final Bitmap[] thumbs = new Bitmap[total];
            try {
                for (int i = 0; i < total; ++i) {
                    thumbs[i] = systemThumb(uris.get(i));
                }
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    batchImportRunning = false;
                    showSnackbar("批量导入失败：" + describe(t));
                });
                return;
            }
            runOnUiThread(() -> finishBatchImport(uris, thumbs));
        }, "batch-import").start();
    }

    /** Add the prepared selection on the UI thread; no decode happens here. */
    private void finishBatchImport(java.util.List<Uri> uris, Bitmap[] thumbs) {
        int added = 0;
        int failed = 0;
        for (int i = 0; i < uris.size(); ++i) {
            if (addFrameOnly(uris.get(i), thumbs[i])) {
                ++added;
            } else {
                ++failed;
            }
        }
        batchImportRunning = false;
        final String msg = "已加入 " + added + " 张"
                + (failed > 0 ? "，跳过 " + failed + " 张" : "");
        statusText.setText(msg);
        showSnackbar(msg);
        rebuildFrameChips();
        if (added > 0 && currentFrame < 0 && !frames.isEmpty()) {
            reopenFrame(frames.size() - added);
        }
    }

    /** Register one URI without decoding it; opening the chip does the decode. */
    private boolean addFrameOnly(Uri uri, Bitmap thumb) {
        if (uri == null) {
            return false;
        }
        final String name = getFileName(uri);
        final String lower = name == null ? "" : name.toLowerCase(java.util.Locale.US);
        if (ImageFormat.unsupportedRawExtension(lower) != null) {
            return false;
        }
        final Frame frame = new Frame(uri, ImageFormat.isRaw(lower), name, defaultLook());
        frame.session().look().meterMethod =
                ImageFormat.isRaw(lower) ? "protect_highlights" : "";
        if (thumb != null) {
            frame.setThumb(thumb);
        }
        frames.add(frame);
        return true;
    }

    /** A small system thumbnail, when the provider can make one. */
    private Bitmap systemThumb(Uri uri) {
        if (android.os.Build.VERSION.SDK_INT < 29) {
            return null;
        }
        try {
            return getContentResolver().loadThumbnail(uri,
                    new android.util.Size(96, 96), null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** One chip per opened frame, newest at the right, the open one ringed. */
    private void rebuildFrameChips() {
        if (thumbStrip == null) {
            return;
        }
        thumbStrip.removeAllViews();
        for (int i = 0; i < frames.size(); ++i) {
            final int index = i;
            final Frame frame = frames.get(i);
            final boolean active = index == currentFrame;
            android.widget.FrameLayout box = new android.widget.FrameLayout(this);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(theme.surface());
            bg.setStroke(dp(active ? 2 : 1),
                    active ? theme.primary() : theme.outlineVariant());
            box.setBackground(bg);
            box.setPadding(dp(3), dp(3), dp(3), dp(3));
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (frame.thumb() != null) {
                img.setImageBitmap(frame.thumb());
            }
            box.addView(img, new android.widget.FrameLayout.LayoutParams(-1, -1));
            box.setOnClickListener(v -> reopenFrame(index));
            box.setOnLongClickListener(v -> {
                showActionSheet("删除这张照片？",
                        (frame.name() == null ? "照片" : frame.name()) + "\n\n只从这条栏里移除，文件本身不动。",
                        new String[]{"删除", "取消"}, which -> {
                            if (which == 0) {
                                removeFrame(index);
                            }
                        });
                return true;
            });
            box.setFocusable(true);
            box.setSelected(active);
            box.setContentDescription((active ? "当前照片，" : "")
                    + (frame.name() == null ? "照片" : frame.name()));
            box.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override public void onInitializeAccessibilityNodeInfo(
                        View host, android.view.accessibility.AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(android.widget.RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(active);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(MIN_TOUCH_DP), dp(MIN_TOUCH_DP));
            lp.rightMargin = dp(6);
            thumbStrip.addView(box, lp);
        }
        if (currentFrame >= 0 && currentFrame < thumbStrip.getChildCount()) {
            final View target = thumbStrip.getChildAt(currentFrame);
            thumbStrip.post(() -> {
                if (thumbScroll != null && target != null) {
                    thumbScroll.smoothScrollTo(Math.max(0, target.getLeft() - dp(36)), 0);
                }
            });
        }
        updateShelfHandle();
        updateShelfVisibility();
        try {
            DebugSink.text("frame-strip", "frames=" + frames.size() + " current=" + currentFrame);
        } catch (Throwable ignored) { }
    }

    /** Drop a chip. The file is untouched -- this is the strip's own list. If
     *  the frame was the one on the canvas, the canvas is emptied rather than
     *  silently showing a photograph that is no longer in the list. */
    private void removeFrame(int index) {
        if (index < 0 || index >= frames.size()) {
            return;
        }
        frames.remove(index);
        if (index == currentFrame) {
            currentFrame = -1;
            currentSession = null;
            cancelLivePreview();
            renderEpoch.bump();
            clearBusyPlate();
            optics = new Look();
            lastRenderedBitmap = null;
            scanVisible = false;
            scanFraction = ScanSample.DEFAULT_FRACTION;
            if (compare != null) {
                compare.clear();
            }
            setStage(0, null);
            statusText.setText("等待导入");
        } else if (currentFrame > index) {
            currentFrame--;
        }
        rebuildFrameChips();
        try { DebugSink.text("frame-strip", "removed " + index + ", now " + frames.size()); }
        catch (Throwable ignored) { }
    }

    /** Re-open a frame from the strip. Same entry point the picker uses, so a
     *  RAW chip and a JPEG chip take exactly the roads they took the first time. */
    private void reopenFrame(int index) {
        if (index < 0 || index >= frames.size()) {
            return;
        }
        if (index == currentFrame && session != null) {
            return;
        }
        final Frame frame = frames.get(index);
        saveLookIntoFrames();
        saveScanStateIntoFrame();
        currentFrame = index;
        reopeningFrame = true;
        restoringOriginal = frame.original();
        suppressBudgetDialog = true;
        rebuildFrameChips();
        loadImage(frame.uri());
    }

    /** Reopen even the frame already on screen; a batch export needs its own session. */
    private void reopenFrameForced(int index) {
        if (index < 0 || index >= frames.size()) {
            return;
        }
        final Frame frame = frames.get(index);
        saveLookIntoFrames();
        saveScanStateIntoFrame();
        currentFrame = index;
        reopeningFrame = true;
        restoringOriginal = frame.original();
        suppressBudgetDialog = true;
        rebuildFrameChips();
        loadImage(frame.uri());
    }

    private void setOriginalVisual(boolean on) {
        if (originalAction == null) {
            return;
        }
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(palette.color(on ? Palette.Role.ACCENT : Palette.Role.SURFACE_ELEVATED));
        originalAction.setBackground(bg);
        if (originalGlyph != null) {
            originalGlyph.setColor(palette.color(on ? Palette.Role.ON_ACCENT : Palette.Role.ACCENT));
        }
        if (originalLabel != null) {
            originalLabel.setTextColor(palette.color(on ? Palette.Role.ON_ACCENT : Palette.Role.ACCENT));
        }
    }

    private View sectionHead(String title, Runnable reset) {
        return sectionHead(title, null, null, reset);
    }

    private View sectionHead(String title, String actionLabel, Runnable action, Runnable reset) {
        return sectionHead(title, actionLabel, action, reset, null);
    }

    private View sectionHead(String title, String actionLabel, Runnable action,
                             Runnable reset, Runnable presets) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(largeText() ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setMinimumHeight(dp(SECTION_HEAD_HEIGHT_DP));
        TextView titleView = type.mixedM3Label(title, M3Tokens.TypeRole.TITLE_SMALL, theme.onSurface());
        titleView.setMaxLines(2);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        head.addView(titleView, new LinearLayout.LayoutParams(
                largeText() ? -1 : 0, -2, largeText() ? 0f : 1f));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.END);
        if (presets != null) {
            MaterialButton presetView = m3TextButton("预设", "预设：保存、套用或分享",
                    theme.primary(), presets);
            actions.addView(presetView, new LinearLayout.LayoutParams(-2, dp(48)));
        }
        if (actionLabel != null && action != null) {
            MaterialButton actionView = m3TextButton(actionLabel, actionLabel,
                    theme.primary(), action);
            actions.addView(actionView, new LinearLayout.LayoutParams(-2, dp(48)));
        }
        MaterialButton resetView = m3TextButton("重置当前页", "重置当前页",
                theme.onSurfaceVariant(), reset);
        actions.addView(resetView, new LinearLayout.LayoutParams(-2, dp(48)));
        head.addView(actions, largeText()
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(-2, -2));
        return head;
    }

    /** The brand facet row above the film shelf. */
        /** The horizontal film chip shelf. */
        /** The spectral curve card. */
        /** The measured-value row and the film note. */
        /** The paper choice row. */
        private View buildFilmPanel() {
        filmPanel = new FilmPanel(this, type, theme, palette, tfBody, tfHan,
                this::showPaperPicker,
                sectionHead("FILM STOCK / 胶片", "全部胶片",
                        this::showFilmPicker, this::resetFilmPanel,
                        this::showPresetsPanel),
                FILM_CARD_HEIGHT_DP, FILM_CURVE_HEIGHT_DP, FILM_METRICS_HEIGHT_DP,
                PAPER_ROW_HEIGHT_DP, MIN_TOUCH_DP);
        brandRow = filmPanel.brandRow();
        filmChipScroll = filmPanel.chipScroll();
        filmChipRow = filmPanel.chipRow();
        filmCurveCard = filmPanel.curveCard();
        filmCurve = filmPanel.curve();
        filmMetricsRow = filmPanel.metricsRow();
        filmNote = filmPanel.filmNote();
        paperValue = filmPanel.paperValue();
        return filmPanel.view();
    }

    /** A film record currently on the strip, or null when none is chosen. */
    private FilmStock currentFilmStock() {
        return live().filmStock == null ? null : filmStocks.get(live().filmStock);
    }

    /** Rebuild the chips, curve, measured values, and note from the Look. */
    private void refreshFilmPanel() {
        refreshFilmPanel(false);
    }

    /** A brand change starts a new shelf; other refreshes keep the chip scroll. */
    private void refreshFilmPanel(boolean resetScroll) {
        if (filmChipRow == null) {
            return;
        }
        final int keepScroll = resetScroll || filmChipScroll == null
                ? 0 : filmChipScroll.getScrollX();
        refreshBrandRow();
        filmChipRow.removeAllViews();
        addFilmChip("original", "原图", "ORIGINAL", showingOriginal);

        // The active stock is the one the curve and the readout describe. If
        // another brand is selected, FilmBrand.shelf pins it first so the
        // shelf never hides the Look that is still in force.
        final String activeId = live().filmStock == null || showingOriginal ? null : live().filmStock;
        refreshFilmShelf(activeId);
        if (filmChipScroll != null) {
            filmChipScroll.post(() -> filmChipScroll.scrollTo(keepScroll, 0));
        }
        final FilmStock stock = currentFilmStock();
        refreshFilmCurve(stock);
        refreshFilmDetails(stock);
    }

    private void refreshFilmShelf(String activeId) {
        for (String id : FilmBrand.shelf(filmIds, brandFilter, activeId)) {
            final int i = filmIds.indexOf(id);
            if (i < 0) {
                continue;
            }
            addFilmChip(id, StockNames.shelf(filmLabels.get(i)),
                    chipSubtitle(i, filmStocks.get(id)),
                    activeId != null && id.equals(activeId));
        }
    }

    private void refreshFilmCurve(FilmStock stock) {
        final SpectrumCurve nextCurve =
                stock == null ? SpectrumCurve.unavailable() : stock.curve();
        // A new curve reveals once. A render-state refresh re-sends the same
        // object, and a theme rebuild finds it already revealed, so neither
        // replays the plotter. Reduced motion shortens the reveal to zero.
        final boolean revealNow = nextCurve.available() && nextCurve != revealedCurve;
        filmCurve.setCurve(nextCurve,
                stock == null ? "" : stock.seriesLabel(),
                live().filmStock == null ? "NO FILM STOCK SELECTED" : SpectrumCurve.UNAVAILABLE_LABEL,
                revealNow ? motionDuration(FILM_CURVE_REVEAL_MS) : 0);
        if (revealNow) {
            revealedCurve = nextCurve;
        }
    }

    private void refreshFilmDetails(FilmStock stock) {
        filmCurve.setContentDescription(curveDescription(stock));
        refreshFilmMetrics(stock);
        filmNote.setText(filmNoteText(stock));
        refreshReadoutStrip();
        // The curve mark and the scan line share one fraction; a refresh must
        // never leave the mark on the previous stock or theme.
        syncScanAvailability();
    }

    /** The brand facet: M3 filter chips, with counts from the catalogue. */
    private void refreshBrandRow() {
        if (brandRow == null) {
            return;
        }
        brandRow.removeAllViews();
        for (String id : FilmBrand.order()) {
            final int count = FilmBrand.count(filmIds, id);
            if (count == 0 && !FilmBrand.ALL.equals(id)) {
                continue;
            }
            final boolean active = id.equals(brandFilter);
            final Chip chip = new Chip(this);
            // A real filter chip, not an assist chip with a checkmark bolted
            // on: the style owns its shape and typography, while the colour
            // state lists are resolved from the theme at build time. Without
            // these explicit lists a theme rebuild keeps the ChipDrawable's
            // previous dark scheme after Dark -> Light.
            chip.setChipDrawable(ChipDrawable.createFromAttributes(this, null,
                    0,
                    com.google.android.material.R.style.Widget_Material3_Chip_Filter));
            final int chipSurface = theme.role(
                    com.google.android.material.R.attr.colorSurfaceContainerLow,
                    Palette.Role.SURFACE_ELEVATED);
            final int chipSelected = theme.role(
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    Palette.Role.ACCENT_SOFT);
            final int chipText = theme.role(
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    Palette.Role.MUTED_INK);
            final int chipTextSelected = theme.role(
                    com.google.android.material.R.attr.colorOnSecondaryContainer,
                    Palette.Role.ACCENT);
            final int chipOutline = theme.role(
                    com.google.android.material.R.attr.colorOutlineVariant,
                    Palette.Role.LINE);
            chip.setChipBackgroundColor(M3Chrome.checkedColors(chipSelected, chipSurface));
            chip.setChipStrokeWidth(Math.max(1, dp(1)));
            chip.setChipStrokeColor(M3Chrome.checkedColors(chipSelected, chipOutline));
            chip.setTextColor(M3Chrome.checkedColors(chipTextSelected, chipText));
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setChecked(active);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setText(type.mixedText(FilmBrand.label(id) + " " + count));
            chip.setContentDescription(FilmBrand.label(id) + "，" + count + " 支胶片"
                    + (active ? "，已选中" : "，筛选"));
            chip.setOnClickListener(v -> {
                if (id.equals(brandFilter)) {
                    return;
                }
                brandFilter = id;
                refreshFilmPanel(true);
            });
            brandRow.addView(chip,
                    new android.view.ViewGroup.LayoutParams(-2, dp(MIN_TOUCH_DP)));
        }
    }

    /** The small line under a chip: the measured type and speed, or the stock's role. */
    private String chipSubtitle(int index, FilmStock stock) {
        if (stock != null) {
            final String iso = stock.info().get("iso");
            if (iso != null) {
                return "ISO " + iso;
            }
            final String type = stock.info().get("type");
            if (type != null) {
                return type.toUpperCase(java.util.Locale.US);
            }
        }
        if (index >= 0 && index < filmTypes.size()
                && "positive".equals(filmTypes.get(index))) {
            return "REVERSAL · SCAN";
        }
        return "FILM";
    }

    /** The text, description, accessibility role and click of a film card. */
    private void buildFilmChipContent(MaterialCardView card, String id, String title,
                                       String subtitle, boolean active,
                                       android.content.res.ColorStateList ink,
                                       android.content.res.ColorStateList muted) {
LinearLayout content = new LinearLayout(this);
content.setOrientation(LinearLayout.VERTICAL);
content.setGravity(android.view.Gravity.CENTER_VERTICAL);
content.setPadding(dp(8), dp(4), dp(24), dp(4));

TextView name = type.m3Label(title, M3Tokens.TypeRole.LABEL_MEDIUM, theme.onSurface(), tfHan);
name.setTextColor(ink);
name.setSingleLine(true);
name.setEllipsize(android.text.TextUtils.TruncateAt.END);
content.addView(name, new LinearLayout.LayoutParams(-1, -2));

TextView kind = type.m3Label(subtitle, M3Tokens.TypeRole.LABEL_SMALL, theme.onSurfaceVariant(), tfBody);
kind.setTextColor(muted);
kind.setSingleLine(true);
kind.setEllipsize(android.text.TextUtils.TruncateAt.END);
LinearLayout.LayoutParams kindLp = new LinearLayout.LayoutParams(-1, -2);
kindLp.topMargin = dp(1);
content.addView(kind, kindLp);
card.addView(content, new android.widget.FrameLayout.LayoutParams(-1, -1));

card.setContentDescription(title + (subtitle.isEmpty() ? "" : "，" + subtitle)
        + (active ? "，已选中，双击切换" : "，未选中，双击选择"));
card.setAccessibilityDelegate(new View.AccessibilityDelegate() {
    @Override public void onInitializeAccessibilityNodeInfo(
            View host, android.view.accessibility.AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        info.setClassName(android.widget.RadioButton.class.getName());
        info.setCheckable(true);
        info.setChecked(active);
    }
});
card.setOnClickListener(v -> {
    if ("original".equals(id)) {
        toggleOriginal();
    }
    else {
        pickFilmChip(id);
    }
});
    }

    private void addFilmChip(final String id, String title, String subtitle, boolean active) {
        // A real MaterialCardView carries pressed, focused, selected, and
        // disabled state; the state lists below only name M3 roles, so a
        // dynamic scheme and the brand fallback both read as the same component.
        final int[][] states = new int[][]{
                { android.R.attr.state_enabled, android.R.attr.state_checked },
                { android.R.attr.state_enabled },
                { -android.R.attr.state_enabled },
        };
        final int checkedBg = theme.role(com.google.android.material.R.attr.colorSecondaryContainer,
                Palette.Role.ACCENT_SOFT);
        final int restBg = theme.role(com.google.android.material.R.attr.colorSurfaceContainerHighest,
                Palette.Role.SURFACE_ELEVATED);
        final int disabledBg = theme.role(com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE);
        final int checkedInk = theme.role(com.google.android.material.R.attr.colorOnSecondaryContainer,
                Palette.Role.ON_ACCENT);
        final int restInk = theme.role(com.google.android.material.R.attr.colorOnSurface,
                Palette.Role.INK);
        final int restMuted = theme.role(com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
        final int disabledInk = M3Chrome.withOpacity(restMuted, M3Tokens.DISABLED_CONTENT_ALPHA);
        final android.content.res.ColorStateList ink = new android.content.res.ColorStateList(
                states, new int[]{checkedInk, restInk, disabledInk});
        final android.content.res.ColorStateList muted = new android.content.res.ColorStateList(
                states, new int[]{M3Chrome.withOpacity(checkedInk, M3Tokens.MEDIUM_EMPHASIS_ALPHA),
                        restMuted, disabledInk});

        final MaterialCardView card = new MaterialCardView(this);
        card.setCheckable(true);
        card.setChecked(active);
        card.setClickable(true);
        card.setFocusable(true);
        card.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(new android.content.res.ColorStateList(
                states, new int[]{checkedBg, restBg, disabledBg}));
        card.setStrokeColor(new android.content.res.ColorStateList(states, new int[]{
                theme.role(com.google.android.material.R.attr.colorOutline, Palette.Role.ACCENT),
                theme.role(com.google.android.material.R.attr.colorOutlineVariant, Palette.Role.LINE),
                M3Chrome.withOpacity(restMuted, M3Tokens.StateLayer.DRAGGED.opacity)}));
        card.setCheckedIconSize(dp(16));
        card.setCheckedIconMargin(dp(4));
        card.setCheckedIconGravity(MaterialCardView.CHECKED_ICON_GRAVITY_TOP_END);
        card.setCheckedIconTint(android.content.res.ColorStateList.valueOf(checkedInk));

        buildFilmChipContent(card, id, title, subtitle, active, ink, muted);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(FILM_CARD_WIDTH_DP + (largeText() ? 20 : 0)), dp(FILM_CARD_HEIGHT_DP));
        lp.rightMargin = dp(FILM_CARD_GAP_DP);
        filmChipRow.addView(card, lp);
    }

    /** A stock chip is a Look choice: same path as any other stock change. */
    private void pickFilmChip(String id) {
        final int index = filmIds.indexOf(id);
        if (index < 0) {
            return;
        }
        if (id.equals(live().filmStock) && !showingOriginal) {
            return;
        }
        live().filmStock = id;
        // Upstream's `selectFilmStock` rule: the Film the user picks brings its
        // own paper. An explicit paper chosen afterwards stays until the next
        // Film selection; a Frame restore keeps its stored Film/paper pair.
        final FilmStock chosen = filmStocks.get(id);
        live().printStock = PrintTarget.choose(live().printStock,
                chosen == null ? null : chosen.targetPrint(), printIds);
        showingOriginal = false;
        refreshFilmPanel();
        refreshPaperLabel();
        onStockChanged();
    }

    /**
     * A reversal/slide is developed and scanned directly; the engine's
     * `scan_film` replaces the paper stage. That is why the paper row is not
     * what makes it printable, and why the old refusal was a UI limit rather
     * than an engine one.
     */
    private boolean isPositiveFilm(String id) {
        final int index = id == null ? -1 : filmIds.indexOf(id);
        return index >= 0 && index < filmTypes.size()
                && "positive".equals(filmTypes.get(index));
    }

    /** Measured values only: a field the profile does not ship stays absent. */
    private void refreshFilmMetrics(FilmStock stock) {
        if (filmMetricsRow == null) {
            return;
        }
        filmMetricsRow.removeAllViews();
        if (stock == null) {
            final String label = live().filmStock == null ? "—" : stockLabel(
                    live().filmStock, filmIds, filmLabels);
            addMetricCell("FILM", label);
            return;
        }
        final java.util.List<String[]> rows = new java.util.ArrayList<>();
        addMetric(rows, stock, "type", "TYPE");
        addMetric(rows, stock, "iso", "ISO");
        addMetric(rows, stock, "balance", "BALANCE");
        addMetric(rows, stock, "use", "USE");
        addMetric(rows, stock, "antihalation", "ANTIHALATION");
        addMetric(rows, stock, "channel_model", "MODEL");
        addMetric(rows, stock, "support", "SUPPORT");
        addMetric(rows, stock, "stage", "STAGE");
        final int count = Math.min(4, rows.size());
        for (int i = 0; i < count; ++i) {
            addMetricCell(rows.get(i)[0], rows.get(i)[1]);
        }
    }

    private void addMetric(java.util.List<String[]> rows, FilmStock stock,
                           String key, String caption) {
        final String value = stock.info().get(key);
        if (value == null || value.isEmpty()) {
            return;
        }
        rows.add(new String[]{caption, value.toUpperCase(java.util.Locale.US)});
    }

    private void addMetricCell(String caption, String value) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(dp(1), dp(2), dp(6), dp(1));
        TextView head = type.m3Label(caption, M3Tokens.TypeRole.LABEL_SMALL,
                theme.onSurfaceVariant(), tfBody);
        head.setSingleLine(true);
        head.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.addView(head, new LinearLayout.LayoutParams(-1, -2));
        TextView text = type.m3Label(value, M3Tokens.TypeRole.LABEL_MEDIUM,
                theme.onSurface(), tfHan);
        text.setSingleLine(true);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.addView(text, new LinearLayout.LayoutParams(-1, -2));
        filmMetricsRow.addView(cell, new LinearLayout.LayoutParams(0, -1, 1f));
    }

    /** A short factual line from the profile; a missing field is not invented. */
    private String filmNoteText(FilmStock stock) {
        if (live().filmStock == null) {
            return "SELECT A STOCK TO SEE ITS MEASURED RESPONSE.";
        }
        if (stock == null) {
            return "CATALOGUE RECORD MISSING FOR " + live().filmStock.toUpperCase(java.util.Locale.US);
        }
        final String description = stock.info().get("description");
        if (isPositiveFilm(live().filmStock)) {
            final String scan = "REVERSAL · DIRECT SCAN · PAPER NOT USED";
            return description != null && !description.isEmpty()
                    ? scan + " · " + description : scan;
        }
        if (description != null && !description.isEmpty()) {
            return description;
        }
        final java.util.List<String> parts = stockNotes(stock);
        if (parts.isEmpty()) {
            return "NO STOCK NOTES SHIPPED.";
        }
        return joinNotes(parts);
    }

    /** The declared paper, halation and fallback support/stage notes. */
    private java.util.List<String> stockNotes(FilmStock stock) {
        final java.util.List<String> parts = new java.util.ArrayList<>();
        // The same declaration the Film choice applies, so the note cannot
        // claim a paper the Look would not move to.
        final String target = stock.targetPrint();
        if (target != null && !target.isEmpty()) {
            parts.add("TARGET " + stockLabel(target, printIds, printLabels)
                    .toUpperCase(java.util.Locale.US));
        }
        final String antihalation = stock.info().get("antihalation");
        if (antihalation != null && !antihalation.isEmpty()) {
            parts.add("ANTIHALATION " + antihalation.toUpperCase(java.util.Locale.US));
        }
        if (parts.isEmpty()) {
            final String support = stock.info().get("support");
            final String stage = stock.info().get("stage");
            if (support != null) {
                parts.add(support.toUpperCase(java.util.Locale.US));
            }
            if (stage != null) {
                parts.add(stage.toUpperCase(java.util.Locale.US));
            }
        }
        return parts;
    }

    private String joinNotes(java.util.List<String> parts) {
        final StringBuilder note = new StringBuilder();
        for (String part : parts) {
            if (note.length() > 0) {
                note.append(" · ");
            }
            note.append(part);
        }
        return note.toString();
    }

    private String curveDescription(FilmStock stock) {
        if (live().filmStock == null) {
            return "光谱：未选择胶片";
        }
        if (stock == null || !stock.curve().available()) {
            return "光谱：所选胶片没有随附采样，曲线不可用";
        }
        return "光谱：" + stock.seriesLabel() + "，" + stock.curve().sampleCount()
                + " 个采样，" + Math.round(stock.curve().minNm()) + " 至 "
                + Math.round(stock.curve().maxNm()) + " 纳米";
    }

    private void refreshPaperLabel() {
        if (paperValue != null) {
            // The picker value is the full catalogue name; only the Readout
            // strip shortens it. Both read the same printLabels record.
            paperValue.setText(fullStockLabel(live().printStock, printIds, printLabels));
        }
        refreshReadoutStrip();
    }

    /**
     * The film picker is the full catalogue, not the brand-filtered shelf. The
     * shelf card and the Readout strip keep their short working names; the
     * sheet lists the record's full label.
     */
    private void showFilmPicker() {
        if (filmIds.isEmpty()) {
            showSnackbar("片库还没有载入");
            return;
        }
        final List<String> names = new ArrayList<>();
        names.add("原图");
        names.addAll(filmLabels);
        final int filmIndex = live().filmStock == null ? -1 : filmIds.indexOf(live().filmStock);
        final int current = showingOriginal ? 0 : (filmIndex < 0 ? -1 : 1 + filmIndex);
        showChoiceSheet("选择胶片", "共 " + filmIds.size() + " 支，品牌标签只筛片架",
                names, current, picked -> {
                    if (picked == 0) {
                        if (!showingOriginal) {
                            toggleOriginal();
                        }
                        return;
                    }
                    final int index = picked - 1;
                    if (index >= 0 && index < filmIds.size()) {
                        pickFilmChip(filmIds.get(index));
                    }
                });
    }

    /** Built-in looks plus the user's saved Looks. */
    private void showPresetsPanel() {
        final java.util.List<BuiltInLook> looks = BuiltInLook.all();
        final org.json.JSONArray presets = readPresets();
        final java.util.List<String> labels = new java.util.ArrayList<>();
        final java.util.List<LookPreset> parsed = new java.util.ArrayList<>();
        for (BuiltInLook look : looks) {
            labels.add("外观 · " + look.name());
        }
        labels.add("保存当前预设");
        for (int i = 0; i < presets.length(); ++i) {
            final LookPreset preset = LookPreset.fromJson(presets.optJSONObject(i));
            if (preset != null) {
                parsed.add(preset);
                labels.add(preset.name().isEmpty() ? "未命名预设" : preset.name());
            }
        }
        labels.add("从剪贴板导入");
        final int saved = looks.size();
        final int importAt = labels.size() - 1;
        showActionSheet("外观 / 预设",
                "内置外观只覆盖胶片、相纸和外观参数；预设保存完整 Look",
                labels.toArray(new String[0]), which -> {
                    if (which < saved) {
                        showBuiltInLook(looks.get(which));
                    } else if (which == saved) {
                        saveCurrentPreset();
                    } else if (which == importAt) {
                        importPresetFromClipboard();
                    } else {
                        showPresetActions(parsed.get(which - saved - 1));
                    }
                });
    }

    private void showBuiltInLook(BuiltInLook look) {
        if (currentSession == null) {
            showSnackbar("先导入照片，再套用外观");
            return;
        }
        final Look base = live().copy();
        showPresetAmountSheet("外观 · " + look.name(), builtInLookSubtitle(look),
                base, look.apply(base));
    }

    private String builtInLookSubtitle(BuiltInLook look) {
        final String filmName = stockLabel(look.filmId(), filmIds, filmLabels);
        if (isPositiveFilm(look.filmId())) {
            return filmName + " · 正片直扫";
        }
        return filmName + " · " + stockLabel(look.printId(), printIds, printLabels);
    }

    private void showPresetActions(LookPreset preset) {
        if (preset == null) {
            return;
        }
        showActionSheet(preset.name(), "应用会写入当前帧并渲染一次",
                new String[]{"应用", "按强度应用", "分享 JSON", "删除"}, which -> {
                    if (which == 0) {
                        applyPreset(preset);
                    } else if (which == 1) {
                        showPresetAmountSheet("预设 · " + preset.name(),
                                "从当前画面过渡到这个预设",
                                live().copy(), preset.look());
                    } else if (which == 2) {
                        sharePreset(preset);
                    } else if (which == 3) {
                        deletePreset(preset.name());
                    }
                });
    }

    /**
     * A draft strength sheet for a built-in look or a complete preset. The
     * slider only changes the label; applying writes one settled edit and
     * renders once, so a cancelled sheet leaves the Frame untouched.
     */
    private void showPresetAmountSheet(String title, String subtitle,
                                       Look base, Look full) {
        if (currentSession == null) {
            showSnackbar("先导入照片，再套用外观");
            return;
        }
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final LinearLayout column = sheetColumn(dialog, title);

        final TextView note = type.m3Label(subtitle, M3Tokens.TypeRole.BODY_MEDIUM,
                theme.onSurfaceVariant(), tfHan);
        note.setSingleLine(false);
        column.addView(note, new LinearLayout.LayoutParams(-1, -2));

        final TextView value = type.m3Label("100%", M3Tokens.TypeRole.TITLE_MEDIUM,
                theme.onSurface(), tfBody);
        value.setPadding(0, dp(8), 0, 0);
        column.addView(value, new LinearLayout.LayoutParams(-1, -2));

        final Slider slider = new Slider(this);
        slider.setValueFrom(0f);
        slider.setValueTo(100f);
        slider.setStepSize(1f);
        slider.setValue(100f);
        slider.setMinimumHeight(dp(MIN_TOUCH_DP));
        slider.setContentDescription("预设强度");
        ViewCompat.setStateDescription(slider, "100%");
        slider.setLabelBehavior(
                com.google.android.material.slider.LabelFormatter.LABEL_GONE);
        slider.addOnChangeListener((s, v, fromUser) -> {
            final String percent = Math.round(v) + "%";
            value.setText(percent);
            ViewCompat.setStateDescription(slider, percent);
        });
        column.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));

        final MaterialButton apply = actionButton("应用", "按当前强度应用外观");
        apply.setOnClickListener(v -> {
            final int percent = Math.round(slider.getValue());
            dialog.dismiss();
            if (percent <= 0) {
                return;
            }
            applyLookToCurrentFrame(PresetAmount.blend(base, full, percent / 100f));
        });
        column.addView(apply, new LinearLayout.LayoutParams(-1, dp(48)));
        showSheet(dialog, scrollableSheet(column));
    }

    private void saveCurrentPreset() {
        if (live().filmStock == null) {
            showSnackbar("先选一张胶片，再保存预设");
            return;
        }
        askText("保存预设", "", name -> {
            final String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty()) {
                showSnackbar("预设需要名字");
                return;
            }
            final boolean stored = storePreset(new LookPreset(trimmed, captureLook()));
            showSnackbar(stored ? "已保存预设：" + trimmed : "预设已满，最多 " + PRESET_LIMIT + " 条");
        });
    }

    private void applyPreset(LookPreset preset) {
        if (preset == null || preset.look().filmStock == null) {
            showSnackbar("这个预设没有胶片");
            return;
        }
        applyLookToCurrentFrame(preset.look());
    }

    /** Write one complete Look into the live Frame, then render once. */
    private void applyLookToCurrentFrame(Look next) {
        if (session == null || currentSession == null) {
            showSnackbar("先导入照片");
            return;
        }
        if (next == null || next.filmStock == null) {
            showSnackbar("这个外观没有胶片");
            return;
        }
        currentSession.edit(look -> look.copyFrom(next));
        optics = currentSession.look();
        applyLook(optics);
        refreshReadoutStrip();
        refreshPaperLabel();
        syncToggleRows();
        syncEdgeDateDetail();
        updateUndoAction();
        renderImage();
    }

    private void sharePreset(LookPreset preset) {
        final Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, preset.toJson().toString());
        startActivity(Intent.createChooser(send, "分享预设"));
    }

    private void importPresetFromClipboard() {
        try {
            final android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip()
                    || clipboard.getPrimaryClip() == null
                    || clipboard.getPrimaryClip().getItemCount() == 0) {
                showSnackbar("剪贴板里没有预设");
                return;
            }
            final String text = clipboard.getPrimaryClip().getItemAt(0)
                    .coerceToText(this).toString();
            final org.json.JSONObject json = new org.json.JSONObject(text);
            LookPreset preset = LookPreset.fromJson(json);
            if (preset == null) {
                preset = new LookPreset("剪贴板", Look.fromJson(json));
            }
            if (preset.look().filmStock == null) {
                showSnackbar("剪贴板不是有效的预设");
                return;
            }
            final boolean stored = storePreset(preset);
            showSnackbar(stored ? "已导入预设：" + preset.name()
                    : "预设已满，最多 " + PRESET_LIMIT + " 条");
        } catch (Throwable t) {
            showSnackbar("预设解析失败：" + describe(t));
        }
    }

    private boolean storePreset(LookPreset preset) {
        final org.json.JSONArray current = readPresets();
        final org.json.JSONArray next = new org.json.JSONArray();
        boolean replaced = false;
        for (int i = 0; i < current.length(); ++i) {
            final LookPreset old = LookPreset.fromJson(current.optJSONObject(i));
            if (old != null && old.name().equals(preset.name())) {
                if (!replaced) {
                    next.put(preset.toJson());
                    replaced = true;
                }
            } else if (old != null) {
                next.put(old.toJson());
            }
        }
        if (!replaced && next.length() >= PRESET_LIMIT) {
            return false;
        }
        if (!replaced) {
            next.put(preset.toJson());
        }
        writePresets(next);
        return true;
    }

    private void deletePreset(String name) {
        final org.json.JSONArray current = readPresets();
        final org.json.JSONArray next = new org.json.JSONArray();
        for (int i = 0; i < current.length(); ++i) {
            final LookPreset old = LookPreset.fromJson(current.optJSONObject(i));
            if (old != null && !old.name().equals(name)) {
                next.put(old.toJson());
            }
        }
        writePresets(next);
        showSnackbar("已删除预设：" + name);
    }

    private org.json.JSONArray readPresets() {
        final String raw = getSharedPreferences(PRESET_PREFS, MODE_PRIVATE)
                .getString(PRESET_KEY, "[]");
        try {
            return new org.json.JSONArray(raw);
        } catch (Throwable ignored) {
            return new org.json.JSONArray();
        }
    }

    private void writePresets(org.json.JSONArray presets) {
        getSharedPreferences(PRESET_PREFS, MODE_PRIVATE).edit()
                .putString(PRESET_KEY, presets.toString()).apply();
    }

    private void showPaperPicker() {
        if (printIds.isEmpty()) {
            showSnackbar("没有可用的相纸");
            return;
        }
        final int current = printIds.indexOf(live().printStock);
        showChoiceSheet("选择相纸 / 拷贝片", "当前 Look 会沿这张相纸显影",
                printLabels, current, picked -> {
                    if (picked < 0 || picked >= printIds.size()
                            || live().printStock.equals(printIds.get(picked))) {
                        return;
                    }
                    live().printStock = printIds.get(picked);
                    refreshPaperLabel();
                    showingOriginal = false;
                    onStockChanged();
                });
    }

    /** The 22-parameter chip strip. */
        /** The single active-parameter slider and its touch policy. */
    private LinearLayout buildOpticsPanel() {
        opticsPanelView = new OpticsPanel(this, type, theme,
                tfDisplay, tfBody, tfHan, MIN_TOUCH_DP, largeText(),
                opticsHost(), opticsButtons());
        final LinearLayout panel = opticsPanelView.view();
        opticsPanel = panel;
        bindOpticsPanel();
        bindActiveOptics();
        syncOpticsChoices();
        return panel;
    }

    /** The panel's callbacks into the Activity's Look and navigation. */
    private OpticsPanel.Host opticsHost() {
        return new OpticsPanel.Host() {
            @Override public OpticsDelta.Param activeParam() {
                return activeOptics();
            }
            @Override public boolean unlocked() {
                return opticsUnlocked;
            }
            @Override public void onLocked() {
                showSnackbar(opticsLockReason());
            }
            @Override public void onSelect(int index) {
                selectOptics(index);
            }
            @Override public void onReset() {
                resetActiveOptics();
            }
            @Override public void onView() {
                showViewAdjustSheet();
            }
            @Override public void onMeter() {
                showMeterPicker();
            }
            @Override public void onScanner() {
                showScanSheet();
            }
            @Override public void onSliderValue(float value) {
                onOpticsSliderValue(value);
            }
            @Override public void onSliderStart() {
                trackingOpticsTouch = true;
                if (opticsSlider != null) {
                    opticsSlider.removeCallbacks(opticsCommitRunnable);
                }
            }
            @Override public void onSliderStop() {
                trackingOpticsTouch = false;
                if (opticsSlider != null) {
                    opticsSlider.removeCallbacks(opticsCommitRunnable);
                }
                commitOptics();
            }
            @Override public void onCameraFilter() {
                showCameraFilterPicker();
            }
            @Override public void onProtectionNorm() {
                showProtectionNormPicker();
            }
        };
    }

    /** Keep the Activity's aliases on the panel's children. */
    private void bindOpticsPanel() {
        opticsChips.clear();
        opticsChips.addAll(opticsPanelView.chips());
        opticsStripScroll = opticsPanelView.stripScroll();
        opticsLockNote = opticsPanelView.lockNote();
        opticsName = opticsPanelView.name();
        opticsReset = opticsPanelView.reset();
        opticsValue = opticsPanelView.value();
        opticsUnit = opticsPanelView.unit();
        opticsDelta = opticsPanelView.delta();
        opticsRange = opticsPanelView.range();
        opticsSlider = opticsPanelView.slider();
        opticsFilterButton = opticsPanelView.filterButton();
        opticsNormButton = opticsPanelView.normButton();
    }

    /** Build the Activity's M3 buttons without changing their style. */
    private OpticsPanel.Buttons opticsButtons() {
        return new OpticsPanel.Buttons() {
            @Override public MaterialButton action(CharSequence text,
                    String description, Runnable onClick) {
                final MaterialButton button = actionButton(text, description);
                button.setOnClickListener(v -> onClick.run());
                return button;
            }
            @Override public MaterialButton text(CharSequence text,
                    String description, int color, Runnable onClick) {
                return m3TextButton(text, description, color, onClick);
            }
        };
    }

    /**
     * The view-adjust layer as an M3 sheet: four category chips, then one
     * labelled slider per field. Values live in {@code optics.view}, so the
     * Frame switch and undo see them with the rest of the Look.
     */
    /** The hue toggle names the state, not the action. */
    private String hueToggleText() {
        if (HueCorrection.fixed(live().filmStock)) {
            return "胶片原色 · 固定";
        }
        return live().hueCorrection > 0.5f ? "胶片原色 · 开" : "胶片原色 · 关";
    }

    private void showViewAdjustSheet() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));

        root.addView(type.m3Label("视图调整 / VIEW", M3Tokens.TypeRole.TITLE_MEDIUM,
                theme.role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK),
                tfDisplay));

        final MaterialButton hueButton = actionButton(
                type.mixedText(hueToggleText()), "胶片原色：关闭或启用该胶片的色相校正");
        hueButton.setOnClickListener(v -> {
            if (HueCorrection.fixed(live().filmStock)) {
                showSnackbar("这只胶片的色相校正由配置文件固定，不能关闭");
                return;
            }
            if (currentSession != null) {
                currentSession.edit(look -> look.hueCorrection =
                        look.hueCorrection > 0.5f ? 0f : 1f);
                optics = currentSession.look();
            } else {
                optics.hueCorrection = optics.hueCorrection > 0.5f ? 0f : 1f;
            }
            hueButton.setText(type.mixedText(hueToggleText()));
            renderImage();
        });
        root.addView(hueButton, new LinearLayout.LayoutParams(-1, dp(48)));
        final MaterialButton gamutButton = actionButton(
                type.mixedText("预览色域 · " + previewSpace),
                "预览色域：只影响屏幕，导出始终使用 sRGB");
        gamutButton.setOnClickListener(v -> showChoiceSheet("预览色域 / GAMUT",
                "只影响屏幕显示；导出的 JPEG/TIFF 始终使用 sRGB",
                java.util.Arrays.asList("sRGB", "Display P3"),
                "Display P3".equals(previewSpace) ? 1 : 0, picked -> {
                    previewSpace = picked == 1 ? "Display P3" : "sRGB";
                    getSharedPreferences("console", MODE_PRIVATE).edit()
                            .putString("preview_space", previewSpace).apply();
                    gamutButton.setText(type.mixedText("预览色域 · " + previewSpace));
                    renderImage();
                }));
        root.addView(gamutButton, new LinearLayout.LayoutParams(-1, dp(48)));

        final ChipGroup groups = new ChipGroup(this);
        groups.setSingleSelection(true);
        groups.setSelectionRequired(true);
        final LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);

        for (ViewAdjust.Group group : ViewAdjust.Group.values()) {
            final Chip chip = new Chip(this);
            OpticsPanel.styleChip(this, theme, chip);
            chip.setText(type.mixedText(group.label + " / " + group.english));
            chip.setCheckable(true);
            chip.setChecked(group == ViewAdjust.Group.WHITE_BALANCE);
            chip.setMinimumHeight(dp(MIN_TOUCH_DP));
            chip.setOnClickListener(v -> populateViewFields(fields, group));
            groups.addView(chip);
        }
        root.addView(groups, new LinearLayout.LayoutParams(-1, -2));

        final ScrollView scroll = new ScrollView(this);
        scroll.addView(fields, new android.widget.FrameLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        populateViewFields(fields, ViewAdjust.Group.WHITE_BALANCE);
        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> viewAdjustHandler.removeCallbacks(viewAdjustCommit));
        dialog.show();
    }

    /**
     * The scan page: one model preset plus the two correction switches the
     * engine already exposes. The preset is only a named combination of those
     * switches, so the chip and the switches always agree.
     */
    private void showScanSheet() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.addView(type.m3Label("扫描 / SCAN", M3Tokens.TypeRole.TITLE_MEDIUM,
                theme.onSurface(), tfDisplay));
        root.addView(type.m3Label("SP-3000 是模型预设，不是实测扫描仪标定。"
                        + "选择预设只打开两项黑白校正；关闭后仍可单独开关。"
                        + "镜头模糊与反锐化属于原生 parity 任务。",
                        M3Tokens.TypeRole.LABEL_MEDIUM, theme.onSurfaceVariant(), tfHan),
                new LinearLayout.LayoutParams(-1, -2));
        if (isPositiveFilm(live().filmStock)) {
            root.addView(type.m3Label("当前是正片直扫：黑白校正不参与渲染；切回负片后生效。",
                            M3Tokens.TypeRole.LABEL_MEDIUM, theme.primary(), tfHan),
                    new LinearLayout.LayoutParams(-1, -2));
        }

        final ChipGroup profiles = new ChipGroup(this);
        profiles.setSingleSelection(true);
        profiles.setSelectionRequired(true);
        final java.util.List<Chip> chips = new ArrayList<>();
        final Runnable[] sync = new Runnable[1];
        final ScanProfile[] values = ScanProfile.values();
        for (ScanProfile profile : values) {
            final Chip chip = new Chip(this);
            OpticsPanel.styleChip(this, theme, chip);
            chip.setText(type.mixedText(profile.label));
            chip.setCheckable(true);
            chip.setChecked(profile == live().scanProfile());
            chip.setMinimumHeight(dp(MIN_TOUCH_DP));
            chip.setOnClickListener(v -> {
                applyScanPreset(profile);
                sync[0].run();
            });
            chips.add(chip);
            profiles.addView(chip);
        }
        root.addView(profiles, new LinearLayout.LayoutParams(-1, -2));

        final MaterialSwitch white = new MaterialSwitch(this);
        final MaterialSwitch black = new MaterialSwitch(this);
        root.addView(switchRow("白点校正", null, white,
                        () -> scanCorrectionOn(OpticsDelta.Param.SCANNER_WHITE),
                        on -> {
                            setScanCorrection(OpticsDelta.Param.SCANNER_WHITE, on);
                            sync[0].run();
                        }),
                new LinearLayout.LayoutParams(-1, dp(56)));
        root.addView(switchRow("黑点校正", null, black,
                        () -> scanCorrectionOn(OpticsDelta.Param.SCANNER_BLACK),
                        on -> {
                            setScanCorrection(OpticsDelta.Param.SCANNER_BLACK, on);
                            sync[0].run();
                        }),
                new LinearLayout.LayoutParams(-1, dp(56)));
        sync[0] = () -> syncScanSheet(chips, white, black);
        sync[0].run();
        // At 2.0× the two switch rows overflow a phone sheet; keep the preset
        // and both corrections reachable instead of clipping the black point.
        final NestedScrollView scroll = new NestedScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(root, new android.widget.FrameLayout.LayoutParams(-1, -2));
        dialog.setContentView(scroll);
        dialog.show();
    }

    private void syncScanSheet(java.util.List<Chip> chips, MaterialSwitch white,
                               MaterialSwitch black) {
        final ScanProfile current = live().scanProfile();
        final ScanProfile[] values = ScanProfile.values();
        for (int i = 0; i < values.length; ++i) {
            chips.get(i).setChecked(values[i] == current);
        }
        syncingToggles = true;
        try {
            final boolean whiteOn = scanCorrectionOn(OpticsDelta.Param.SCANNER_WHITE);
            white.setChecked(whiteOn);
            white.setContentDescription(toggleDescription("白点校正", whiteOn));
            final boolean blackOn = scanCorrectionOn(OpticsDelta.Param.SCANNER_BLACK);
            black.setChecked(blackOn);
            black.setContentDescription(toggleDescription("黑点校正", blackOn));
        } finally {
            syncingToggles = false;
        }
    }

    private boolean scanCorrectionOn(OpticsDelta.Param param) {
        return live().get(param) > 0.5f;
    }

    private void applyScanPreset(ScanProfile profile) {
        if (currentSession != null) {
            currentSession.edit(look -> look.applyScanProfile(profile));
            optics = currentSession.look();
        } else {
            optics.applyScanProfile(profile);
        }
        updateUndoAction();
        refreshReadoutStrip();
        renderImage();
    }

    private void setScanCorrection(OpticsDelta.Param param, boolean on) {
        final float value = on ? 1f : 0f;
        if (currentSession != null) {
            currentSession.edit(look -> look.set(param, value));
            optics = currentSession.look();
        } else {
            optics.set(param, value);
        }
        updateUndoAction();
        refreshReadoutStrip();
        renderImage();
    }

    /**
     * Composition as an M3 sheet. The values are part of the Look and go
     * through the engine's geometry node; no bitmap transform is baked in.
     */
    /** The quarter-turn chip row. */
    private void buildComposeTurns(LinearLayout root, int[] draftTurns) {
        final ChipGroup turns = new ChipGroup(this);
        turns.setSingleSelection(true);
        turns.setSelectionRequired(true);
        final String[] labels = {"0°", "90°", "180°", "270°"};
        for (int i = 0; i < labels.length; ++i) {
            final int quarter = i;
            final Chip chip = new Chip(this);
            OpticsPanel.styleChip(this, theme, chip);
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setChecked(draftTurns[0] == quarter);
            chip.setMinimumHeight(dp(MIN_TOUCH_DP));
            chip.setOnClickListener(v -> draftTurns[0] = quarter);
            turns.addView(chip);
        }
        root.addView(turns, new LinearLayout.LayoutParams(-1, -2));

    }

    /** The rotation and crop sliders. */
    private void buildComposeSliders(LinearLayout root, float[] draft) {
        composeSlider(root, "旋转 / ROTATION", -45f, 45f, draft[0],
                v -> draft[0] = v);
        composeSlider(root, "裁切 X / CROP X", 0f, 1f, draft[1],
                v -> draft[1] = v);
        composeSlider(root, "裁切 Y / CROP Y", 0f, 1f, draft[2],
                v -> draft[2] = v);
        composeSlider(root, "裁切宽 / CROP W", 0.05f, 1f, draft[3],
                v -> draft[3] = v);
        composeSlider(root, "裁切高 / CROP H", 0.05f, 1f, draft[4],
                v -> draft[4] = v);

    }

    /** The two flip switches. */
    private void buildComposeFlips(LinearLayout root, boolean[] draftFlips) {
        final MaterialSwitch flipH = new MaterialSwitch(this);
        flipH.setText(type.mixedText("水平翻转 / FLIP H"));
        flipH.setChecked(draftFlips[0]);
        flipH.setOnCheckedChangeListener((b, checked) -> draftFlips[0] = checked);
        root.addView(flipH, new LinearLayout.LayoutParams(-1, dp(48)));

        final MaterialSwitch flipV = new MaterialSwitch(this);
        flipV.setText(type.mixedText("垂直翻转 / FLIP V"));
        flipV.setChecked(draftFlips[1]);
        flipV.setOnCheckedChangeListener((b, checked) -> draftFlips[1] = checked);
        root.addView(flipV, new LinearLayout.LayoutParams(-1, dp(48)));

    }

    /** The apply and reset buttons. */
    private void buildComposeActions(LinearLayout root, BottomSheetDialog dialog,
                                             int[] draftTurns, float[] draft, boolean[] draftFlips) {
        final MaterialButton apply = actionButton(
                type.mixedText("应用构图"), "应用旋转、裁切和翻转");
        apply.setOnClickListener(v -> {
            live().geometryTurns = draftTurns[0];
            live().geometryRotation = draft[0];
            live().geometryCropX = draft[1];
            live().geometryCropY = draft[2];
            live().geometryCropW = draft[3];
            live().geometryCropH = draft[4];
            live().geometryFlipH = draftFlips[0];
            live().geometryFlipV = draftFlips[1];
            dialog.dismiss();
            renderImage();
        });
        root.addView(apply, new LinearLayout.LayoutParams(-2, dp(48)));

        final MaterialButton reset = actionButton(type.mixedText("重置构图"), "重置旋转、裁切和翻转");
        reset.setOnClickListener(v -> {
            live().geometryTurns = 0;
            live().geometryRotation = 0f;
            live().geometryCropX = live().geometryCropY = 0f;
            live().geometryCropW = live().geometryCropH = 1f;
            live().geometryFlipH = live().geometryFlipV = false;
            dialog.dismiss();
            renderImage();
        });
        root.addView(reset, new LinearLayout.LayoutParams(-2, dp(48)));

    }

    private void showComposeSheet() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.addView(type.m3Label("构图 / COMPOSE", M3Tokens.TypeRole.TITLE_MEDIUM,
                theme.onSurface(), tfDisplay));

        final int[] draftTurns = {live().geometryTurns};
        final float[] draft = {live().geometryRotation, live().geometryCropX, live().geometryCropY,
                live().geometryCropW, live().geometryCropH};
        final boolean[] draftFlips = {live().geometryFlipH, live().geometryFlipV};

        buildComposeTurns(root, draftTurns);
        buildComposeSliders(root, draft);
        buildComposeFlips(root, draftFlips);
        buildComposeActions(root, dialog, draftTurns, draft, draftFlips);
        dialog.setContentView(scrollableSheet(root));
        dialog.show();
    }

    private void composeSlider(LinearLayout parent, String label, float min, float max,
                               float value, java.util.function.Consumer<Float> apply) {
        final LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.addView(type.m3Label(label, M3Tokens.TypeRole.LABEL_LARGE, theme.onSurface(), tfHan),
                new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView readout = type.m3Label(String.format(java.util.Locale.US, "%.2f", value),
                M3Tokens.TypeRole.BODY_MEDIUM, theme.primary(), tfBody);
        head.addView(readout);
        parent.addView(head, new LinearLayout.LayoutParams(-1, -2));

        final Slider slider = new Slider(this);
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setStepSize(0.01f);
        slider.setValue(Math.max(min, Math.min(max, value)));
        slider.setLabelBehavior(com.google.android.material.slider.LabelFormatter.LABEL_GONE);
        slider.setContentDescription(label);
        ViewCompat.setStateDescription(slider,
                String.format(java.util.Locale.US, "%.2f", value));
        slider.addOnChangeListener((s, v, fromUser) -> {
            apply.accept(v);
            readout.setText(String.format(java.util.Locale.US, "%.2f", v));
            ViewCompat.setStateDescription(s, String.format(java.util.Locale.US, "%.2f", v));
        });
        parent.addView(slider, new LinearLayout.LayoutParams(-1, dp(SLIDER_HEIGHT_DP)));
    }

    private void populateViewFields(LinearLayout fields, ViewAdjust.Group group) {
        fields.removeAllViews();
        for (ViewAdjust.Field field : ViewAdjust.Field.values()) {
            if (field.group != group) {
                continue;
            }
            final LinearLayout block = new LinearLayout(this);
            block.setOrientation(LinearLayout.VERTICAL);
            block.setPadding(0, dp(10), 0, dp(4));

            final LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(android.view.Gravity.CENTER_VERTICAL);
            head.addView(type.m3Label(field.label, M3Tokens.TypeRole.LABEL_LARGE,
                    theme.role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK),
                    tfHan), new LinearLayout.LayoutParams(0, -2, 1f));
            final TextView value = type.m3Label(
                    ViewAdjust.valueText(field.get(optics.view), field.unit),
                    M3Tokens.TypeRole.BODY_MEDIUM,
                    theme.role(com.google.android.material.R.attr.colorPrimary, Palette.Role.ACCENT),
                    tfBody);
            head.addView(value, new LinearLayout.LayoutParams(-2, -2));
            block.addView(head, new LinearLayout.LayoutParams(-1, -2));

            final Slider slider = new Slider(this);
            slider.setValueFrom(field.min);
            slider.setValueTo(field.max);
            slider.setStepSize(field.max - field.min > 50f ? 1f : 0.01f);
            slider.setValue(Math.max(field.min, Math.min(field.max, field.get(optics.view))));
            slider.setLabelBehavior(
                    com.google.android.material.slider.LabelFormatter.LABEL_GONE);
            slider.setContentDescription(field.label + "，视图调整");
            ViewCompat.setStateDescription(slider,
                    ViewAdjust.valueText(field.get(optics.view), field.unit));
            slider.addOnChangeListener((s, v, fromUser) -> {
                field.set(optics.view, v);
                value.setText(ViewAdjust.valueText(v, field.unit));
                ViewCompat.setStateDescription(s,
                        ViewAdjust.valueText(v, field.unit));
                if (!fromUser) {
                    return;
                }
                requestLivePreview();
                viewAdjustHandler.removeCallbacks(viewAdjustCommit);
                viewAdjustHandler.postDelayed(viewAdjustCommit, 180);
            });
            slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override public void onStartTrackingTouch(Slider s) {
                    viewAdjustHandler.removeCallbacks(viewAdjustCommit);
                }
                @Override public void onStopTrackingTouch(Slider s) {
                    viewAdjustHandler.removeCallbacks(viewAdjustCommit);
                    commitViewAdjust();
                }
            });
            block.addView(slider, new LinearLayout.LayoutParams(-1, dp(SLIDER_HEIGHT_DP)));
            fields.addView(block, new LinearLayout.LayoutParams(-1, -2));
        }

        final MaterialButton reset = actionButton(type.mixedText("重置本页"), "重置当前视图调整分类");
        reset.setOnClickListener(v -> {
            optics.view.reset(group);
            populateViewFields(fields, group);
            recordOpticsEdit();
            renderImage();
        });
        fields.addView(reset, new LinearLayout.LayoutParams(-2, dp(48)));
    }

    /** Set a chip's M3 filter style from the current theme's roles. */
        /**
     * The active parameter's readout, above the slider: name and reset on one
     * line, then value, unit, display-only Δ, and the contract's visible range.
     */
        /** A label + current choice + arrow, the same shape as the paper picker. */
        private OpticsDelta.Param activeOptics() {
        return OpticsDelta.paramAt(activeOptics);
    }

    /** Bind the one slider and the readout to the strip's active parameter. */
    private void bindActiveOptics() {
        if (opticsSlider == null) {
            return;
        }
        final OpticsDelta.Param spec = activeOptics();
        final float stored = optics.get(spec);
        syncingOptics = true;
        try {
            // Every contract range contains 1.0, so parking the thumb there is
            // always inside the previous range. The step is cleared, the upper
            // bound widened, and only then are the real bounds stated: this
            // survives every transition, including one into a range whose
            // lower bound equals the previous upper bound (switch -> filter
            // strength). The real value is restored below.
            opticsSlider.setValue(1.0f);
            opticsSlider.setStepSize(0f);
            opticsSlider.setValueTo(200.0f);
            opticsSlider.setValueFrom(spec.min);
            opticsSlider.setValueTo(spec.max);
            opticsSlider.setStepSize(spec.step);
            opticsSlider.setValue(spec.snap(stored));
        } finally {
            syncingOptics = false;
        }
        showActiveOptics(stored);
        refreshOpticsChips();
    }

    /** One slider change: update the Look and its head; commit on release. */
    private void onOpticsSliderValue(float value) {
        if (syncingOptics) {
            return;
        }
        final OpticsDelta.Param spec = activeOptics();
        optics.set(spec, value);
        showActiveOptics(value);
        refreshReadoutStrip();
        // One render per completed gesture: a touch release commits at once;
        // keyboard, switch access and TalkBack changes coalesce.
        if (trackingOpticsTouch) {
            requestLivePreview();
        } else {
            scheduleOpticsCommit(opticsSlider);
        }
    }

    /** Show a value in the head without moving the slider or committing. */
    private void showActiveOptics(float value) {
        final OpticsDelta.Param spec = activeOptics();
        final String number = OpticsDelta.valueText(value);
        final String delta = OpticsDelta.text(value, spec.defaultValue);
        final boolean moved = !spec.isDefault(value);
        final int color = moved ? theme.primary() : theme.onSurfaceVariant();
        opticsName.setText(spec.label);
        opticsValue.setText(number);
        opticsUnit.setText(spec.unit);
        opticsDelta.setText("Δ " + delta);
        opticsDelta.setTextColor(color);
        opticsReset.setTextColor(color);
        opticsRange.setText("范围 " + OpticsDelta.valueText(spec.min) + " – "
                + OpticsDelta.valueText(spec.max) + " " + spec.unit
                + " · 步进 " + stepText(spec.step)
                + outsideRangeNote(spec, value));
        opticsSlider.setContentDescription(spec.label);
        // The slider's own RangeInfo carries the real float range; the state
        // description carries the value the Look actually holds, which may be
        // outside the M3 slider's cap on an older Look.
        ViewCompat.setStateDescription(opticsSlider,
                number + " " + spec.unit + "，Δ " + delta + outsideRangeNote(spec, value));
        opticsReset.setContentDescription("重置" + spec.label + "为 "
                + OpticsDelta.valueText(spec.defaultValue) + " " + spec.unit);
    }

    private static String outsideRangeNote(OpticsDelta.Param spec, float value) {
        if (value > spec.max + 0.0005f) {
            return "，高于滑杆上限 " + OpticsDelta.valueText(spec.max) + " " + spec.unit;
        }
        if (value < spec.min - 0.0005f) {
            return "，低于滑杆下限 " + OpticsDelta.valueText(spec.min) + " " + spec.unit;
        }
        return "";
    }

    private static String stepText(float step) {
        return Math.abs(step - Math.round(step)) < 0.001f
                ? String.format(java.util.Locale.US, "%.0f", step)
                : String.format(java.util.Locale.US, "%.2f", step);
    }

    /** Select a strip chip: one UI selection, no Look change and no render. */
    private void selectOptics(int index) {
        final OpticsDelta.Param spec = OpticsDelta.paramAt(index);
        if (activeOptics == spec.ordinal() && opticsSlider != null) {
            // A checkable chip toggles itself on click; the strip has exactly
            // one selection, so put the check back where it belongs.
            refreshOpticsChips();
            return;
        }
        activeOptics = spec.ordinal();
        bindActiveOptics();
    }

    private void refreshOpticsChips() {
        for (int i = 0; i < opticsChips.size(); ++i) {
            final Chip chip = opticsChips.get(i);
            final boolean active = i == activeOptics;
            chip.setChecked(active);
            chip.setContentDescription(OpticsDelta.paramAt(i).label + "，参数 "
                    + (i + 1) + " / " + opticsChips.size()
                    + (active ? "，已选中" : "，未选中"));
        }
        if (opticsStripScroll != null && activeOptics >= 0
                && activeOptics < opticsChips.size()) {
            final Chip active = opticsChips.get(activeOptics);
            opticsStripScroll.post(() -> opticsStripScroll.smoothScrollTo(
                    Math.max(0, active.getLeft() - dp(24)), 0));
        }
    }

    /** One render after a settled keyboard/TalkBack change. */
    private void scheduleOpticsCommit(Slider slider) {
        slider.removeCallbacks(opticsCommitRunnable);
        slider.postDelayed(opticsCommitRunnable, OPTICS_COMMIT_DELAY_MS);
    }

    /** Reset only the active parameter; already at default is a no-op. */
    private void resetActiveOptics() {
        if (!opticsUnlocked) {
            showSnackbar(opticsLockReason());
            return;
        }
        final OpticsDelta.Param spec = activeOptics();
        if (spec.isDefault(optics.get(spec))) {
            return;
        }
        if (currentSession != null) {
            currentSession.edit(look -> look.reset(spec));
            optics = currentSession.look();
        } else {
            optics.reset(spec);
        }
        bindActiveOptics();
        refreshReadoutStrip();
        commitOptics();
    }

    private void showCameraFilterPicker() {
        if (!opticsUnlocked) {
            showSnackbar(opticsLockReason());
            return;
        }
        final OpticsDelta.CameraFilter[] all = OpticsDelta.CameraFilter.values();
        final List<String> labels = new ArrayList<>();
        for (OpticsDelta.CameraFilter filter : all) {
            labels.add(filter.label);
        }
        final int selected = OpticsDelta.CameraFilter.fromWire(optics.cameraFilter).ordinal();
        showChoiceSheet("镜头滤镜 / CAMERA FILTER",
                "Wratten 滤镜在镜头前；放大机的滤色片属于相纸阶段",
                labels, selected, picked -> {
                    if (picked < 0 || picked >= all.length) {
                        return;
                    }
                    final OpticsDelta.CameraFilter chosen = all[picked];
                    if (chosen.wire.equals(optics.cameraFilter)) {
                        return;
                    }
                    if (currentSession != null) {
                        currentSession.edit(look -> look.cameraFilter = chosen.wire);
                        optics = currentSession.look();
                    } else {
                        optics.cameraFilter = chosen.wire;
                    }
                    syncOpticsChoices();
                    commitOptics();
                });
    }

    /** Metering is a Look value: applying it re-states the same JSON as a render. */
    private void showMeterPicker() {
        final java.util.List<String> labels = java.util.Arrays.asList(Metering.NAMES);
        final int selected = Metering.indexOf(live().meterMethod);
        showChoiceSheet("测光 / METER",
                "关闭时由胶片自己的曝光决定；JPEG 默认关闭，RAW 默认保护高光",
                labels, selected, picked -> {
                    if (picked < 0 || picked >= Metering.IDS.length) {
                        return;
                    }
                    if (currentSession != null) {
                        currentSession.edit(look -> look.meterMethod = Metering.IDS[picked]);
                        optics = currentSession.look();
                    } else {
                        optics.meterMethod = Metering.IDS[picked];
                    }
                    renderImage();
                });
    }

    private void showProtectionNormPicker() {
        if (!opticsUnlocked) {
            showSnackbar(opticsLockReason());
            return;
        }
        final OpticsDelta.ProtectionNorm[] all = OpticsDelta.ProtectionNorm.values();
        final List<String> labels = new ArrayList<>();
        for (OpticsDelta.ProtectionNorm norm : all) {
            labels.add(norm.label);
        }
        final int selected = OpticsDelta.ProtectionNorm.fromWire(optics.sceneNorm).ordinal();
        showChoiceSheet("保护基准 / PROTECTION NORM",
                "高光与暗部保护按这个场景测量计算，不改变保护量",
                labels, selected, picked -> {
                    if (picked < 0 || picked >= all.length) {
                        return;
                    }
                    final OpticsDelta.ProtectionNorm chosen = all[picked];
                    if (chosen.wire.equals(optics.sceneNorm)) {
                        return;
                    }
                    if (currentSession != null) {
                        currentSession.edit(look -> look.sceneNorm = chosen.wire);
                        optics = currentSession.look();
                    } else {
                        optics.sceneNorm = chosen.wire;
                    }
                    syncOpticsChoices();
                    commitOptics();
                });
    }

    private void syncOpticsChoices() {
        if (opticsFilterButton != null) {
            final String label = OpticsDelta.CameraFilter.fromWire(optics.cameraFilter).label;
            opticsFilterButton.setText(label + "  ↓");
            opticsFilterButton.setContentDescription(
                    "镜头滤镜，当前 " + label + "，双击选择");
        }
        if (opticsNormButton != null) {
            final String label = OpticsDelta.ProtectionNorm.fromWire(optics.sceneNorm).label;
            opticsNormButton.setText(label + "  ↓");
            opticsNormButton.setContentDescription(
                    "保护基准，当前 " + label + "，双击选择");
        }
    }

    /**
     * The Optics head: title and the locked reason. The reset lives on the
     * active parameter's row, so it can only touch the one parameter in view.
     */
        /** The one sentence a locked Optics panel shows and repeats on tap. */
    private String opticsLockReason() {
        return session == null ? "先导入一张照片" : "先选择胶片或相纸，完成一次渲染";
    }

        private LinearLayout buildFormatPanel() {
        final LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.addView(sectionHead("PHYSICAL SCALE / 光学尺度", this::resetFormatPanel),
                new LinearLayout.LayoutParams(-1, -2));
        scalePanel = new ScalePanel(this, type, theme, tfDisplay, tfBody, tfHan,
                new ScalePanel.Host() {
                    @Override public GateScale.Gate activeGate() {
                        return GateScale.byId(live().overscanFormat);
                    }
                    @Override public void onPick(String gateId) {
                        pickScale(gateId);
                    }
                });
        panel.addView(scalePanel.view(), new LinearLayout.LayoutParams(
                -1, scalePanel.stripHeight()));

        final MaterialButton compose = actionButton(
                type.mixedText("构图 / COMPOSE"), "打开构图：旋转、翻转、裁切");
        compose.setOnClickListener(v -> showComposeSheet());
        panel.addView(compose, new LinearLayout.LayoutParams(-2, dp(48)));

        final LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        foot.setGravity(android.view.Gravity.CENTER_VERTICAL);
        foot.addView(type.m3Label("片门与尺度联动 · 不裁切构图",
                M3Tokens.TypeRole.LABEL_LARGE, theme.onSurfaceVariant(), tfHan),
                new LinearLayout.LayoutParams(0, -2, 1f));
        formatFootValue = type.m3Label(
                mmText(GateScale.resolve(live().overscanFormat, live().filmFormatMm).longEdgeMm) + " mm",
                M3Tokens.TypeRole.LABEL_LARGE, theme.primary(), tfBody);
        foot.addView(formatFootValue);
        panel.addView(foot, new LinearLayout.LayoutParams(-1, dp(28)));
        panel.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        rebuildFormatCards();
        return panel;
    }

    private void rebuildFormatCards() {
        if (scalePanel == null) {
            return;
        }
        scalePanel.rebuild(edgeIds);
    }

    private float fontScale() {
        return Math.max(1f, getResources().getConfiguration().fontScale);
    }

    /** A Scale card and its Gate are one choice, so one tap moves both. */
    private void pickScale(String gateId) {
        final GateScale.Gate gate = GateScale.byId(gateId);
        if (gate == null) {
            return;
        }
        if (gate.id.equals(live().overscanFormat)
                && Math.abs(gate.longEdgeMm - live().filmFormatMm) < 0.01f) {
            return;
        }
        live().overscanFormat = gate.id;
        live().filmFormatMm = gate.longEdgeMm;
        updateEdgeValue();
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(gate.longEdgeMm) + " mm");
        }
        rebuildFormatCards();
        if (scalePanel != null) {
            scalePanel.revealSelected();
        }
        if (edgePagesPanel != null) {
            edgePagesPanel.syncRebate();
        }
        onEdgeChanged();
    }

    private LinearLayout buildEdgePanel() {
        rebateRefreshers.clear();
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        final View home = buildEdgeHome();
        edgePagesPanel = new EdgePagesPanel(this, type, theme, tfBody, tfHan,
                new EdgePagesPanel.Host() {
                    @Override public EdgeLookAccess look() {
                        return new EdgeLookAccess(MainActivity.this.live());
                    }
                    @Override public void onBack() {
                        openEdgePage(-1);
                    }
                    @Override public String rebateGateLabel() {
                        if (edgeIds.isEmpty()) {
                            return "无可用片门";
                        }
                        return GateScale.resolve(live().overscanFormat,
                                live().filmFormatMm).label;
                    }
                    @Override public void showEdgePicker() {
                        MainActivity.this.showEdgePicker();
                    }
                    @Override public boolean canHostRebate() {
                        return GateScale.canHostRebate(sourceWidth, sourceHeight);
                    }
                    @Override public String effectiveDateText() {
                        return MainActivity.this.effectiveDateText();
                    }
                    @Override public String imprintDate() {
                        return MainActivity.this.imprintDate();
                    }
                    @Override public void askText(String title, String initial,
                                                   java.util.function.Consumer<String> onValue) {
                        MainActivity.this.askText(title, initial, onValue);
                    }
                    @Override public void askSeed(String title, int initial,
                                                   java.util.function.IntConsumer onValue) {
                        MainActivity.this.askSeed(title, initial, onValue);
                    }
                    @Override public void showChoiceSheet(String title, String note,
                                                           List<String> labels, int selectedIndex,
                                                           java.util.function.IntConsumer onPick) {
                        MainActivity.this.showChoiceSheet(title, note, labels,
                                selectedIndex, onPick);
                    }
                    @Override public void showSnackbar(String message) {
                        MainActivity.this.showSnackbar(message);
                    }
                    @Override public void requestLivePreview() {
                        MainActivity.this.requestLivePreview();
                    }
                    @Override public void commitRebate() {
                        MainActivity.this.commitRebate();
                    }
                    @Override public long commitDelayMs() {
                        return OPTICS_COMMIT_DELAY_MS;
                    }
                    @Override public boolean largeText() {
                        return MainActivity.this.largeText();
                    }
                }, edgeRows(), rebateRefreshers::add, this::refreshEdgeCards);
        root.addView(home, new LinearLayout.LayoutParams(-1, -2));
        root.addView(edgePagesPanel.rebatePage(), new LinearLayout.LayoutParams(-1, -2));
        root.addView(edgePagesPanel.datePage(), new LinearLayout.LayoutParams(-1, -2));
        openEdgePage(edgePageIndex);
        edgePagesPanel.syncRebate();
        edgePagesPanel.syncDate();
        return root;
    }

    /** The shared Edge switch row, borrowed from the Activity's own style. */
    private EdgePagesPanel.Rows edgeRows() {
        return new EdgePagesPanel.Rows() {
            @Override public View switchRow(String labelText, TextView detail,
                                            MaterialSwitch toggle,
                                            java.util.function.BooleanSupplier current,
                                            java.util.function.Consumer<Boolean> onChange) {
                return MainActivity.this.switchRow(labelText, detail, toggle, current, onChange);
            }
            @Override public String toggleDescription(String labelText, boolean on) {
                return MainActivity.toggleDescription(labelText, on);
            }
            @Override public void setSyncingToggles(boolean syncing) {
                syncingToggles = syncing;
            }
            @Override public MaterialButton textButton(CharSequence text, String description,
                                                       int color, Runnable action) {
                return m3TextButton(text, description, color, action);
            }
        };
    }

    /** The Edge landing page: one card per edge feature. */
    private LinearLayout buildEdgeHome() {
        edgeHomePanel = new EdgeHomePanel(this, type, theme, tfBody, tfHan,
                sectionHead("FRAME & IMPRINT / 边框", this::resetEdgePanel),
                new EdgeHomePanel.Host() {
                    @Override public String rebateSummary() {
                        return MainActivity.this.rebateSummary();
                    }
                    @Override public String dateSummary() {
                        return MainActivity.this.dateSummary();
                    }
                    @Override public void onOpenRebate() {
                        openEdgePage(0);
                    }
                    @Override public void onOpenDate() {
                        openEdgePage(1);
                    }
                }, rebateRefreshers::add);
        return (LinearLayout) edgeHomePanel.view();
    }

    /** -1 = the two cards, 0 = the Rebate page, 1 = the Date back page. */
    private void openEdgePage(int which) {
        edgePageIndex = which;
        if (edgeHomePanel != null) {
            edgeHomePanel.view().setVisibility(which < 0 ? View.VISIBLE : View.GONE);
        }
        if (edgePagesPanel != null) {
            edgePagesPanel.rebatePage().setVisibility(
                    which == 0 ? View.VISIBLE : View.GONE);
            edgePagesPanel.datePage().setVisibility(
                    which == 1 ? View.VISIBLE : View.GONE);
            if (which == 0) {
                edgePagesPanel.syncRebate();
            }
            if (which == 1) {
                edgePagesPanel.syncDate();
            }
        }
        refreshEdgeCards();
        // The panel is one ScrollView; a page opens at its own top.
        if (panels != null && activeTabIndex == 3 && panels[3] instanceof ScrollView) {
            ((ScrollView) panels[3]).scrollTo(0, 0);
        }
    }

    /** Repaint the entry cards and every chosen row from the live Look. */
    private void refreshEdgeCards() {
        for (int i = 0; i < rebateRefreshers.size(); ++i) {
            rebateRefreshers.get(i).run();
        }
    }

    private String rebateSummary() {
        if (!GateScale.canHostRebate(sourceWidth, sourceHeight)) {
            return "这张图没有可用片门";
        }
        if (!live().overscanOn) {
            return "未开启 · 点进去设置";
        }
        final String gate = edgeIds.isEmpty()
                ? gateLabel(live().overscanFormat)
                : GateScale.resolve(live().overscanFormat, live().filmFormatMm).label;
        return gate + " · 雾 " + fmt1(live().edgeFog) + " · 漏光 " + fmt1(live().edgeLeaks)
                + " · 张数 " + (live().frameNumber < 0.5f
                        ? "自动" : String.valueOf(Math.round(live().frameNumber)));
    }

    private String dateSummary() {
        if (!live().dateOn) {
            return "未开启 · 点进去设置";
        }
        return ControlChoice.labelOf(DateBack.PLACEMENTS, live().datePlacement)
                + ControlChoice.labelOf(DateBack.CORNERS, live().dateCorner)
                + " · " + ControlChoice.labelOf(DateBack.STYLES, live().dateStyle)
                + " · " + effectiveDateText();
    }

    private static String fmt1(float value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    /** The date text and the capture-data line are free text, not numbers. */
    private void askText(String title, String initial, java.util.function.Consumer<String> onValue) {
        final com.google.android.material.textfield.TextInputLayout field =
                new com.google.android.material.textfield.TextInputLayout(this);
        field.setBoxBackgroundMode(
                com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        final com.google.android.material.textfield.TextInputEditText input =
                new com.google.android.material.textfield.TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setText(initial == null ? "" : initial);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(3);
        field.addView(input, new com.google.android.material.textfield.TextInputLayout.LayoutParams(
                -1, -2));
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(field)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) ->
                        onValue.accept(input.getText().toString()))
                .show();
    }

    /** The seeds are arbitrary ints, so they ask for a number rather than a slider. */
    private void askSeed(String title, int initial, java.util.function.IntConsumer onValue) {
        final com.google.android.material.textfield.TextInputLayout field =
                new com.google.android.material.textfield.TextInputLayout(this);
        field.setBoxBackgroundMode(
                com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        final com.google.android.material.textfield.TextInputEditText input =
                new com.google.android.material.textfield.TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(initial));
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        field.addView(input, new com.google.android.material.textfield.TextInputLayout.LayoutParams(
                -1, -2));
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(field)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        onValue.accept(RebateControls.clampSeed(
                                Long.parseLong(input.getText().toString().trim())));
                    } catch (NumberFormatException ignored) {
                        showSnackbar("请输入 0 到 2147483647 之间的整数");
                    }
                })
                .show();
    }

    /** A switch change or a chosen row commits one render, not one per frame. */
    private void commitRebate() {
        onEdgeChanged();
    }

    private void showEdgePicker() {
        if (edgeIds.isEmpty()) {
            showSnackbar("这张图没有可用片门");
            return;
        }
        final int current = edgeIds.indexOf(live().overscanFormat);
        showChoiceSheet("选择片门格式", "只列出与这张图形状相符的片门",
                edgeLabels, current, picked -> {
                    if (picked < 0 || picked >= edgeIds.size()) {
                        return;
                    }
                    // The frame that goes in this gate, not whatever the Scale
                    // page happened to be left on.
                    selectGate(edgeIds.get(picked));
                    onEdgeChanged();
                });
    }

    /**
     * The M3 navigation rail used at ≥600 dp. It is the same four workspaces
     * as the phone tabs, with the same {@link #activeTabIndex}; {@link #setTab}
     * keeps both forms in sync. The real {@link NavigationRailView} supplies
     * the selected indicator, 48 dp-plus item targets, and the selected state
     * that accessibility services announce.
     */
    private NavigationRailView buildNavigationRail() {
        final NavigationRailView rail = new NavigationRailView(this);
        rail.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        rail.inflateMenu(R.menu.console_nav);
        for (int i = 0; i < TAB_NAMES.length; ++i) {
            final MenuItem item = rail.getMenu().findItem(TAB_ITEM_IDS[i]);
            if (item == null) {
                continue;
            }
            // A span keeps the Chinese label on OPPOSans through the rail's
            // active/inactive text-appearance changes.
            item.setTitle(type.typed(TAB_NAMES[i], tfHan));
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                item.setContentDescription(TAB_NAMES[i] + " " + TAB_ENGLISH[i]);
            }
        }
        rail.setOnItemSelectedListener(item -> {
            if (suppressWorkspaceSelection) {
                return true;
            }
            final int index = workspaceIndex(item.getItemId());
            if (index < 0) {
                return false;
            }
            if (index != activeTabIndex) {
                setTab(index);
                setPanelCollapsed(false);
            }
            return true;
        });
        return rail;
    }

    private static int workspaceIndex(int itemId) {
        for (int i = 0; i < TAB_ITEM_IDS.length; ++i) {
            if (TAB_ITEM_IDS[i] == itemId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The four phone workspaces use Material secondary tabs: one selected
     * indicator, 48 dp targets, and the selected state exposed by TabLayout
     * itself. The panel below is owned by {@link #setTab(int)}.
     */
    private View buildTabs() {
        tabLayout = new TabLayout(this);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setBackground(theme.glassSurface(GlassStyle.DOCK_RADIUS_DP));
        tabLayout.setClipToOutline(true);
        tabLayout.setElevation(dp(GlassStyle.SHEET_ELEVATION_DP));
        tabLayout.setSelectedTabIndicatorHeight(dp(2));
        tabLayout.setSelectedTabIndicatorColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorPrimary,
                palette.color(Palette.Role.ACCENT)));

        for (int i = 0; i < TAB_NAMES.length; ++i) {
            final TabLayout.Tab tab = tabLayout.newTab()
                    .setText(TAB_NAMES[i])
                    .setContentDescription(TAB_NAMES[i] + " " + TAB_ENGLISH[i]);
            tabLayout.addTab(tab, i, i == 0);
            tab.view.setMinimumHeight(dp(48));
            tab.view.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                    lastTabTouchAt = android.os.SystemClock.uptimeMillis();
                    return false;
                }
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                    v.performClick();
                    // The listener consumes UP, so the TabView never clears
                    // its own pressed state; clear the ripple here or the
                    // state layer stays under the selected tab.
                    v.setPressed(false);
                    v.jumpDrawablesToCurrentState();
                    return true;
                }
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.setPressed(false);
                    v.jumpDrawablesToCurrentState();
                }
                return false;
            });
            // The tab text appearance is the M3 title-small role; the family
            // stays OPPOSans for the Chinese labels.
            applyTypeface(tab.view, tfHan);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (suppressWorkspaceSelection) {
                    return;
                }
                final int index = tab.getPosition();
                if (index == activeTabIndex) {
                    return;
                }
                setTab(index);
                setPanelCollapsed(false);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }

            @Override public void onTabReselected(TabLayout.Tab tab) {
                if (suppressWorkspaceSelection) {
                    return;
                }
                // A real tap just set lastTabTouchAt; a reselect from a layout
                // pass must not expand the panel the user collapsed. Keyboard,
                // switch-access, and TalkBack activation carry focus instead.
                final boolean activated = tab.view.isPressed()
                        || tab.view.isFocused()
                        || tab.view.isAccessibilityFocused()
                        || android.os.SystemClock.uptimeMillis() - lastTabTouchAt <= 500L;
                if (!activated) {
                    return;
                }
                setPanelCollapsed(!panelCollapsed);
                showSnackbar(panelCollapsed ? "参数面板已收起，再点当前页签展开" : "参数面板已展开");
            }
        });
        return tabLayout;
    }

    private void setPanelCollapsed(boolean collapsed) {
        panelCollapsed = collapsed;
        if (panelHolder != null) {
            panelHolder.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        }
        if (controls != null) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) controls.getLayoutParams();
            if (wideLayout && ConsoleLayout.isTwoColumn(currentWidthDp())) {
                // The column keeps the window height; collapsing hides only the
                // panel, leaving the tabs where they are.
                lp.width = dp(ConsoleLayout.controlColumnWidthDp(currentWidthDp()));
                lp.height = -1;
            } else {
                lp.width = -1;
                // The rail replaces the bottom tabs in the single-column wide
                // shell, so the deck is only the panel plus its shell padding.
                final boolean medium = wideLayout && !ConsoleLayout.isTwoColumn(currentWidthDp());
                lp.height = dp(medium
                        ? CONTROLS_OPEN_DP - TABS_HEIGHT_DP
                        : (collapsed ? CONTROLS_CLOSED_DP : CONTROLS_OPEN_DP));
            }
            lp.weight = 0f;
            controls.setLayoutParams(lp);
        }
        if (panelHolder != null) {
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) panelHolder.getLayoutParams();
            final boolean twoColumn = ConsoleLayout.isTwoColumn(currentWidthDp());
            lp.height = twoColumn ? 0 : dp(PANEL_HEIGHT_DP);
            lp.weight = twoColumn ? 1f : 0f;
            panelHolder.setLayoutParams(lp);
        }
    }

    /**
     * The M3 sheet header: a heading, a real text-button close, and the sheet's
     * own surface. Every sheet in the Console starts here, so the title a
     * TalkBack user hears and the title a sighted user reads are one view.
     */
    private LinearLayout sheetColumn(final BottomSheetDialog dialog, String title) {
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        // The BottomSheetDialog's M3 container owns the surface and its rounded
        // top; a child background would square those corners off.
        column.setPadding(0, dp(4), 0, dp(8));

        final LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), 0, dp(8), 0);
        final TextView titleView = type.m3Label(String.valueOf(title),
                M3Tokens.TypeRole.TITLE_MEDIUM, theme.onSurface(), tfHan);
        ViewCompat.setAccessibilityHeading(titleView, true);
        head.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1f));

        final MaterialButton close = new MaterialButton(this);
        close.setText("关闭");
        close.setAllCaps(false);
        close.setMinWidth(0);
        close.setMinimumWidth(0);
        close.setInsetTop(0);
        close.setInsetBottom(0);
        close.setPadding(dp(12), 0, dp(12), 0);
        close.setContentDescription("关闭");
        type.applyTypeRole(close, M3Tokens.TypeRole.LABEL_LARGE, tfHan);
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close, new LinearLayout.LayoutParams(-2, dp(48)));
        column.addView(head, new LinearLayout.LayoutParams(-1, -2));
        return column;
    }

    /**
     * A modal M3 bottom sheet. Focus is trapped by the dialog window; the view
     * that opened the sheet is asked for focus again when it closes, so a
     * keyboard or switch-access user returns to the control they left.
     */
    private void showSheet(final BottomSheetDialog dialog, View content) {
        final View invoker = getCurrentFocus();
        dialog.setContentView(content);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(d -> {
            if (openSheet == d) {
                openSheet = null;
            }
            if (invoker != null && invoker.isAttachedToWindow()) {
                invoker.requestFocus();
            }
        });
        dialog.show();
        final BottomSheetBehavior<android.widget.FrameLayout> behavior = dialog.getBehavior();
        behavior.setFitToContents(true);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        openSheet = dialog;
    }

    private void dismissOpenSheet() {
        if (openSheet != null) {
            if (openSheet.isShowing()) {
                openSheet.dismiss();
            }
            openSheet = null;
        }
    }

    /** A sheet body tall enough for its rows, never taller than 62 % of the window. */
    private int sheetBodyHeight(int rows, int rowDp) {
        final int max = Math.min(dp(430),
                Math.round(getResources().getDisplayMetrics().heightPixels * 0.62f));
        final int wanted = Math.max(dp(rowDp), rows * dp(rowDp) + dp(12));
        return Math.min(max, wanted);
    }

    /** Import, export, and About use the same M3 sheet as the stock pickers. */
    void showActionSheet(String title, String note, String[] items,
                         java.util.function.IntConsumer onPick) {
        showActionSheet(title, note, items, onPick, null, null);
    }

    /** The same sheet, with a gesture on its title. */
    void showActionSheet(String title, String note, String[] items,
                         java.util.function.IntConsumer onPick, Runnable onTitleLongPress) {
        showActionSheet(title, note, items, onPick, onTitleLongPress, null);
    }

    /**
     * The same sheet with a line that never scrolls away: the update
     * announcement's download address stays above a long changelog.
     */
    void showActionSheet(String title, String note, String[] items,
                         java.util.function.IntConsumer onPick, Runnable onTitleLongPress,
                         String pinned) {
        if (items == null || items.length == 0 || isFinishing()) {
            return;
        }
        dismissOpenSheet();
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setTitle(title);
        final LinearLayout column = sheetColumn(dialog, title);

        if (pinned != null && !pinned.isEmpty()) {
            final TextView pinView = type.m3Label(pinned, M3Tokens.TypeRole.BODY_MEDIUM,
                    theme.onSurface(), tfHan);
            pinView.setPadding(dp(16), 0, dp(16), dp(8));
            column.addView(pinView, new LinearLayout.LayoutParams(-1, -2));
        }

        final NestedScrollView scroll = new NestedScrollView(this);
        scroll.setFillViewport(false);
        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        if (note != null && !note.isEmpty()) {
            final TextView noteView = type.m3Label(note, M3Tokens.TypeRole.BODY_MEDIUM,
                    theme.onSurfaceVariant(), tfHan);
            noteView.setLineSpacing(dp(3), 1f);
            noteView.setPadding(dp(16), 0, dp(16), dp(8));
            body.addView(noteView, new LinearLayout.LayoutParams(-1, -2));
        }
        for (int i = 0; i < items.length; ++i) {
            final int index = i;
            final MaterialButton row = new MaterialButton(this);
            row.setText(items[i]);
            row.setAllCaps(false);
            row.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            row.setInsetTop(0);
            row.setInsetBottom(0);
            row.setMinWidth(0);
            row.setMinimumWidth(0);
            row.setMinimumHeight(dp(48));
            row.setSingleLine(false);
            row.setPadding(dp(16), dp(6), dp(16), dp(6));
            type.applyTypeRole(row, M3Tokens.TypeRole.BODY_LARGE, tfHan);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                onPick.accept(index);
            });
            body.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        scroll.addView(body, new android.widget.FrameLayout.LayoutParams(-1, -2));
        column.addView(scroll, new LinearLayout.LayoutParams(-1,
                sheetBodyHeight(items.length + (note == null || note.isEmpty() ? 0 : 2), 52)));
        showSheet(dialog, column);
    }

    /** The feedback group, in the app's own card rather than a system dialog.
     *
     *  <p>Auto-shown once per build: a group nobody can find is useless, and a
     *  card on every launch is a card people learn to dismiss without reading.
     *  The About sheet carries a permanent entry, so the way back never depends
     *  on this having appeared. */
    private void maybeShowFeedbackGroup() {
        final android.content.SharedPreferences p = getSharedPreferences(PROMO_PREFS, MODE_PRIVATE);
        if (p.getBoolean(PROMO_MUTED, false)) {
            return;
        }
        if (p.getInt(PROMO_SEEN_CODE, 0) == UpdateCheck.versionCode(this)) {
            return;
        }
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || !uiReady) {
                return;
            }
            showFeedbackGroup(true);
        }, 1800);
    }

    /** The feedback card header and its close link. */
    private void addFeedbackHeader(LinearLayout card, android.app.Dialog dialog) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), 0, dp(16), 0);
        head.addView(type.mixedM3Label("FEEDBACK / 反馈与交流",
                        M3Tokens.TypeRole.LABEL_LARGE, theme.primary()),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView close = type.m3Label("关闭", M3Tokens.TypeRole.LABEL_LARGE,
                theme.onSurfaceVariant(), tfHan);
        close.setPaintFlags(close.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        close.setPadding(dp(12), dp(14), 0, dp(14));
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        card.addView(head, new LinearLayout.LayoutParams(-1, dp(42)));
    }

    /** The feedback card copy and group number. */
    private void addFeedbackBody(LinearLayout card) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), 0, dp(16), 4);
        TextView title = type.m3Label("遇到问题，或者想聊冲印？",
                M3Tokens.TypeRole.HEADLINE_SMALL, theme.onSurface(), tfHan);
        body.addView(title);
        TextView copyText = type.m3Label("渲染报错、导出异常、想要某支胶片，直接发在群里。"
                + "问题能复现的话，把「复制错误信息」的内容一起贴上。",
                M3Tokens.TypeRole.BODY_MEDIUM, theme.onSurfaceVariant(), tfHan);
        copyText.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams copyTextLp = new LinearLayout.LayoutParams(-1, -2);
        copyTextLp.topMargin = dp(9);
        body.addView(copyText, copyTextLp);

        TextView number = type.m3Label(QQ_GROUP, M3Tokens.TypeRole.DISPLAY_SMALL,
                theme.primary(), tfDisplay);
        LinearLayout.LayoutParams numberLp = new LinearLayout.LayoutParams(-1, -2);
        numberLp.topMargin = dp(16);
        body.addView(number, numberLp);
        TextView hint = type.m3Label("QQ 群号，点下面按钮复制后搜索加入",
                M3Tokens.TypeRole.LABEL_MEDIUM, theme.onSurfaceVariant(), tfHan);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, -2);
        hintLp.topMargin = dp(5);
        body.addView(hint, hintLp);
        card.addView(body, new LinearLayout.LayoutParams(-1, -2));
    }

    /** The open and copy actions. */
    private void addFeedbackActions(LinearLayout card) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView open = type.m3Label("打开 QQ", M3Tokens.TypeRole.LABEL_LARGE,
                theme.primary(), tfHan);
        open.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable openBg = new android.graphics.drawable.GradientDrawable();
        openBg.setColor(theme.role(com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Palette.Role.SURFACE_ELEVATED));
        openBg.setStroke(dp(1), theme.primary());
        open.setBackground(openBg);
        open.setOnClickListener(v -> openQqGroup());
        LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        openLp.rightMargin = dp(8);
        actions.addView(open, openLp);

        TextView copy = type.m3Label("复制群号", M3Tokens.TypeRole.LABEL_LARGE,
                theme.role(com.google.android.material.R.attr.colorOnPrimary,
                        Palette.Role.ON_ACCENT), tfHan);
        copy.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable copyBg = new android.graphics.drawable.GradientDrawable();
        copyBg.setColor(theme.primary());
        copy.setBackground(copyBg);
        copy.setOnClickListener(v -> {
            if (copyToClipboard("SpektraLab 反馈群", QQ_GROUP)) {
                copy.setText("已复制");
            } else {
                copy.setText("复制失败 · 请长按");
            }
        });
        copy.setLongClickable(true);
        copy.setOnLongClickListener(v -> {
            copyToClipboard("SpektraLab 反馈群", QQ_GROUP);
            return true;
        });
        actions.addView(copy, new LinearLayout.LayoutParams(0, dp(42), 1.45f));
        card.addView(actions, new LinearLayout.LayoutParams(-1, -2));
    }

    /** The mute link shown on the automatic popup. */
    private void addFeedbackMute(LinearLayout card, android.app.Dialog dialog,
                                          android.content.SharedPreferences p) {
        TextView mute = type.m3Label("不再自动弹出", M3Tokens.TypeRole.LABEL_MEDIUM,
                theme.onSurfaceVariant(), tfHan);
        mute.setGravity(android.view.Gravity.CENTER);
        mute.setPaintFlags(mute.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        mute.setPadding(0, 0, 0, dp(14));
        mute.setOnClickListener(v -> {
            p.edit().putBoolean(PROMO_MUTED, true).apply();
            dialog.dismiss();
            showSnackbar("已关闭，仍可在「关于」里找到反馈群");
        });
        card.addView(mute, new LinearLayout.LayoutParams(-1, dp(30)));

    }

    /** Show the card and record that this build has mentioned it. */
    private void showFeedbackCard(android.app.Dialog dialog, LinearLayout card,
                                           android.content.SharedPreferences p) {
        dialog.setContentView(card);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            int width = Math.min(dp(358), getResources().getDisplayMetrics().widthPixels - dp(32));
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.32f);
        }
        // Shown counts as seen: the point is that a new build mentions it once.
        p.edit().putInt(PROMO_SEEN_CODE, UpdateCheck.versionCode(this)).apply();
    }

    void showFeedbackGroup(final boolean auto) {
        if (isFinishing()) {
            return;
        }
        final android.content.SharedPreferences p = getSharedPreferences(PROMO_PREFS, MODE_PRIVATE);
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(theme.role(
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Palette.Role.SURFACE_ELEVATED));
        View cap = new View(this);
        cap.setBackgroundColor(theme.primary());
        card.addView(cap, new LinearLayout.LayoutParams(-1, dp(3)));

        addFeedbackHeader(card, dialog);
        addFeedbackBody(card);
        addFeedbackActions(card);
        if (auto) {
            addFeedbackMute(card, dialog, p);
        }
        showFeedbackCard(dialog, card, p);
    }

    private boolean copyToClipboard(String labelText, String text) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText(labelText, text));
            return true;
        } catch (Throwable t) {
            try { DebugSink.text("clipboard", String.valueOf(t)); } catch (Throwable ignored) { }
            return false;
        }
    }

    /** QQ's group card, then the search route. A ROM without QQ simply gets
     *  the number copied; that is a working answer, not a failure. */
    private void openQqGroup() {
        final String[] schemes = {
                "mqqwpa://im/chat?chat_type=group&uin=" + QQ_GROUP + "&version=1&src_type=web",
                "mqq://im/chat?chat_type=group&uin=" + QQ_GROUP + "&version=1&src_type=web",
        };
        for (String scheme : schemes) {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(scheme)));
                return;
            } catch (Throwable ignored) { }
        }
        copyToClipboard("SpektraLab 反馈群", QQ_GROUP);
        showSnackbar("群号已复制，请在 QQ 里搜索加入");
    }

    /**
     * A choice bottom sheet built from real M3 radio buttons. The current
     * choice is the checked button, so TalkBack announces it without a second
     * custom description; the full stock name is the row's own text.
     */
    private void showChoiceSheet(String title, String note, List<String> labels,
                                 int selectedIndex, java.util.function.IntConsumer onPick) {
        if (labels == null || labels.isEmpty() || isFinishing()) {
            return;
        }
        dismissOpenSheet();
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setTitle(title);
        final LinearLayout column = sheetColumn(dialog, title);

        if (note != null && !note.isEmpty()) {
            final TextView noteView = type.m3Label(note, M3Tokens.TypeRole.BODY_MEDIUM,
                    theme.onSurfaceVariant(), tfHan);
            noteView.setLineSpacing(dp(3), 1f);
            noteView.setPadding(dp(16), 0, dp(16), dp(4));
            column.addView(noteView, new LinearLayout.LayoutParams(-1, -2));
        }

        final RadioGroup group = new RadioGroup(this);
        group.setOrientation(LinearLayout.VERTICAL);
        final MaterialRadioButton[] rows = new MaterialRadioButton[labels.size()];
        for (int i = 0; i < labels.size(); ++i) {
            final MaterialRadioButton row = new MaterialRadioButton(this);
            row.setId(View.generateViewId());
            row.setText(labels.get(i));
            row.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,
                    M3Tokens.TypeRole.BODY_LARGE.sizeSp);
            row.setTypeface(tfHan);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(4), dp(16), dp(4));
            row.setMinHeight(dp(48));
            row.setSingleLine(false);
            row.setMaxLines(3);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            rows[i] = row;
            group.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        if (selectedIndex >= 0 && selectedIndex < rows.length) {
            rows[selectedIndex].setChecked(true);
            // Tapping the current choice closes the sheet without changing the
            // Look; choosing another row is handled by the group below.
            rows[selectedIndex].setOnClickListener(v -> dialog.dismiss());
        }
        group.setOnCheckedChangeListener((g, checkedId) -> {
            if (checkedId == View.NO_ID) {
                return;
            }
            for (int i = 0; i < rows.length; ++i) {
                if (rows[i].getId() != checkedId) {
                    continue;
                }
                dialog.dismiss();
                onPick.accept(i);
                return;
            }
        });

        final NestedScrollView scroll = new NestedScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(group, new android.widget.FrameLayout.LayoutParams(-1, -2));
        column.addView(scroll, new LinearLayout.LayoutParams(-1,
                sheetBodyHeight(labels.size(), 56)));
        showSheet(dialog, column);
    }

    private void resetFilmPanel() {
        // 重置当前页 is the page's presentation, not the Look: the facet goes
        // back to 全部 and the shelf back to its first stock, while the Film,
        // paper, and exposure the user chose stay in force and no render runs.
        brandFilter = FilmBrand.ALL;
        refreshFilmPanel(true);
    }

    private void resetFormatPanel() {
        // The page's default is a Gate, not a bare number: resetting the Scale
        // must not leave the Edge claiming a different piece of film.
        selectGate(defaultGateId());
        onEdgeChanged();
    }

    private void resetEdgePanel() {
        live().overscanOn = false;
        live().dateOn = false;
        live().dateText = "";
        live().edgeMode = "strip";
        live().edgeGate = "auto";
        live().edgeHoles = "white";
        live().edgeCarrier = "none";
        live().edgeTextOn = true;
        live().edgeFog = 1f;
        live().edgeLeaks = 0f;
        live().frameNumber = 0f;
        live().edgeFNumber = 0f;
        live().cameraSeed = 1;
        live().frameSeed = 1;
        live().datePlacement = "frame";
        live().dateCorner = "br";
        live().dateStyle = "lcd";
        live().dateInsetX = 3f;
        live().dateInsetY = 2.4f;
        live().dateSize = 1f;
        live().dateEv = 3.5f;
        live().dataText = "";
        live().dateTextB = "";
        syncToggleRows();
        syncEdgeDateDetail();
        if (session != null) {
            onEdgeChanged();
        }
    }

    private void toggleOriginal() {
        if (beforeBitmap == null) {
            showSnackbar("先导入一张照片");
            return;
        }
        // An unrendered import is already the original: pressing this must not
        // become the thing that starts a render.
        if (showingOriginal && lastRenderedBitmap == null) {
            showSnackbar("选一张胶片后才会渲染");
            return;
        }
        showingOriginal = !showingOriginal;
        if (currentFrame >= 0 && currentFrame < frames.size()) {
            frames.get(currentFrame).setOriginal(showingOriginal);
        }
        if (showingOriginal) {
            compare.setAfter(null);
            emptyState.setVisibility(View.GONE);
            rawState.setVisibility(View.GONE);
            compare.setVisibility(View.VISIBLE);
            statusText.setText("ORIGINAL · 未渲染");
            setOriginalVisual(true);
            refreshFilmPanel();
            return;
        }
        setOriginalVisual(false);
        syncScanAvailability();
        if (lastRenderedBitmap != null) {
            compare.setAfter(lastRenderedBitmap);
            compare.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            rawState.setVisibility(View.GONE);
            statusText.setText("LIVE");
            refreshFilmPanel();
        } else if (session != null && uiReady) {
            renderImage();
        }
    }

        /** Only the Gates whose shape this Frame is: the engine's own 5 % rule. */
    private void refreshEdgeFormats() {
        edgeLabels.clear();
        edgeIds.clear();
        final double aspect = (sourceWidth > 0 && sourceHeight > 0)
                ? (double) Math.max(sourceWidth, sourceHeight) / Math.min(sourceWidth, sourceHeight)
                : 0.0;
        final List<GateScale.Gate> available = GateScale.available(sourceWidth, sourceHeight);
        int best = 0;
        double bestError = Double.MAX_VALUE;
        for (int i = 0; i < available.size(); ++i) {
            final GateScale.Gate gate = available.get(i);
            final double error = aspect <= 0 ? 0.0 : Math.abs(aspect / gate.aspect - 1.0);
            edgeLabels.add(gate.label);
            edgeIds.add(gate.id);
            if (error < bestError) { bestError = error; best = i; }
        }

        if (!edgeIds.isEmpty()) {
            if (!edgeIds.contains(live().overscanFormat)) {
                live().overscanFormat = edgeIds.get(Math.max(0, Math.min(best, edgeIds.size() - 1)));
            }
            // The Scale is the Gate's measured long edge, never a neighbouring
            // page value: 6x17 is 168 mm, not the old 170 mm card.
            final GateScale.Gate gate = GateScale.byId(live().overscanFormat);
            if (gate != null) {
                live().filmFormatMm = gate.longEdgeMm;
            }
        } else {
            // No Gate fits this shape. Keep the stored pair consistent even
            // though the Rebate has nothing to cut.
            final GateScale.Gate pair = GateScale.pair(live().overscanFormat, live().filmFormatMm);
            live().overscanFormat = pair.id;
            live().filmFormatMm = pair.longEdgeMm;
            if (live().overscanOn) {
                // A rebate cannot be drawn for a shape no Gate can hold. Leaving
                // it on would send the engine a layout it has already refused.
                live().overscanOn = false;
                syncToggleRows();
            }
        }

        updateEdgeValue();
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(live().filmFormatMm) + " mm");
        }
        rebuildFormatCards();
        // The page's Gate row and switch follow the same available set: a shape
        // no Gate can hold cannot keep the Rebate on.
        if (edgePagesPanel != null) {
            edgePagesPanel.syncRebate();
        }
        try {
            DebugSink.text("overscan", "aspect=" + aspect + " gates=" + edgeIds
                    + " chosen=" + live().overscanFormat + " scale=" + live().filmFormatMm);
        } catch (Throwable ignored) { }
    }

    /**
     * A rebate or date change is an edge-only edit: the UI never states the
     * Look itself. The render thread is the single full-Look writer, so a page
     * change cannot re-state the whole Look twice or park the UI behind a long
     * render's lock. When there is nothing to draw, the next render states
     * everything anyway.
     */
    private void onEdgeChanged() {
        if (!uiReady || session == null || live().filmStock == null) {
            return;
        }
        showingOriginal = false;
        setOriginalVisual(false);
        try {
            renderImage();
        } catch (Throwable t) {
            // A rejected render must not take the UI down with it.
            showErrorSnackbar("片边没有更新", describe(t));
        }
    }

    /** The manufacturer's edge print: the stock's own catalogue name. */
    private String edgeTextFor(String stock) {
        final int i = filmIds.indexOf(stock);
        final String name = (i >= 0 && i < filmLabels.size()) ? filmLabels.get(i) : "KODAK";
        return name.toUpperCase(java.util.Locale.US);
    }

    /** "'26 10 31" — a typed imprint, or the frame's own EXIF date. */
    private String typedDateText() {
        final String text = live().dateText;
        return text == null || text.isEmpty() ? null : text;
    }

    /** A typed imprint, or the frame's own EXIF date. */
    private String imprintDate() {
        final String typed = typedDateText();
        if (typed != null) {
            return typed;
        }
        return defaultImprintDate();
    }

    /**
     * The Frame's own default: its EXIF capture date, or today when the file
     * has none. Cached for the open image; a typed text never overwrites it.
     */
    private String defaultImprintDate() {
        if (defaultDateText != null && !defaultDateText.isEmpty()) {
            return defaultDateText;
        }
        String exif = null;
        try {
            if (sourceUri != null) {
                try (InputStream in = getContentResolver().openInputStream(sourceUri)) {
                    if (in != null) {
                        exif = new androidx.exifinterface.media.ExifInterface(in)
                                .getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL);
                    }
                }
            }
        } catch (Throwable ignored) { }
        String text = DateBack.formatExif(exif);
        if (text == null) {
            // The file has no usable capture date. The page still needs a
            // starting value, so the caller supplies today's date here rather
            // than letting the pure formatter read a clock behind its back.
            final java.util.Calendar c = java.util.Calendar.getInstance();
            text = DateBack.formatDate(c.get(java.util.Calendar.YEAR),
                    c.get(java.util.Calendar.MONTH) + 1,
                    c.get(java.util.Calendar.DAY_OF_MONTH));
        }
        defaultDateText = text;
        return defaultDateText;
    }

    /** A custom imprint, or the Frame's own EXIF date until the user writes one. */
    private String effectiveDateText() {
        final String typed = typedDateText();
        return typed != null ? typed : imprintDate();
    }

    /**
     * A two-state Edge row built on the real M3 switch. `current` is the
     * authoritative Look field, so a rejected change (a Gate that does not fit)
     * reverts the switch instead of leaving its checked state out of step with
     * the engine value.
     *
     * <p>The optional detail line is the date back's imprint, shown only while
     * the switch is on and using the same text the engine is given.
     */
    private View switchRow(String labelText, TextView detail, final MaterialSwitch toggle,
                           java.util.function.BooleanSupplier current,
                           java.util.function.Consumer<Boolean> onChange) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.addView(type.m3Label(labelText, M3Tokens.TypeRole.LABEL_LARGE,
                theme.role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK), tfHan));
        if (detail != null) {
            LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
            detailLp.topMargin = dp(3);
            textCol.addView(detail, detailLp);
        }
        row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1f));

        final boolean initial = current.getAsBoolean();
        toggle.setChecked(initial);
        // An 8 dp pad above and below makes the switch's own touch bounds 48 dp.
        toggle.setPadding(0, dp(8), 0, dp(8));
        toggle.setContentDescription(toggleDescription(labelText, initial));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            if (syncingToggles) {
                return;
            }
            onChange.accept(checked);
            final boolean now = current.getAsBoolean();
            if (button.isChecked() != now) {
                syncingToggles = true;
                button.setChecked(now);
                syncingToggles = false;
            }
            button.setContentDescription(toggleDescription(labelText, now));
        });
        row.addView(toggle, new LinearLayout.LayoutParams(-2, -2));

        row.setMinimumHeight(dp(MIN_TOUCH_DP));
        row.setClickable(true);
        row.setOnClickListener(v -> toggle.toggle());
        row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return row;
    }

    private static String toggleDescription(String labelText, boolean on) {
        return labelText + "，" + (on ? "开" : "关");
    }

    /** Show the date back's line only while the imprint is enabled. */
    private void syncEdgeDateDetail() {
        if (edgePagesPanel != null) {
            edgePagesPanel.syncDateDetail();
        }
    }

    /** Re-apply a restored Look to the Edge pages' switches and controls. */
    private void syncToggleRows() {
        if (edgePagesPanel == null) {
            return;
        }
        edgePagesPanel.syncRebate();
        edgePagesPanel.syncDate();
    }

    /**
     * One render for a completed optics gesture. The render thread sends the
     * parameter delta under the same lock as the draw, so a gesture produces
     * one update and one picture.
     */
    private void commitOptics() {
        if (opticsSlider != null) {
            opticsSlider.removeCallbacks(opticsCommitRunnable);
        }
        if (uiReady && session != null) {
            showingOriginal = false;
            setOriginalVisual(false);
            recordOpticsEdit();
            renderImage();
        }
    }

    /** Record the settled Optics/View state in this Frame's session. */
    private void recordOpticsEdit() {
        if (currentSession != null) {
            currentSession.record();
        }
    }

    private void commitViewAdjust() {
        viewAdjustHandler.removeCallbacks(viewAdjustCommit);
        recordOpticsEdit();
        renderImage();
    }

    /** The old four-step ribbon is gone; the render state is in the canvas foot. */
    private void setStep(int active) { }

    private void setTab(int active) {
        final int previous = activeTabIndex;
        activeTabIndex = active;
        for (int i = 0; i < panels.length; ++i) {
            panels[i].setVisibility(i == active ? View.VISIBLE : View.GONE);
        }
        syncTabLayout(active);
        syncNavigationRail(active);
        animatePanelIn(previous, active);
    }

    private void syncTabLayout(int active) {
        if (tabLayout == null) {
            return;
        }
        final TabLayout.Tab tab = tabLayout.getTabAt(active);
        if (tab != null && tabLayout.getSelectedTabPosition() != active) {
            suppressWorkspaceSelection = true;
            try {
                tabLayout.selectTab(tab, false);
            } finally {
                suppressWorkspaceSelection = false;
            }
        }
    }

    private void syncNavigationRail(int active) {
        if (navigationRail == null || active < 0 || active >= TAB_ITEM_IDS.length) {
            return;
        }
        final MenuItem item = navigationRail.getMenu().findItem(TAB_ITEM_IDS[active]);
        if (item != null && !item.isChecked()) {
            suppressWorkspaceSelection = true;
            try {
                navigationRail.setSelectedItemId(TAB_ITEM_IDS[active]);
            } finally {
                suppressWorkspaceSelection = false;
            }
        }
    }

    private void animatePanelIn(int previous, int active) {
        if (!uiReady || previous == active || active < 0 || active >= panels.length) {
            return;
        }
        final int duration = motionDuration(170);
        final View panel = panels[active];
        panel.setAlpha(duration > 0 ? 0f : 1f);
        panel.setTranslationY(dp(4));
        if (duration > 0) {
            panel.animate().alpha(1f).translationY(0f).setDuration(duration).start();
        } else {
            panel.setTranslationY(0f);
        }
    }

    /**
     * Rows at the top and bottom of a frame that carry no picture at all -- the
     * "two big black bars" report, measured rather than guessed. A render that
     * comes back letterboxed says so in the status line.
     */
    private static int[] blackBands(Bitmap b) {
        final int w = b.getWidth(), h = b.getHeight();
        final int[] row = new int[w];
        int top = 0, bottom = 0;
        for (int y = 0; y < h; ++y) {
            b.getPixels(row, 0, w, 0, y, w, 1);
            boolean black = true;
            for (int x = 0; x < w && black; ++x) {
                black = (row[x] & 0xFFFFFF) <= 0x060606;
            }
            if (!black) { top = y; break; }
        }
        for (int y = h - 1; y >= 0; --y) {
            b.getPixels(row, 0, w, 0, y, w, 1);
            boolean black = true;
            for (int x = 0; x < w && black; ++x) {
                black = (row[x] & 0xFFFFFF) <= 0x060606;
            }
            if (!black) { bottom = h - 1 - y; break; }
        }
        return new int[]{top, bottom};
    }

    /** The optics controls only mean something once there is a frame to change. */
    private void setOpticsUnlocked(boolean unlocked) {
        opticsUnlocked = unlocked;
        if (opticsPanel == null) {
            return;
        }
        final float alpha = unlocked ? 1f : M3Tokens.DISABLED_CONTENT_ALPHA;
        if (opticsStripScroll != null) {
            opticsStripScroll.setAlpha(alpha);
            opticsStripScroll.setEnabled(unlocked);
        }
        for (Chip chip : opticsChips) {
            chip.setEnabled(unlocked);
        }
        if (opticsSlider != null) {
            opticsSlider.setEnabled(unlocked);
        }
        if (opticsReset != null) {
            opticsReset.setEnabled(unlocked);
        }
        if (opticsFilterButton != null) {
            opticsFilterButton.setEnabled(unlocked);
        }
        if (opticsNormButton != null) {
            opticsNormButton.setEnabled(unlocked);
        }
        if (opticsLockNote != null) {
            opticsLockNote.setText(unlocked ? "" : "LOCKED · " + opticsLockReason());
            opticsLockNote.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        }
    }

    /** Which of the three things on the stage is showing. */
    private void setStage(int mode, String name) {
        stageMode = mode;
        if (mode == 0) {
            compare.clear();
        }
        emptyState.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        compare.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        rawState.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
        if (mode == 0) {
            showingOriginal = false;
            setOriginalVisual(false);
        }
        if (name != null && !name.isEmpty()) {
            if (fileLabel != null) {
                fileLabel.setText(name);
            }
            rawName.setText(name);
        }
        if (stageMeta != null) {
            stageMeta.setText(mode == 2 ? "RAW LOADED"
                    : mode == 1 ? (isRawFile ? "RAW PREVIEW" : "JPEG") : "NO FRAME");
        }
        syncScanAvailability();
    }

    /**
     * The two doors, asked on every tap of 01.
     *
     * They are not the same door: the gallery's picker only shows what the
     * media store knows as an image, and a Sony .ARW -- or a HEIC a download
     * left untyped -- is not one. Long-press used to hold this and nobody
     * found it; a tap asking a question costs one tap and answers it.
     */
    private void showImportMenu() {
        showActionSheet("导入照片", "从相册或文件管理器选择", new String[]{
                "从相册选择（照片）",
                "从文件管理器选择（RAW / HEIC / 任意文件）",
        }, which -> {
            // A deliberate pick is a *new* photograph, not a chip coming back.
            // Cleared here so a chip whose file disappeared cannot leave the
            // flag set and make the next import silently skip the strip.
            reopeningFrame = false;
            suppressBudgetDialog = false;
            saveLookIntoFrames();
            if (which == 0) {
                pickImage();
            } else {
                pickRawFile();
            }
        });
    }

    /** Export tiers come from the session and the device, not a fixed sensor. */
    /**
     * Hands the file that was just written to another app, bypassing the file
     * picker. Lightroom's own picker asks Android for pictures; on some ROMs the
     * picture list has no entry for TIFF, so a perfectly good .tif never
     * appears in it -- while an ACTION_SEND carrying "image/tiff" reaches it
     * fine. Same class of problem Adobe documents for CR3 on Android.
     */
    private void shareLastExport() {
        if (lastExportUri == null) {
            showSnackbar("还没有导出过文件");
            return;
        }
        try {
            final Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(lastExportMime == null ? "image/*" : lastExportMime);
            send.putExtra(Intent.EXTRA_STREAM, lastExportUri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "发送到"));
        } catch (Throwable t) {
            showErrorSnackbar("无法发送", describe(t));
        }
    }

    private void showExportDialog() {
        if (session == null) {
            showSnackbar("先导入一张照片");
            return;
        }
        final String full = (sourceWidth > 0 && sourceHeight > 0)
                ? "全尺寸 " + sourceWidth + "×" + sourceHeight : "全尺寸";
        final String fullNote = probe.isLimited()
                ? "（本机上限约 " + String.format(java.util.Locale.US, "%.1f", maxImportPixels() / 1e6)
                  + " MP，可能导出失败）"
                : "（原图）";
        // What the driver says it can take, as opposed to what this build
        // currently lets through. The two were only ever visible as one number,
        // and it was always the smaller -- so a phone reporting 2 GiB of
        // maxStorageBufferRange looked like it was capped at 15 MP for no
        // stated reason. Name both, and say which one is temporary.
        final String driverLine;
        if (probe.maxStorageBufferRange > 0) {
            driverLine = String.format(java.util.Locale.US,
                    "驱动单缓冲 %.0f MiB（分块渲染）；本机内存预算约 %.1f MP",
                    probe.maxStorageBufferRange / 1048576.0, maxImportPixels() / 1e6);
        } else {
            driverLine = probe.valid ? "驱动未报单缓冲上限（分块渲染）" : "设备信息读取失败";
        };
        // One sheet, six rows. It was two -- format, then size -- and the first
        // thing that did was hide "全尺寸（原图）" behind a tap that nobody
        // expects from a menu that used to end in it.
        final boolean limited = probe.isLimited();
        // "the gallery says it is corrupt" is a question this sheet can answer
        // before the file is written, not after: Android's picture viewer does
        // not read 16-bit TIFF at all, and the phone's Lightroom does.
        // DNG is shipped as a file for a desktop, and says so on the tin. The
        // phone's Lightroom refuses every linear DNG this writer produces --
        // four containers, two photometrics, 600x400 through 3000x2000, a
        // correct XYZ(D50) colour matrix, a camera-shaped IFD0 preview -- with
        // "failed to load the development version", which Adobe lists as a
        // known Android bug. Rather than let that read as our defect, the row
        // and the note name the destination and close the question.
        final String note = (limited
                ? "TIFF 是 16 位；"
                  + "导出后点导出旁边的「分享」，分享到 Lightroom 即可使用（手机相册多半不显示 TIFF）。JPEG 哪儿都能看。"
                : "TIFF 是 16 位；"
                  + "导出后点导出旁边的「分享」，分享到 Lightroom 即可使用（手机相册多半不显示 TIFF）。JPEG 是 8 位、哪儿都能看")
                + "\n" + driverLine
                + "\nDNG 只给电脑上的 Lightroom / ACR 用：手机版 Lr 打不开是已知问题，"
                  + "遇到报错不必反馈。";
        new ExportSheet(new ExportSheet.Host() {
            @Override public String note() {
                return note;
            }

            @Override public void showSheet(String title, String body, String[] rows,
                                            java.util.function.IntConsumer onPick) {
                showActionSheet(title, body, rows, onPick::accept);
            }

            @Override public void export(String format, String tier) {
                exportImage(format, tier);
            }

            @Override public void exportBatch(String format, String tier) {
                startBatchExport(format, tier);
            }
        }).show();
    }

    /** Reset from the old single control strip: delegate to the film page. */
    private void resetLook() { resetFilmPanel(); }
                /**
     * A frame this device cannot take at its own size: **asked about, not
     * quietly shrunk**.
     *
     * The import used to scale silently (a toast, on the RAW path only), which
     * reads as "the app ate my resolution" the next time the user exports a
     * 4700-pixel frame from a 45 MP negative. The two numbers are the whole
     * story: what the file is, and what this phone renders in one go.
     */
    private void showImportBudgetDialog(final Uri uri, final int w, final int h, final int capPixels) {
        final long available = availableMemoryBytes();
        final ImportBudgetSheet.Request request = new ImportBudgetSheet.Request(
                uri, w, h, capPixels, IMPORT_LONG_EDGE,
                available > 0 ? (int) DeviceProbe.maxPixelsByMemory(available) : 0,
                live().overscanOn);
        new ImportBudgetSheet(new ImportBudgetSheet.Host() {
            @Override public void showSheet(String title, String body, String[] rows,
                                            java.util.function.IntConsumer onPick) {
                showActionSheet(title, body, rows, onPick::accept);
            }

            @Override public void importFit(Uri picked) {
                importBudgetAccepted = true;
                loadImage(picked);
            }

            @Override public void importOriginal(Uri picked) {
                importAtNativeSize = true;
                loadImage(picked);
            }
        }).show(request);
    }

    private static String megapixels(int w, int h) {
        return String.format(java.util.Locale.US, "%.1f", (double) w * (double) h / 1e6);
    }

    /** The file's own size, from the header, before anything is decoded. */
    private int[] sniffImageSize(Uri uri) {
        try {
            android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream head = getContentResolver().openInputStream(uri)) {
                if (head != null) {
                    android.graphics.BitmapFactory.decodeStream(head, null, bounds);
                }
            }
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                return new int[]{bounds.outWidth, bounds.outHeight};
            }
        } catch (Throwable t) {
            try { DebugSink.text("decode", "sniff " + t); } catch (Throwable ignored) { }
        }
        return null;
    }

    /** Long-press on the title: version, update check, and where the code lives. */
    /**
     * The probe, re-read if it never landed.
     *
     * `ensureEngine()` reads it exactly once, immediately after the engine is
     * created -- and `engine` is assigned first, so a throw in that one call
     * leaves `probe` at `parse("{}")` for the life of the process while every
     * render works perfectly. The device then reads as "设备信息读取失败" and,
     * worse, `maxImportPixels()` loses the one number it is derived from.
     */
    /**
     * Ask the driver whether it enforces the range it reports.
     *
     * The canary is the only way to tell a device that merely reports 128 MiB
     * from one that enforces it: past the limit the writes are dropped and no
     * error comes back, so a soft report and a hard wall are identical from
     * the API. It runs once, on its own thread, as soon as the engine exists --
     * before the first import asks for a budget.
     *
     * The import budget stays on the reported value until the answer lands,
     * which is the conservative side: someone who imports immediately is
     * never handed a larger frame than the measurement would have allowed.
     */
    private void startCanaryProbe() {
        if (canaryStarted || engine == null) {
            return;
        }
        canaryStarted = true;
        final SpektraLabEngine e = engine;
        new Thread(() -> {
            String json = null;
            try {
                synchronized (renderMutex) { json = e.probeCanary(DeviceProbe.CANARY_NEED_BYTES); }
            } catch (Throwable t) {
                try { DebugSink.text("canary", "threw: " + describe(t)); } catch (Throwable ignored) { }
            }
            final String result = json;
            final long budget = maxImportPixels();
            runOnUiThread(() -> {
                try {
                    if (result != null) {
                        probe.applyCanary(result);
                    }
                    DebugSink.text("canary", (result == null ? "(no answer)" : result)
                            + "\n" + probe.describe());
                } catch (Throwable ignored) { }
                try { DeviceReport.submit(this, probe, budget); } catch (Throwable ignored) { }
            });
        }, "canary-probe").start();
    }

    private DeviceProbe probeNow() {
        if (!probe.valid && engine == null) {
            // "关于" can be opened before anything is imported, which is the
            // usual way this reads as a failure: there is no engine yet, so
            // there is nothing to ask. Build it.
            try { ensureEngine(); }
            catch (Throwable t) {
                try { DebugSink.text("device-profile", "engine create failed: " + describe(t)); }
                catch (Throwable ignored) { }
            }
        }
        if (!probe.valid && engine != null) {
            try {
                final String json = engine.deviceProfile();
                probe = DeviceProbe.parse(json);
                try { DebugSink.text("device-profile", "re-read " + probe.describe() + "\n" + json); }
                catch (Throwable ignored) { }
            } catch (Throwable t) {
                try { DebugSink.text("device-profile", "re-read failed: " + describe(t)); }
                catch (Throwable ignored) { }
            }
        }
        return probe;
    }

    private void showAbout() {
        probeNow();
        final String version = UpdateCheck.versionName(this) + "（" + UpdateCheck.versionCode(this) + "）";
        final String details = probe.describe()
                + "\n\n引擎：SpektraLab / spektrafilm（GPL-3.0，Andrea Volpato 的光谱胶片模型）"
                + "\n移植：github.com/SAINTTaiYi/SpektraLab-Android";
        // The report switch lives here rather than in a settings screen: it is
        // one line of one dialog, and it is the only thing this app sends that
        // the user did not ask for. The label carries the state, because the
        // sheet's note cannot scroll and a longer paragraph would be clipped.
        final boolean reporting = DeviceReport.enabled(this);
        showActionSheet("SpektraLab Android " + version, details, new String[]{
                "检查更新",
                reporting ? "驱动报告：开（每周一次，只报机型和 GPU）" : "驱动报告：关",
                "反馈群（QQ 1134320449）", "打开 GitHub", "关闭"
        }, which -> {
            if (which == 0) {
                UpdateCheck.check(this, true);
            } else if (which == 1) {
                DeviceReport.setEnabled(this, !reporting);
                showAbout();
            } else if (which == 2) {
                showFeedbackGroup(false);
            } else if (which == 3) {
                try {
                    startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/SAINTTaiYi/SpektraLab-Android/releases/latest")));
                } catch (Throwable ignored) { }
            }
        });
    }

    /** Reads assets/resources/stock_catalog.json (generated from the engine's profiles). */
    private void loadStockCatalog() {
        // A theme rebuild runs this again through the composition root. Clear
        // first, or every toggle appends a second copy of the catalogue.
        filmCatalogue.clear();
        printCatalogue.clear();
        filmStocks.clear();
        try (InputStream in = getAssets().open("resources/stock_catalog.json")) {
            final org.json.JSONObject root = new org.json.JSONObject(
                    new String(readFully(in), java.nio.charset.StandardCharsets.UTF_8));
            final org.json.JSONArray films = root.optJSONArray("films");
            filmCatalogue.addAll(StockCatalogue.read(films));
            printCatalogue.addAll(StockCatalogue.read(root.optJSONArray("prints")));
            readFilmStocks(films);
        } catch (Throwable t) {
            android.util.Log.w("SpektraLab", "stock catalog: " + t);
        }
        if (filmCatalogue.isEmpty()) {
            filmCatalogue.add(new StockCatalogue.Stock(
                    "kodak_portra_400", "Kodak Portra 400", "negative", ""));
        }
        if (printCatalogue.isEmpty()) {
            printCatalogue.add(new StockCatalogue.Stock(
                    "kodak_portra_endura", "Kodak Portra Endura", "print", ""));
        }

        refreshFilmPanel();
        refreshPaperLabel();
    }

    /** Read an asset to its end. The catalogue now carries spectral arrays, so
     *  its size is no longer something `available()` can be trusted to predict. */
    private static byte[] readFully(InputStream in) throws java.io.IOException {
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        final byte[] chunk = new byte[16 * 1024];
        int n;
        while ((n = in.read(chunk)) > 0) {
            out.write(chunk, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * Read each film record: the baked spectral arrays (record-level, or the
     * older {@code spectral}/{@code data} nesting), the declared source, and
     * the real scalar info fields. Missing arrays stay missing: the panel and
     * the Readout strip show the explicit unavailable state rather than a
     * fabricated series.
     */
    private void readFilmStocks(org.json.JSONArray films) {
        if (films == null) {
            return;
        }
        for (int i = 0; i < films.length(); ++i) {
            final org.json.JSONObject o = films.optJSONObject(i);
            if (o == null) {
                continue;
            }
            final String id = o.optString("id");
            if (id.isEmpty()) {
                continue;
            }
            org.json.JSONObject data = o.optJSONObject("spectral");
            if (data == null) {
                data = o.optJSONObject("data");
            }
            if (data == null) {
                data = o;
            }
            final String declared = o.optString("spectral_source",
                    data.optString("spectral_source", ""));
            final org.json.JSONArray log = data.optJSONArray("log_sensitivity");
            final org.json.JSONArray density = data.optJSONArray("channel_density");
            final org.json.JSONArray series;
            final String source;
            if ("channel_density".equals(declared) && density != null) {
                series = density;
                source = "channel_density";
            } else if (log != null) {
                series = log;
                source = "log_sensitivity";
            } else {
                series = density;
                source = "channel_density";
            }
            final double[] wavelengths = jsonDoubles(data.optJSONArray("wavelengths"));
            final double[][] rows = jsonRows(series);
            final SpectrumCurve curve = SpectrumCurve.of(wavelengths, rows);
            final String seriesLabel = !curve.available() ? ""
                    : "channel_density".equals(source) ? "DENSITY" : "LOG SENSITIVITY";
            final org.json.JSONObject info = o.optJSONObject("info");
            // `targetPrint` is the generator's record-level field; the profile's
            // `target_print` is the same fact under the older spelling.
            final String targetPrint = PrintTarget.declared(
                    o.optString("targetPrint", ""), o.optString("target_print", ""));
            filmStocks.put(id, new FilmStock(SpectrumSamples.of(wavelengths, rows),
                    curve, seriesLabel, readStockInfo(info == null ? o : info), targetPrint));
        }
    }

    /** Every scalar field the record carries, so the panel can show real values. */
    private static java.util.Map<String, String> readStockInfo(org.json.JSONObject o) {
        final java.util.Map<String, String> info = new java.util.LinkedHashMap<>();
        final java.util.Iterator<String> keys = o.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            if ("id".equals(key) || "name".equals(key) || "spectral_source".equals(key)) {
                continue;
            }
            final Object value = o.opt(key);
            if (value instanceof String) {
                final String text = ((String) value).trim();
                if (!text.isEmpty()) {
                    info.put(key, text);
                }
            } else if (value instanceof Number || value instanceof Boolean) {
                info.put(key, String.valueOf(value));
            }
        }
        return info;
    }

    /** One row per wavelength; absent or non-numeric values become NaN gaps. */
    private static double[] jsonDoubles(org.json.JSONArray array) {
        if (array == null) {
            return null;
        }
        final double[] values = new double[array.length()];
        for (int i = 0; i < values.length; ++i) {
            values[i] = array.optDouble(i, Double.NaN);
        }
        return values;
    }

    /** A matrix of channel values; a single flat array is one channel. */
    private static double[][] jsonRows(org.json.JSONArray array) {
        if (array == null || array.length() == 0) {
            return null;
        }
        if (array.optJSONArray(0) == null) {
            final double[] flat = jsonDoubles(array);
            final double[][] rows = new double[flat.length][];
            for (int i = 0; i < rows.length; ++i) {
                rows[i] = new double[]{flat[i]};
            }
            return rows;
        }
        final double[][] rows = new double[array.length()][];
        for (int i = 0; i < rows.length; ++i) {
            rows[i] = jsonDoubles(array.optJSONArray(i));
        }
        return rows;
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Tap: the gallery, which is where phone photos and JPEGs are.
     *
     * Three intents, in the order the user expects them, and the first one this
     * ROM can actually resolve wins: the system Photo Picker (13+), then
     * `GET_CONTENT image/*` (every Android before 13), then the document
     * picker (every Android, period).
     *
     * The chain is not belt-and-braces. A launch that cannot be resolved throws
     * `ActivityNotFoundException`, and an uncaught throw inside a click handler
     * does not show an error -- it kills the process. That is exactly the
     * report "一选导入就自动退回桌面了" on a Honor/HarmonyOS phone: no message,
     * because there was nothing left alive to show one. Those ROMs ship without
     * the AOSP Photo Picker activity (it lives in the MediaProvider module, and
     * an HMS build does not have it) while the `ACTION_PICK_IMAGES` intent
     * still resolves to the chooser's fallback -- so the check has to be at the
     * launch, and it has to have somewhere to go next.
     */
    private void pickImage() {
        java.util.List<Intent> chain = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            chain.add(new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES));
        }
        chain.add(new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true));
        chain.add(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
        if (!launchFirstResolvable(chain, 1)) {
            showSnackbar("本机没有可用的图片选择器；请点「导入」→「从文件管理器选择」");
        }
    }

    /**
     * Launch the first intent this ROM resolves, and never let the launch itself
     * kill the process. False when none of them is available.
     */
    private boolean launchFirstResolvable(java.util.List<Intent> chain, int requestCode) {
        for (Intent intent : chain) {
            try {
                if (intent.resolveActivity(getPackageManager()) == null) {
                    try { DebugSink.text("picker", "unresolvable " + intent.getAction()); }
                    catch (Throwable ignored) { }
                    continue;
                }
                startActivityForResult(intent, requestCode);
                try { DebugSink.text("picker", "launched " + intent.getAction()); }
                catch (Throwable ignored) { }
                return true;
            } catch (Throwable t) {
                // Resolvable and still refused: a provider that vanished
                // between the check and the launch, a SecurityException, or a
                // picker that throws from its own onCreate. Next one.
                try { DebugSink.text("picker", intent.getAction() + " failed: " + t); }
                catch (Throwable ignored) { }
            }
        }
        return false;
    }

    /**
     * Documents, for RAW. Sony's .ARW and several other RAW containers are
     * typed application/octet-stream by the storage layer, so a picker filtered
     * to image/* hides them completely -- which the user reads, correctly, as
     * "this app cannot import RAW". Asking for any file fixes that, but Android
     * then routes to Files rather than Photos, which is why it is its own
     * entry instead of replacing the gallery.
     *
     * **The same entry is the answer for a HEIC that the gallery will not
     * show.** The MIME list below names the HEIF types explicitly as well as
     * `image/*`, because several storage layers do not map `.heic` to an image
     * type at all -- a downloaded one then appears nowhere in a picker filtered
     * to images, which is what "下载后没法导入" describes.
     */
    private void pickRawFile() {
        final String[] types = {
                "image/*",
                "image/heic", "image/heif", "image/x-heic", "image/x-heif", "image/x-cfif",
                "application/octet-stream",
                "image/x-sony-arw", "image/x-canon-cr2", "image/x-canon-cr3",
                "image/x-nikon-nef", "image/x-adobe-dng", "image/x-panasonic-rw2",
                "image/x-panasonic-raw", "image/x-olympus-orf", "image/x-fuji-raf",
                "image/x-pentax-pef", "image/x-samsung-srw", "image/x-sigma-x3f"
        };
        java.util.List<Intent> chain = new ArrayList<>();
        Intent documents = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        documents.addCategory(Intent.CATEGORY_OPENABLE);
        documents.setType("*/*");
        documents.putExtra(Intent.EXTRA_MIME_TYPES, types);
        documents.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        documents.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        chain.add(documents);
        Intent content = new Intent(Intent.ACTION_GET_CONTENT);
        content.addCategory(Intent.CATEGORY_OPENABLE);
        content.setType("*/*");
        content.putExtra(Intent.EXTRA_MIME_TYPES, types);
        content.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        chain.add(content);
        if (!launchFirstResolvable(chain, 1)) {
            showSnackbar("本机没有可用的文件选择器");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1) {
            return;
        }
        if (resultCode != RESULT_OK) {
            return;  // the user cancelled
        }
        // The picker can return several items in a ClipData; the single-URI
        // field stays for pickers that only hand back one.
        final java.util.List<Uri> picked = new java.util.ArrayList<>();
        if (data != null) {
            final android.content.ClipData clip = data.getClipData();
            if (clip != null) {
                for (int i = 0; i < clip.getItemCount(); ++i) {
                    final Uri item = clip.getItemAt(i).getUri();
                    if (item != null && !picked.contains(item)) {
                        picked.add(item);
                    }
                }
            }
            final Uri single = data.getData();
            if (single != null && !picked.contains(single)) {
                picked.add(single);
            }
        }
        if (picked.isEmpty()) {
            try { DebugSink.text("picker", "RESULT_OK with no data"); } catch (Throwable ignored) { }
            showSnackbar("没有拿到图片（选择器返回空）。请点「导入」→「从文件管理器选择」再试一次");
            return;
        }
        // A new pick replaces the restored current Frame; keep its grant for
        // the next process when the provider offered a persistable one.
        pendingSession = null;
        restoringOriginal = false;
        for (Uri uri : picked) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Throwable ignored) {
                // GET_CONTENT and Photo Picker grants are not persistable.
            }
        }
        if (picked.size() > 1) {
            startBatchImport(picked);
        } else {
            loadImage(picked.get(0));
        }
    }

    /** Reset the live Look session and Scan state before decoding an import. */
    private void prepareImportState(boolean reopening) {
        cancelLivePreview();
        // A new import works out its own imprint date; the previous frame's
        // cached text must not travel with the new Source.
        defaultDateText = null;
        // A chip returns to a photograph and to what was done to it; a pick
        // starts with no film. Scan inspect belongs to the same Frame.
        if (reopening) {
            final Frame target = (currentFrame >= 0 && currentFrame < frames.size())
                    ? frames.get(currentFrame) : null;
            restoreScanStateFromFrame(target);
            attachSession(target == null ? new FrameLookSession(defaultLook()) : target.session());
            applyLook(optics);
        } else {
            saveScanStateIntoFrame();
            restoreScanStateFromFrame(null);
            attachSession(new FrameLookSession(defaultLook()));
            applyLook(optics);
        }
        updateUndoAction();
        applyRestoredLook();
    }

    /** Drop the previous source and canvas before a new decode. */
    private void resetCanvasForImport() {
        // Free the previous frame before decoding: a 20 MP pick wants a
        // ~200 MB direct buffer and the old session still holds its planes.
        // Closing under the render lock avoids the SIGSEGV seen when a
        // render races a session close.
        synchronized (renderMutex) {
            if (session != null) {
                session.close();
                session = null;
            }
        }
        lastRenderedBitmap = null;
        setOpticsUnlocked(false);
        setStage(0, null);
        sourceBitmap = null;
        if (sourcePixels != null) {
            SpektraLabEngine.freeDirect(sourcePixels);
            sourcePixels = null;
        }
        // The canvas is not a control: CompareView holds the last render and
        // it survives a new session unless it is cleared here.
        if (compare != null) {
            compare.clear();
        }
        System.gc();
    }

    /** Decode, rotate and scale one still, publishing its source size. */
    /** Pack the decoded still into the engine's float RGB buffer, one row at a time. */
    private void packSourcePixels() {
        // A full int[] of 20 MP would be another 80 MB on the Java heap next
        // to the direct buffer. `allocateDirect` is capped at the heap growth
        // limit (512 MB on many phones), so a 45 MP frame is malloc'd through
        // the engine instead; the buffer has no cleaner and must be freed.
        if (sourcePixels != null) {
            SpektraLabEngine.freeDirect(sourcePixels);
            sourcePixels = null;
        }
        final long bytes = (long) sourceWidth * sourceHeight * 3L * 4L;
        sourcePixels = SpektraLabEngine.allocDirect(bytes).order(ByteOrder.nativeOrder());
        final int[] rowPixels = new int[sourceWidth];
        for (int y = 0; y < sourceHeight; y++) {
            sourceBitmap.getPixels(rowPixels, 0, sourceWidth, 0, y, sourceWidth, 1);
            for (int px : rowPixels) {
                sourcePixels.putFloat(((px >> 16) & 0xFF) / 255.0f);
                sourcePixels.putFloat(((px >> 8) & 0xFF) / 255.0f);
                sourcePixels.putFloat((px & 0xFF) / 255.0f);
            }
        }
        sourcePixels.rewind();
    }

    /** Publish the still's comparison preview and its stage header. */
    private void publishStillPreview(Uri uri) {
        final float edge = Math.max(sourceWidth, sourceHeight);
        final float preview = Math.min(1f, 1024f / edge);
        final int pw = Math.max(1, Math.round(sourceWidth * preview));
        final int ph = Math.max(1, Math.round(sourceHeight * preview));
        beforeBitmap = Bitmap.createScaledBitmap(sourceBitmap, pw, ph, true);
        // A source already within the preview edge is returned unchanged by
        // createScaledBitmap. Recycling it here would leave CompareView
        // holding a recycled bitmap and crash the next draw.
        if (beforeBitmap != sourceBitmap) {
            sourceBitmap.recycle();
        }
        sourceBitmap = null;
        compare.setBefore(beforeBitmap);
        statusText.setText("Local preview");
        setStage(1, getFileName(uri));
        rememberFrame(uri);
        refreshFilmPanel();
        setStep(1);
    }

    /** Open the engine session that owns the packed still. */
    private void openStillSession() throws Exception {
        ensureEngine();
        synchronized (renderMutex) {
            if (session != null) {
                session.close();
                session = null;
            }
        }
        session = engine.openSession(sourcePixels, sourceWidth, sourceHeight, 3);
        suppressBudgetDialog = false;
    }

    /** Whether the still needs the shrink dialog before it is decoded. */
    private boolean askImportBudget(Uri uri) {
        if (importAtNativeSize || importBudgetAccepted || suppressBudgetDialog) {
            return false;
        }
        final int[] full = sniffImageSize(uri);
        if (full == null) {
            return false;
        }
        final long area = (long) full[0] * (long) full[1];
        final int cap = maxImportPixels();
        if (area > cap || Math.max(full[0], full[1]) > IMPORT_LONG_EDGE) {
            showImportBudgetDialog(uri, full[0], full[1], cap);
            return true;
        }
        return false;
    }

    private boolean decodeStillSource(Uri uri, boolean nativeSize, int forcedCap) {
        final int edgeCap = nativeSize ? Integer.MAX_VALUE : IMPORT_LONG_EDGE;
        final int capPixels = nativeSize ? Integer.MAX_VALUE
        : (forcedCap > 0 ? forcedCap : maxImportPixels());
        sourceBitmap = decodeImport(uri, capPixels, edgeCap);
        if (sourceBitmap == null) {
            // The old message here was "无法解码图片", which gave the user
            // nothing to act on. A HEIC/HEIF this phone's decoder refuses is
            // the common case, so the dialog carries the way out instead of
            // describing it.
            showActionSheet("[E-1107] 这张图本机解不开",
            "多半是 HEIC/HEIF 或 10-bit 的图（三星、iPhone 默认存这种）。\n"
            + "换「从文件选择」再试一次，读不到的话请先另存为 JPEG。",
            new String[]{"从文件选择", "取消"}, which -> {
                if (which == 0) {
                    pickRawFile();
                }
            });
        return false;
}
sourceBitmap = applyExifRotation(uri, sourceBitmap);
// Three ceilings in three units, and the strictest wins. The rule
// lives in ImportBudget so the decoder and the budget dialog cannot
// disagree about what this phone may hold.
final long availBytes = availableMemoryBytes();
final double k = ImportBudget.scale(sourceBitmap.getWidth(),
sourceBitmap.getHeight(), capPixels, edgeCap, availBytes);
if (k < 1.0) {
    final int nw = Math.max(1, (int) Math.round(sourceBitmap.getWidth() * k));
    final int nh = Math.max(1, (int) Math.round(sourceBitmap.getHeight() * k));
    Bitmap scaled = Bitmap.createScaledBitmap(sourceBitmap, nw, nh, true);
    if (scaled != sourceBitmap) {
        sourceBitmap.recycle();
    }
    sourceBitmap = scaled;
    try { DebugSink.text("import-scale", "k=" + k + " -> " + nw + "x" + nh
            + " avail=" + availBytes); } catch (Throwable ignored) { }
}
sourceWidth = sourceBitmap.getWidth();
sourceHeight = sourceBitmap.getHeight();
refreshEdgeFormats();
        return true;
    }

    private void loadImage(Uri uri) {
        final boolean reopening = reopeningFrame;
        renderEpoch.bump();
        sourceUri = uri;
        prepareImportState(reopening);
        try {
            // Check if this is a RAW file by extension
            String fileName = getFileName(uri);
            final String lower = fileName == null ? "" : fileName.toLowerCase(java.util.Locale.US);
            isRawFile = ImageFormat.isRaw(lower);
            if (!reopening) { live().meterMethod = isRawFile ? "protect_highlights" : ""; }
            final String unsupported = ImageFormat.unsupportedRawExtension(lower);
            if (unsupported != null) {
                showSnackbar("[E-1107] 暂不支持 " + unsupported + " 格式");
                return;
            }
            if (isRawFile) {
                loadRawImage(uri);
                return;
            }

            // **Ask before shrinking.** Importing at the negative's own size is
            // the point of a 45 MP camera; a silent downscale is the app making
            // that decision on the user's behalf, and it shows up later as an
            // export that is smaller than the file. The header gives the size
            // without decoding anything, so the question is free.
            if (askImportBudget(uri)) {
                return;
            }
            importBudgetAccepted = false;

            resetCanvasForImport();

            // Cap by area, the way the RAW path does. inSampleSize only halves,
            // so one exact scale follows if the sample still overshoots.
            final boolean nativeSize = importAtNativeSize;
            importAtNativeSize = false;                       // one import only
            // A cap the last render failure handed us (see offerSmallerImport):
            // one import's worth, then back to the device's own answer.
            final int forcedCap = pendingCapPixels;
            pendingCapPixels = 0;
            e1001Offered = false;
            if (!decodeStillSource(uri, nativeSize, forcedCap)) {
                return;
            }

            packSourcePixels();

            publishStillPreview(uri);

            openStillSession();
            // No parameter write here: a fresh session is stated by the render
            // that produces the picture (see `renderImage`), on the render
            // thread, so the UI never waits behind the engine's lock.
            lastImportError = null;
            // An import is not a look. The frame lands unrendered with no film
            // chosen; the first render happens when the user picks a stock.
            showingOriginal = true;
            lastRenderedBitmap = null;
            if (!reopening) {
                brandFilter = FilmBrand.ALL;
            }
            setOriginalVisual(true);
            finishImportRender();
        } catch (Throwable t) {
            restoringOriginal = false;
            if (pendingSession != null) {
                pendingSession = null;
                SessionDocument.file(getNoBackupFilesDir()).delete();
            }
            lastImportError = describeImportFailure(t);
            showErrorSnackbar("导入失败", lastImportError);
            try { DebugSink.text("import-failed", lastImportError); } catch (Throwable ignored) { }
        }
    }

    /**
     * The long edge accepted without asking.
     *
     * It was 4700, from the days when a dispatch past 16,776,960 threads lost
     * its top rows: the long edge was the only number that kept a 3:2 frame
     * under that wall. The wall is gone (chunked dispatches, and every plane
     * is row-banded now), so the edge no longer has to answer for the area --
     * `maxImportPixels()` does, and it is derived from this device's binding
     * range and the memory the phone has free right now.
     *
     * 12000 is where a frame stops being plausible rather than where it stops
     * working: the engine banners every pass that would bind a whole plane, so
     * the area budget (`maxImportPixels`) is the real ceiling. A 45 MP camera
     * raw stays at its own 8256 x 5504, and a 77 MP rebate canvas is exported
     * from the frame the phone actually imported.
     */
    private static final int IMPORT_LONG_EDGE = 12000;
    /** Used when the profile cannot be read at all. */
    private static final int IMPORT_PIXEL_FALLBACK = 16490040;

    private SpektraLabEngine ensureEngine() throws Exception {
        if (engine == null) {
            vkLogFile().delete();
            engine = SpektraLabEngine.create(getResourcesDir(), vkLogFile().getAbsolutePath());
            // **The probe comes first, and nothing may stop it.** It used to sit
            // after `uploadCapabilities()`, so a throw in any of the uploads left
            // `probe` at its `parse("{}")` default -- `? · 无单缓冲上限` -- and the
            // next call skipped this whole block because `engine` was already
            // set. That is a device reporting 128 MiB letting a 14.8 MP frame
            // in, and the render dying on a binding range.
            probe = DeviceProbe.parse(engine.deviceProfile());
            try { DebugSink.text("device-profile", probe.describe() + "\n" + engine.deviceProfile()); }
            catch (Throwable ignored) { }
            startCanaryProbe();
            try { uploadCapabilities(); } catch (Throwable ignored) { }
        }
        return engine;
    }

    /**
     * Pixels this device will actually render.
     *
     * Every kernel binds a whole plane, a plane is 12 bytes per pixel, and a
     * binding may not exceed the GPU's maxStorageBufferRange: 128 MiB holds
     * 11 MP, 256 MiB holds 22 MP. Importing past that does not degrade, it
     * fails -- "spk_sep_fir_acc: buffer exceeds maxStorageBufferRange" -- which
     * is what a 50 MP DNG did on a phone reporting 128 MiB. Ask the device
     * rather than guess a long edge, and keep the old workgroup cap.
     */
    private long availableMemoryBytes() {
        try {
            return memoryInfo().availMem;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Total RAM is a property of the phone; free memory moves by gigabytes. */
    private long totalMemoryBytes() {
        try {
            return memoryInfo().totalMem;
        } catch (Throwable t) {
            return 0;
        }
    }

    private android.app.ActivityManager.MemoryInfo memoryInfo() {
        final android.app.ActivityManager am =
                (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        final android.app.ActivityManager.MemoryInfo mi =
                new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi;
    }

    /**
     * Pixels of source this device will render.
     *
     * <p>The engine bands every pass that would otherwise bind a whole plane,
     * so {@link SpektraLabEngine#maxFramePixels()} returns 0 and a missing
     * binding ceiling is not a reason to fall back to the driver range. The
     * memory estimate is the only remaining ceiling; the user is asked before
     * a frame is shrunk, rather than silently refused.
     */
    private int maxImportPixels() {
        try { ensureEngine(); } catch (Throwable ignored) { }
        probeNow();
        int byNative = 0;
        try {
            byNative = engine == null ? 0 : engine.maxFramePixels();
        } catch (Throwable ignored) { }
        final long available = Math.max(availableMemoryBytes(),
                (long) (totalMemoryBytes() * 0.45));
        return ImportBudget.areaCap(byNative,
                DeviceProbe.maxPixelsByMemory(available));
    }

    /** Put a decoded RAW frame on the canvas before its first print. */
    private void applyRawPreview(Bitmap preview, Uri uri) {
        beforeBitmap = preview;
        updateHistogram(preview);
        compare.setBefore(preview);
        setStage(1, getFileName(uri));
        rememberFrame(uri);
        refreshFilmPanel();
        setStep(1);
        statusText.setText("Raw preview");
    }

    /** Mark the RAW import ready and develop it when its frame already has a film. */
    private void finishRawImport(boolean reopening) {
        if (session == null) {
            pendingSession = null;
            restoringOriginal = false;
            return;
        }
        suppressBudgetDialog = false;
        showingOriginal = true;
        lastRenderedBitmap = null;
        if (!reopening) {
            brandFilter = FilmBrand.ALL;
        }
        setOriginalVisual(true);
        finishImportRender();
    }

    /** Open the RAW session with the import caps and the current Look. */
    private SpektraLabEngine.Session openRawForImport(String filePath) {
        // `film_stock` names a profile file: "" and "null" both make
        // `load_profile` fail ("bad stock name"), so an unchosen film
        // omits the key and the engine keeps its own default. The UI
        // still shows 原图 and renders nothing until a chip is picked.
        final String filmField = live().filmStock == null ? ""
                : String.format("\"film_stock\":\"%s\",", live().filmStock);
        final String params = String.format(java.util.Locale.US,
                "{%s\"print_stock\":\"%s\",\"input_color_space\":\"ProPhoto RGB\","
                        + "\"input_cctf_decoding\":false,\"output_color_space\":\"sRGB\","
                        + "\"print_exposure\":%.2f,\"preview_long_edge\":2048}",
                filmField, live().printStock,
                optics.get(OpticsDelta.Param.EXPOSURE)
        );

        try {
            DebugSink.text("open-params", "path=" + filePath + "\n" + params);
        } catch (Throwable ignored) {
        }
        // The cap comes from the device, not from a constant: a plane is
        // 12 B/px and a binding may not exceed the GPU's
        // maxStorageBufferRange. Two limits apply, whichever is lower --
        // that range, and the 65 535-workgroup dispatch limit
        // (16,776,960 px) this driver still loses blocks over.
        final boolean nativeSize = importAtNativeSize;
        importAtNativeSize = false;
        if (nativeSize) {
            try {
                DebugSink.text("import-mode", "native size, no cap");
            } catch (Throwable ignored) {
            }
        }
        // Keep the caps so a RAW export can decode the same frame in its
        // own session.
        rawImportMaxPixels = nativeSize ? 0 : maxImportPixels();
        rawImportLongEdge = nativeSize ? 0 : IMPORT_LONG_EDGE;
        final SpektraLabEngine.Session opened = engine.openRawSession(filePath, params,
                rawImportMaxPixels, rawImportLongEdge);
        lastImportError = null;
        return opened;
    }

    /** Solve the print exposure and filter pack once on open. */
    private void solveRawSession(SpektraLabEngine.Session source) {
        try {
            final String solved = source.solve("both");
            DebugSink.text("solve", solved == null ? "(null)" : solved);
        } catch (Throwable se) {
            DebugSink.text("solve-failed", String.valueOf(se));
        }
    }

    /** Publish a RAW import failure with its user-facing error code. */
    private void reportRawImportFailure(Throwable t) {
        final String said = describeImportFailure(t);
        lastImportError = said;
        // A High-Efficiency NEF is not a preview job: name the compression
        // this build cannot decode and leave the file alone.
        runOnUiThread(() -> {
            statusText.setText("RAW加载失败 " + SpkError.shortForm(said));
            showErrorSnackbar("RAW 加载失败", said);
        });
    }

    /** Publish the decoded RAW size and offer the native-size retry. */
    private void applyRawDimensions(SpektraLabEngine.Session source, Uri uri) {
        // Get dimensions from session
        int[] dims = source.getDimensions();
        int w = dims[0];
        int h = dims[1];
        sourceWidth = w;
        sourceHeight = h;
        try { DebugSink.text("raw-dims", "w=" + w + " h=" + h); } catch (Throwable ignored) {}

        // Say when the device, not the photo, set the size. Silent
        // downscaling reads as a bug the next time the user exports.
        final int[] src = SpektraLabEngine.rawSourceSize();
        final boolean shrunk = src != null && src.length == 2
                && (long) src[0] * src[1] > (long) w * h;
        try {
            DebugSink.text("raw-cap", shrunk
                    ? "source " + src[0] + "x" + src[1] + " -> session " + w + "x" + h
                    : "not capped " + w + "x" + h);
        } catch (Throwable ignored) {}

        runOnUiThread(() -> {
            refreshEdgeFormats();
            setStep(1);
            if (shrunk && !suppressBudgetDialog) {
                // The raw has to be decoded before its size is known,
                // so this asks afterwards -- but it still asks, and
                // it offers the escape hatch the long-press used to
                // hide.
                final int rawW = src[0], rawH = src[1];
                final String budgetMessage = "原图 " + rawW + "×" + rawH + "（" + megapixels(rawW, rawH)
                                + " MP），这台机器一次能渲染约 "
                                + String.format(java.util.Locale.US, "%.1f", maxImportPixels() / 1e6)
                                + " MP，已按 " + w + "×" + h + " 导入。"
                                + "\n\n按原尺寸导入可能渲染失败（E-1001）。";
                        showActionSheet("原图超过本机预算", budgetMessage,
                                new String[]{"就这样", "按原尺寸重来"}, which -> {
                                    if (which == 1) {
                                        importAtNativeSize = true;
                                        retryingNativeSize = true;
                                        loadRawImage(uri);
                                    }
                                });
            }
            statusText.setText("RAW metadata loaded");
        });
    }

    /**
     * The decoded RAW frame in working space, packed without a transfer
     * function. A camera JPEG is not a stand-in: it is the camera's
     * rendering, not this frame.
     */
    private Bitmap decodeRawPreview(SpektraLabEngine.Session source) {
        Bitmap rawView = null;
        try {
            final int[] d = source.exportSource("live");
            if (d != null && d[0] > 0 && d[1] > 0) {
                final int rw = d[0], rh = d[1];
                final int[] px = new int[rw * rh];
                if (source.copyArgbRange(px, 0, rh) == rw * rh) {
                    rawView = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888);
                    rawView.setPixels(px, 0, rw, 0, 0, rw, rh);
                }
            }
            try {
                int probePixel = 0;
                if (rawView != null) {
                    final int[] one = new int[1];
                    rawView.getPixels(one, 0, 1, Math.max(0, rawView.getWidth() / 2),
                            Math.max(0, rawView.getHeight() / 2), 1, 1);
                    probePixel = one[0];
                }
                DebugSink.text("raw-view", rawView == null ? "export: empty"
                        : "export " + rawView.getWidth() + "x" + rawView.getHeight()
                        + " mid=" + Integer.toHexString(probePixel));
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            try {
                DebugSink.text("raw-view", "export failed: " + t);
            } catch (Throwable ignored) {
            }
        }
        return rawView;
    }

    /** Clear the previous frame state and return the RAW path, or null. */
    private String prepareRawImport(Uri uri) {
            String filePath = getFilePathFromUri(uri);
            rawFilePath = filePath;
            if (filePath == null) {
                showSnackbar("无法获取RAW文件路径");
                return null;
            }

    statusText.setText("Decoding RAW…");
    // **The same two things the JPEG path does, for the same reasons**
    // — and this path had none of them, which is why a RAW import kept
    // the previous photograph everywhere except the little source
    // thumbnail:
    //   · compare.clear() drops `CompareView.after`, the previous
    //     render, which nothing else ever clears;
    //   · the last render and the optics lock are reset below.
    if (compare != null) {
        compare.clear();
    }
    lastRenderedBitmap = null;
    setOpticsUnlocked(false);
    setStage(2, getFileName(uri));

        return filePath;
    }

    /** Run the RAW decode, solve and preview off the UI thread. */
    private void startRawImportThread(String filePath, String importParams, boolean reopening, Uri uri) {
        new Thread(() -> importRawOnWorker(filePath, importParams, reopening, uri)).start();
    }

    /** The worker half of a RAW import. */
    private void importRawOnWorker(String filePath, String importParams, boolean reopening, Uri uri) {
        try {
            ensureEngine();
            // Under the render lock: closing a session out from under
            // a render is a native crash, not an exception.
            synchronized (renderMutex) {
                if (session != null) { session.close(); session = null; }
            }

            session = openRawForImport(filePath);
            // **The session's own parameters, which this path never sent.**
            // `openRawSession` carries the film, the paper and the decode
            // colour space; it does not carry grain, halation, glare, the
            // format or the rebate. The Look was snapshotted on the UI
            // thread, so `solve` below meters with the same values the
            // first preview will render.
            if (importParams != null) {
                session.setParams(importParams);
            }

            // The raw itself is the honest "before" side; see decodeRawPreview.
            final Bitmap preview = decodeRawPreview(session);
            final boolean hasPreview = preview != null;
            if (!hasPreview) {
                // The canvas is not allowed to be blank for no reason.
                // A session that opened but produced no source frame is
                // a bug, and it has to say so rather than look like a
                // photo that failed to load.
                try { DebugSink.text("raw-view", "no source frame for the canvas"); }
                catch (Throwable ignored) { }
                runOnUiThread(() -> statusText.setText("RAW 已载入，原图预览未生成"));
            }
            if (hasPreview) {
                runOnUiThread(() -> applyRawPreview(preview, uri));
            }

            // "Process" (配光): without a solved print exposure and
            // filter pack the print runs on defaults and blows the
            // highlights. The mac app makes the user press Process;
            // this build does it once on open.
            solveRawSession(session);
            runOnUiThread(() -> finishRawImport(reopening));

            applyRawDimensions(session, uri);
        } catch (Throwable t) {
            reportRawImportFailure(t);
        }
    }

    private void loadRawImage(Uri uri) {
        final boolean reopening = reopeningFrame;
        renderEpoch.bump();
        // The Look the RAW session must carry, snapshotted here on the UI
        // thread: `solve` below meters with it, and the worker never reads a
        // control.
        final String importParams = live().filmStock == null
                ? null
                : snapshotFor(live(), live().filmStock, PREVIEW_LONG_EDGE, false).params;
        try {
            final String filePath = prepareRawImport(uri);
            if (filePath == null) {
                return;
            }
            startRawImportThread(filePath, importParams, reopening, uri);
        } catch (Throwable t) {
            lastImportError = describeImportFailure(t);
            showErrorSnackbar("RAW 加载失败", lastImportError);
        }
    }

    /**
     * A failure the user has to be able to read and copy.
     *
     * <p>A Toast is two lines on most phones and ends in an ellipsis, which is
     * exactly how the E-9000 detail disappeared for two releases: the message
     * carried the whole dlopen chain and the screen showed "……so …". This one
     * scrolls, can be selected, and copies itself in one tap -- an error the
     * user cannot forward is an error nobody can fix.
     */
    /** The error card header and close link. */
    private void addErrorHeader(LinearLayout card, android.app.Dialog dialog) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), 0, dp(16), 0);
        TextView kicker = type.mixedM3Label("ERROR / 错误详情",
                M3Tokens.TypeRole.LABEL_LARGE, theme.primary());
        head.addView(kicker, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView close = type.m3Label("关闭", M3Tokens.TypeRole.LABEL_LARGE,
                theme.onSurfaceVariant(), tfHan);
        close.setPaintFlags(close.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        close.setPadding(dp(12), dp(14), 0, dp(14));
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        card.addView(head, new LinearLayout.LayoutParams(-1, dp(42)));
    }

    /** The scrollable error detail. */
    private void addErrorBody(LinearLayout card, String shownTitle, String shownBody) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout bodyBox = new LinearLayout(this);
        bodyBox.setOrientation(LinearLayout.VERTICAL);
        bodyBox.setPadding(dp(16), 0, dp(16), 4);
        TextView titleView = type.m3Label(shownTitle, M3Tokens.TypeRole.TITLE_LARGE,
                theme.onSurface(), tfHan);
        bodyBox.addView(titleView);
        TextView advice = type.m3Label("请复制下面的完整信息，方便定位问题。",
                M3Tokens.TypeRole.BODY_MEDIUM, theme.onSurfaceVariant(), tfHan);
        LinearLayout.LayoutParams adviceLp = new LinearLayout.LayoutParams(-1, -2);
        adviceLp.topMargin = dp(8);
        bodyBox.addView(advice, adviceLp);
        TextView detail = type.m3Label(shownBody, M3Tokens.TypeRole.BODY_SMALL,
                theme.onSurface(), tfHan);
        detail.setTextIsSelectable(true);
        detail.setPadding(dp(12), dp(11), dp(12), dp(11));
        android.graphics.drawable.GradientDrawable detailBg = new android.graphics.drawable.GradientDrawable();
        detailBg.setColor(theme.role(
                com.google.android.material.R.attr.colorSurfaceContainerLowest,
                Palette.Role.SURFACE_SUNKEN));
        detail.setBackground(detailBg);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
        detailLp.topMargin = dp(14);
        bodyBox.addView(detail, detailLp);
        scroll.addView(bodyBox, new android.widget.FrameLayout.LayoutParams(-1, -2));
        int bodyHeight = (int) Math.min(dp(340), getResources().getDisplayMetrics().heightPixels * 0.44f);
        card.addView(scroll, new LinearLayout.LayoutParams(-1, bodyHeight));
    }

    /** The close and copy actions. */
    private void addErrorActions(LinearLayout card, android.app.Dialog dialog,
                                         String shownTitle, String shownBody) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(16), dp(14), dp(16), dp(16));
        TextView closeAction = type.m3Label("关闭", M3Tokens.TypeRole.LABEL_LARGE,
                theme.primary(), tfHan);
        closeAction.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
        closeBg.setColor(theme.role(
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Palette.Role.SURFACE_ELEVATED));
        closeBg.setStroke(dp(1), theme.primary());
        closeAction.setBackground(closeBg);
        closeAction.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        closeLp.rightMargin = dp(8);
        actions.addView(closeAction, closeLp);
        TextView copy = type.m3Label("复制错误信息", M3Tokens.TypeRole.LABEL_LARGE,
                theme.role(com.google.android.material.R.attr.colorOnPrimary,
                        Palette.Role.ON_ACCENT), tfHan);
        copy.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable copyBg = new android.graphics.drawable.GradientDrawable();
        copyBg.setColor(theme.primary());
        copy.setBackground(copyBg);
        copy.setOnClickListener(v -> {
            try {
                final String text = shownTitle + "\n\n" + shownBody
                        + "\n\nbuild=SpektraLab Android " + UpdateCheck.versionName(this)
                        + "\n" + probe.describe();
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(
                                android.content.Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("SpektraLab", text));
                copy.setText("已复制");
            } catch (Throwable t) {
                copy.setText("复制失败 · 请长按详情");
            }
        });
        actions.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1.45f));
        card.addView(actions, new LinearLayout.LayoutParams(-1, -2));
    }

    /** Show the error card with the shared dialog window shape. */
    private void showErrorCardWindow(android.app.Dialog dialog, LinearLayout card) {
        dialog.setContentView(card);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            int width = Math.min(dp(358),
                    getResources().getDisplayMetrics().widthPixels - dp(32));
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.30f);
        }
    }

    private void showErrorDialog(String title, String body) {
        final String shownTitle = (title == null || title.isEmpty()) ? "错误" : title;
        final String shownBody = (body == null || body.isEmpty()) ? "[E-0000] 未知错误" : body;
        runOnUiThread(() -> {
            final android.app.Dialog dialog = new android.app.Dialog(this);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(theme.role(
                    com.google.android.material.R.attr.colorSurfaceContainerHigh,
                    Palette.Role.SURFACE_ELEVATED));
            View cap = new View(this);
            cap.setBackgroundColor(theme.role(com.google.android.material.R.attr.colorError,
                    Palette.Role.ACCENT));
            card.addView(cap, new LinearLayout.LayoutParams(-1, dp(3)));

            addErrorHeader(card, dialog);
            addErrorBody(card, shownTitle, shownBody);
            addErrorActions(card, dialog, shownTitle, shownBody);
            showErrorCardWindow(dialog, card);
        });
    }

    private int extractInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) {
            return 0;
        }
        idx += key.length();
        int end = json.indexOf(",", idx);
        if (end < 0) {
            end = json.indexOf("}", idx);
        }
        if (end < 0) {
            return 0;
        }
        try {
            return Integer.parseInt(json.substring(idx, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        result = cursor.getString(idx);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFilePathFromUri(Uri uri) {
        // For content:// URIs, we need to copy to a temp file
        if (uri.getScheme().equals("content")) {
            try {
                String fileName = SafeFileName.of(getFileName(uri), "import.bin");
                java.io.File tempFile = new java.io.File(getCacheDir(), fileName);
                try (InputStream in = getContentResolver().openInputStream(uri);
                     java.io.OutputStream out = new java.io.FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                return tempFile.getAbsolutePath();
            } catch (Exception e) {
                return null;
            }
        } else if (uri.getScheme().equals("file")) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * The EXIF orientation, or -1 when the provider will not give it to us.
     *
     * Two attempts on purpose: a stream first, then the file descriptor. Some
     * pickers and cloud providers hand out a one-shot stream, and the second
     * openInputStream() then fails -- the old code swallowed that and returned
     * the unrotated bitmap, which is exactly how a portrait frame arrives
     * sideways "at random".
     */
    /** The TIFF/EXIF orientation of a file on disk (raw containers included). */
    private int orientationOfPath(String path) {
        int fromPlatform = -1;
        try {
            fromPlatform = new androidx.exifinterface.media.ExifInterface(path).getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
        } catch (Throwable t) {
            try { DebugSink.text("exif", "path: " + t); } catch (Throwable ignored) { }
        }
        if (fromPlatform > 0) {
            return fromPlatform;
        }
        // ExifInterface does not open every raw container: RW2 writes 0x55
        // where TIFF writes 42, and the platform reader refuses it outright on
        // some builds. IFD0's tag 0x0112 is four bytes away from a header we
        // can read ourselves.
        return tiffOrientation(path);
    }

    /** TAG_ORIENTATION out of a TIFF-style container, magic 42 or Panasonic's 0x55. */
    private int tiffOrientation(String path) {
        java.io.RandomAccessFile file = null;
        try {
            file = new java.io.RandomAccessFile(path, "r");
            if (file.length() < 16) {
                return -1;
            }
            final byte[] head = new byte[8];
            file.readFully(head);
            final boolean little = head[0] == 0x49 && head[1] == 0x49;
            final int magic = shortAt(head, 2, little);
            if (magic != 42 && magic != 0x55) {
                return -1;
            }
            final long ifd = longAt(head, 4, little);
            if (ifd <= 0 || ifd > file.length() - 2) {
                return -1;
            }
            file.seek(ifd);
            final byte[] count = new byte[2];
            file.readFully(count);
            final int entries = shortAt(count, 0, little);
            if (entries <= 0 || entries > 512) {
                return -1;
            }
            final byte[] table = new byte[entries * 12];
            file.readFully(table);
            for (int i = 0; i < entries; ++i) {
                final int base = i * 12;
                if (shortAt(table, base, little) != 0x0112) {
                    continue;  // Orientation
                }
                if (shortAt(table, base + 2, little) != 3) {
                    continue;  // SHORT
                }
                return shortAt(table, base + 8, little);
            }
        } catch (Throwable t) {
            return -1;
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return -1;
    }

    private static int shortAt(byte[] b, int at, boolean little) {
        final int a = b[at] & 0xFF, c = b[at + 1] & 0xFF;
        return little ? (c << 8 | a) : (a << 8 | c);
    }

    private static long longAt(byte[] b, int at, boolean little) {
        long v = 0;
        for (int i = 0; i < 4; ++i) {
            final int idx = little ? at + 3 - i : at + i;
            v = (v << 8) | (b[idx] & 0xFF);
        }
        return v;
    }

    private int readOrientation(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in != null) {
                return new androidx.exifinterface.media.ExifInterface(in).getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Throwable ignored) { }
        try (android.os.ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r")) {
            if (fd != null) {
                return new androidx.exifinterface.media.ExifInterface(fd.getFileDescriptor()).getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Throwable ignored) { }
        return -1;
    }

    /**
     * The image, decoded. `BitmapFactory` first -- the path every JPEG and PNG
     * has always taken -- and `ImageDecoder` when that comes back empty.
     *
     * The second attempt is for HEIC/HEIF. Samsung and iPhone save 10-bit HEIF,
     * `BitmapFactory.decodeStream` returns null on those (and reports no bounds
     * at all for the sniff), while the platform decoder behind `ImageDecoder`
     * (API 28+) hands back a software bitmap. The old code said "无法解码图片"
     * and stopped -- which a Samsung user read, correctly, as "this app cannot
     * import my photos".
     */
    private Bitmap decodeImport(Uri uri, int capPixels, int edgeCap) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream head = getContentResolver().openInputStream(uri)) {
                if (head != null) {
                    BitmapFactory.decodeStream(head, null, bounds);
                }
            } catch (Throwable t) {
                try { DebugSink.text("decode", "sniff " + t); } catch (Throwable ignored) { }
            }
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                int sample = 1;
                while ((long) (bounds.outWidth / (sample * 2)) * (bounds.outHeight / (sample * 2)) >= capPixels
                        || Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= edgeCap) {
                    sample *= 2;
                }
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = sample;
                try (InputStream stream = getContentResolver().openInputStream(uri)) {
                    Bitmap decoded = BitmapFactory.decodeStream(stream, null, opts);
                    if (decoded != null) {
                        return decoded;
                    }
                    try { DebugSink.text("decode", "null at sample " + sample); }
                    catch (Throwable ignored) { }
                } catch (Throwable t) {
                    try { DebugSink.text("decode", "stream " + t); } catch (Throwable ignored) { }
                }
            } else {
                try { DebugSink.text("decode", "no bounds " + bounds.outWidth + "x" + bounds.outHeight); }
                catch (Throwable ignored) { }
            }
        } catch (Throwable t) {
            try { DebugSink.text("decode", "outer " + t); } catch (Throwable ignored) { }
        }
        return decodeViaImageDecoder(uri, capPixels, edgeCap);
    }

    /**
     * The platform decoder through its newer door, scaled in the decoder so the
     * full-size bitmap never exists. **Software allocator on purpose**: a
     * hardware bitmap cannot be read back with `getPixels`, and the import path
     * reads every pixel into a float buffer.
     */
    private Bitmap decodeViaImageDecoder(Uri uri, final int capPixels, final int edgeCap) {
        if (android.os.Build.VERSION.SDK_INT < 28) {
            return null;
        }
        try {
            final android.graphics.ImageDecoder.Source src =
                    android.graphics.ImageDecoder.createSource(getContentResolver(), uri);
            Bitmap decoded = android.graphics.ImageDecoder.decodeBitmap(src,
                    (decoder, info, source) -> {
                        final android.util.Size size = info.getSize();
                        final int w = size.getWidth(), h = size.getHeight();
                        final double k = ImportBudget.scale(w, h, capPixels, edgeCap, 0);
                        if (k < 1.0) {
                            decoder.setTargetSize(Math.max(1, (int) Math.round(w * k)),
                                                  Math.max(1, (int) Math.round(h * k)));
                        }
                        decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
            try { DebugSink.text("decode", "imagedecoder " + decoded.getWidth() + "x"
                    + decoded.getHeight()); } catch (Throwable ignored) { }
            return decoded;
        } catch (Throwable t) {
            try { DebugSink.text("decode", "imagedecoder " + t); } catch (Throwable ignored) { }
            return null;
        }
    }

    /**
     * A failure the user can act on. `OutOfMemoryError` is an `Error`, not an
     * `Exception`, which is why the `catch (Exception)` this replaces let the
     * process die on the Honor report instead of saying 内存不足.
     */
    private String describeImportFailure(Throwable t) {
        try { DebugSink.text("import-error", String.valueOf(t)); } catch (Throwable ignored) { }
        if (t instanceof OutOfMemoryError) {
            return "[E-1003] 内存不足：这张图对本机太大。换一张小一点的，或先关掉其它应用再试";
        }
        return SpkError.format(describe(t));
    }

    private Bitmap applyExifRotation(Uri uri, Bitmap bitmap) {
        final int orientation = readOrientation(uri);
        try { DebugSink.text("exif", "orientation=" + orientation + " bitmap="
                + bitmap.getWidth() + "x" + bitmap.getHeight()); } catch (Throwable ignored) { }
        return rotateForOrientation(bitmap, orientation);
    }

    /** The orientation tag as a matrix. 0 or negative means "unreadable". */
    private Bitmap rotateForOrientation(Bitmap bitmap, int orientation) {
        if (orientation <= 0) {
            return bitmap;  // unreadable: leave the pixels alone
        }
        try {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            switch (orientation) {
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90); break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180); break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270); break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1); break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1, -1); break;
                // The two mirrored rotations. Rare, but a phone that writes 5
                // or 7 and lands in the default branch shows its photo sideways.
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.postRotate(90); matrix.postScale(-1, 1); break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.postRotate(-90); matrix.postScale(-1, 1); break;
                default:
                    return bitmap;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            return bitmap;
        }
    }


    /**
     * A stock change only re-renders. Measured against fresh sessions on this
     * engine, the stored print solution already gives the same picture after a
     * film switch (mean 64.4/58.9/55.7 either way) and a paper switch only moves
     * the paper's own speed, which the enlarger-exposure slider is for. Calling
     * `spk_solve` here re-metered a cached full-size negative and never returned,
     * which is what left the status bar stuck on "配光中".
     */
    private void onStockChanged() {
        if (session == null || !uiReady) {
            return;
        }
        if (live().filmStock == null) {
            // No film yet: the frame stays the import's own, and the sheet's
            // choice is remembered for the first render.
            showSnackbar("选择一张胶片开始渲染");
            return;
        }
        // No UI-thread parameter write here any more: writing the engine from
        // the UI could park it behind a long render's lock. `renderImage`
        // snapshots the Look on the UI thread and the render thread sends it
        // under the same lock it renders under -- one writer, one order.
        showingOriginal = false;
        setOriginalVisual(false);
        refreshFilmPanel();
        renderImage();
    }


    /**
     * The Look re-statement builder preview and export both call.
     *
     * <p>It snapshots the live controls into a {@link Look} **on the calling
     * thread** and hands that detached copy to {@link LookRestatement}, so every
     * value the UI shows -- the Film stock, the Print stock, all 22 optical
     * parameters, the camera filter, the protection norm, and every Rebate and
     * Date back field -- is in the delta. The render thread only ever sends a
     * string that was built here; it never reads a control or a Frame.
     *
     * <p>The only facts added here are the ones a Look does not own: the source
     * kind, the render's long edge, whether the stock is a Reversal stock, its
     * edge print, and the Date back's EXIF default.
     *
     * <p>The old inline format string lived only in the preview path and the
     * export re-called it with the preview's stock; a parameter that reached
     * one and not the other was invisible until a file disagreed with the
     * screen. There is now one string, built in one place.
     */
    private RenderSnapshot snapshotFor(Look look, String stock, int longEdge,
                                       boolean forExport) {
        return RenderSnapshot.of(look, stock, longEdge,
                new LookRestatement.Context(
                        isRawFile,
                        longEdge,
                        isPositiveFilm(stock),
                        edgeTextFor(stock),
                        look.dateOn ? defaultImprintDate() : "",
                        forExport ? "sRGB" : previewSpace));
    }

    /*
     * The file facts in that context, and why each is what it is:
     *
     * `auto_exposure` is off for everything that is not a RAW. The engine's
     * default meter is `center_weighted`: an arithmetic mean of luminance
     * under a Gaussian centred on the frame. An arithmetic mean is dominated
     * by whatever is brightest, so a sky-lit frame meters about three times
     * mid-grey and prints dark. A camera JPEG has already been metered once,
     * by the camera; metering it again is double-counting. A RAW is
     * scene-referred and keeps the meter.
     */

    /**
     * A control moved. Ask the live thread for one small frame.
     *
     * <p>Requests coalesce: the thread draws at most one frame per
     * {@link #LIVE_MIN_GAP_MS} and always uses the newest snapshot. Nothing is
     * recorded, persisted, or sent to the engine on this call -- the UI thread
     * only takes the Look and its parameters.
     */
    private void requestLivePreview() {
        if (liveStopped || isFinishing() || session == null || live().filmStock == null) {
            return;
        }
        // A full render owns the engine until it lands; its result is about to
        // replace whatever a live frame could draw now.
        if (rendering) {
            return;
        }
        if (showingOriginal) {
            showingOriginal = false;
            setOriginalVisual(false);
        }
        try {
            liveSnapshot = snapshotFor(captureLook(), live().filmStock, LIVE_LONG_EDGE, false);
        } catch (Throwable t) {
            try { DebugSink.text("live", "snapshot failed " + describe(t)); } catch (Throwable ignored) { }
            return;
        }
        liveGate.request();
        liveWanted = true;
        ensureLiveThread();
        synchronized (liveLock) {
            liveLock.notifyAll();
        }
    }

    private void ensureLiveThread() {
        if (liveThread != null) {
            return;
        }
        liveThread = new Thread(this::liveLoop, "live-preview");
        liveThread.setDaemon(true);
        liveThread.start();
    }

    /** Drop a live frame that has not painted yet, and every frame in flight. */
    private void cancelLivePreview() {
        liveGate.cancel();
        liveWanted = false;
        liveSnapshot = null;
    }

    /** Stop the daemon before the session is torn down. */
    private void stopLivePreview() {
        liveStopped = true;
        liveWanted = false;
        synchronized (liveLock) {
            liveLock.notifyAll();
        }
        if (liveThread != null) {
            liveThread.interrupt();
            liveThread = null;
        }
    }

    private void liveLoop() {
        while (!liveStopped) {
            synchronized (liveLock) {
                while (!liveWanted && !liveStopped) {
                    try { liveLock.wait(); } catch (InterruptedException e) { return; }
                }
            }
            if (liveStopped) {
                return;
            }
            // Let the gesture settle for a moment: requests that arrive during
            // the sleep fold into the frame that follows.
            final long since = System.currentTimeMillis() - liveLastStart;
            if (since < LIVE_MIN_GAP_MS) {
                try {
                    Thread.sleep(LIVE_MIN_GAP_MS - since);
                } catch (InterruptedException e) {
                    return;
                }
            }
            liveWanted = false;
            if (!liveGate.consume()) {
                continue;
            }
            final RenderSnapshot snapshot = liveSnapshot;
            if (snapshot == null || rendering || session == null) {
                continue;
            }
            liveLastStart = System.currentTimeMillis();
            drawLivePreview(snapshot, liveGate.generation(), liveGate.fullGeneration(),
                    currentFrame);
        }
    }

    /**
     * One reprint at {@link #LIVE_LONG_EDGE}. The snapshot was taken on the UI
     * thread; the worker neither reads a control nor writes the Look.
     */
    private void drawLivePreview(RenderSnapshot snapshot, int generation,
                                 int fullGeneration, int frame) {
        final Bitmap bmp = renderLiveFrame(snapshot, generation, fullGeneration);
        if (bmp == null) {
            return;
        }
        runOnUiThread(() -> {
            if (!uiReady || liveStopped || rendering || session == null
                    || compare == null
                    || frame != currentFrame
                    || !liveGate.accepts(generation, fullGeneration)) {
                return;
            }
            lastRenderedBitmap = bmp;
            tagPreviewSpace(bmp);
            updateHistogram(bmp);
            compare.setAfter(bmp);
            setStage(1, null);
        });
    }

    /** The native half of a live frame; null when the request is already stale. */
    private Bitmap renderLiveFrame(RenderSnapshot snapshot, int generation,
                                  int fullGeneration) {
        synchronized (renderMutex) {
            if (session == null || !liveGate.accepts(generation, fullGeneration)) {
                return null;
            }
            try {
                SpektraLabEngine.setViewAdjust(snapshot.viewAdjust);
                session.setParams(snapshot.params);
                final int[] dims = session.renderBegin("live", true);
                final int w = dims[0], h = dims[1];
                if (w <= 0 || h <= 0) {
                    throw new RuntimeException("empty live frame");
                }
                if (livePixels == null || livePixels.length < w * h) {
                    livePixels = new int[w * h];
                }
                final int copied = session.copyArgbRange(livePixels, 0, h);
                if (copied != w * h) {
                    throw new RuntimeException("live copy " + copied + "/" + (w * h));
                }
                final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bmp.setPixels(livePixels, 0, w, 0, 0, w, h);
                session.renderEnd();
                try { DebugSink.text("live", "frame " + w + "x" + h); }
                catch (Throwable ignored) { }
                return bmp;
            } catch (Throwable t) {
                try { session.renderEnd(); } catch (Throwable ignored) { }
                try { DebugSink.text("live", "fail " + describe(t)); }
                catch (Throwable ignored) { }
                return null;
            }
        }
    }

    private void renderImage() {
        // The full pass owns the canvas from here: a live frame still in flight
        // must not land on top of the result the user waited for.
        cancelLivePreview();
        liveGate.fullRenderStarted();
        // Every branch says something. The foot is one thin line, too quiet to
        // carry "nothing happened"; failures need to be readable.
        if (session == null) {
            try { DebugSink.text("render", "no session, lastImportError=" + lastImportError); }
            catch (Throwable ignored) { }
            showErrorSnackbar("还没有可渲染的图像", lastImportError == null
                    ? "先导入一张照片。点画布中央的「选择照片」，或点下方「换图」；RAW / HEIC 请选「从文件管理器选择」。"
                    : "上次导入没有完成：\n\n" + lastImportError);
            return;
        }
        if (live().filmStock == null) {
            showSnackbar("先在下方选一张胶片");
            return;
        }
        if (rendering) {
            // One native result buffer for the whole process: two renders would
            // interleave on it and hand back another frame's dimensions.
            try { DebugSink.text("render", "already rendering"); } catch (Throwable ignored) { }
            statusText.setText("Rendering…");
            // Not dropped: a tap that lands mid-render is the *last* choice the
            // user made, so it becomes the next render instead of a toast.
            pendingRender = true;
            showSnackbar("正在渲染，完成后按新选择再画一次…");
            return;
        }
        final String renderedStock = live().filmStock;
        // A settled edit is whatever Look the next render will use. Recording
        // here catches every control path (slider release, stock, switch, reset)
        // without each one having to remember undo.
        final Look renderLook = captureLook();
        // A settled edit is whatever Look the next render will use. Recording
        // here catches every control path (slider release, stock, switch, reset)
        // without each one having to remember undo. The session owns the state
        // and the history; this only decides when an edit is settled.
        if (!restoringUndo && currentSession != null) {
            currentSession.record();
        }
        updateUndoAction();
        // The view layer is a process-global native pass, so snapshot its
        // values with the Look and send them under the render lock.
        final RenderSnapshot snapshot = snapshotFor(renderLook, renderedStock, PREVIEW_LONG_EDGE, false);
        final double[] viewAdjust = snapshot.viewAdjust;
        // Snapshot the whole Look here, on the UI thread: the render thread gets
        // one finished string and never reads a control or a Frame.
        final String previewParams;
        try {
            previewParams = snapshot.params;
            // The line a "every film looks the same" report needs: what this
            // render is about to tell the engine, next to the UI selection.
            DebugSink.text("params", "draw film=" + renderedStock + " print=" + live().printStock
                    + " render=" + rendering + " long=" + PREVIEW_LONG_EDGE);
        } catch (Throwable t) {
            showErrorSnackbar("渲染参数有误", describe(t));
            return;
        }
        activeRenderEpoch = renderEpoch.current();
        activeRenderFrame = currentFrame;
        rendering = true;
        progressBar.setVisibility(View.VISIBLE);
        busyLabel.setVisibility(View.VISIBLE);
        statusText.setText("Rendering…");
        try { DebugSink.text("render", "begin " + sourceWidth + "x" + sourceHeight); }
        catch (Throwable ignored) { }

        startPreviewRender(viewAdjust, previewParams);
    }

    /** The worker half of a preview render. */
    private void renderPreviewOnWorker(double[] viewAdjust, String previewParams) {
        synchronized (renderMutex) {
            if (session == null) {
                throw new RuntimeException("session closed before render");
            }
            // The finished Look snapshot, taken on the UI thread before
            // this thread started. Under the render lock it is one writer.
            SpektraLabEngine.setViewAdjust(viewAdjust);
            session.setParams(previewParams);
            int[] dims = session.renderBegin("live");
            int w = dims[0], h = dims[1];
            if (w <= 0 || h <= 0) {
                throw new RuntimeException("engine returned empty frame");
            }

            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            int[] pixels = new int[w * h];
            int copied = session.copyArgbRange(pixels, 0, h);
            if (copied != w * h) {
                throw new RuntimeException("pixel copy incomplete " + copied + "/" + (w * h));
            }
            bmp.setPixels(pixels, 0, w, 0, 0, w, h);
            session.renderEnd();
            // What the meter did to *this* frame. A render that looks dark
            // is either the film's curve or an exposure the engine decided
            // on, and the two are indistinguishable by eye -- one line of
            // numbers settles it.
            try {
                final String pj = session.progressJson();
                final int at = pj == null ? -1 : pj.indexOf("\"auto_exposure_ev\"");
                DebugSink.text("exposure", "tier=live " + (at < 0 ? "(no progress json)"
                        : pj.substring(at, Math.min(pj.length(), at + 40))));
            } catch (Throwable ignored) { }
            // The rebate's own numbers, next to the buffer they belong to.
            // "the edge is wrong in the export but right in the preview" is
            // a question only these two lines can answer.
            try {
                DebugSink.text("overscan-geom", "tier=live " + w + "x" + h + " "
                        + session.overscanGeometry());
            } catch (Throwable ignored) { }
            // The engine's log for this render. When a frame comes back as
            // one flat colour there is nothing else to read: it says which
            // tier was built, how big, and what each dispatch covered.
            uploadVkLog();

            final int fw = w, fh = h;
            try {
                long hsh = 1469598103934665603L;
                for (int i = 0; i < pixels.length; i += 7) { hsh ^= pixels[i]; hsh *= 1099511628211L; }
                long t0 = System.currentTimeMillis();
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, bo);
                DebugSink.text("preview", "w=" + w + " h=" + h + " hash=" + Long.toHexString(hsh)
                        + " copy=" + copied + " first=" + Integer.toHexString(pixels[0])
                        + " mid=" + Integer.toHexString(pixels[(h / 2) * w + w / 2])
                        + " jpegBytes=" + bo.size());
                DebugSink.bytes("preview", "p.jpg", bo.toByteArray(), "image/jpeg");
                DebugSink.text("preview-timing", "jpeg_ms=" + (System.currentTimeMillis() - t0));
            } catch (Throwable ignored) {}
            runOnUiThread(() -> applyPreviewResult(bmp, fw, fh));
        }
    }

    /** Render failure reporting and the oversize retry. */
    private void handlePreviewFailure(Throwable e) {
        try { session.renderEnd(); } catch (Throwable ignored) {}
        final String msg = describe(e);
        runOnUiThread(() -> applyRenderFailure(msg));
        // E-1001 is "a plane is bigger than one binding may name", and
        // the only cure is a smaller session. The import cap already
        // answers for a normal import, so reaching here means the frame
        // came in past it -- a native-size import, or a build that had
        // no cap. Offering the retry beats printing a code and leaving
        // the user to find the long-press menu.
        // 1001 is the binding range by name; 1009 is a Vulkan call that
        // failed without naming one, which on a frame this size is the
        // same ceiling. Both have the same cure.
        final int code = SpkError.codeOf(msg);
        if (code == 1001 || code == 1009) {
            offerSmallerImport();
        }
    }

    private void startPreviewRender(double[] viewAdjust, String previewParams) {
        new Thread(() -> {
            try {
                renderPreviewOnWorker(viewAdjust, previewParams);
            } catch (Throwable e) {
                handlePreviewFailure(e);
            }
        }).start();
    }

    /** Take the RENDERING plate off the canvas. Safe to call twice. */
    private void clearBusyPlate() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (busyLabel != null) {
            busyLabel.setVisibility(View.GONE);
        }
    }

    /** Show a finished preview and clear the busy state. */
    private void applyPreviewResult(Bitmap bmp, int fw, int fh) {
        if (!renderEpoch.isCurrent(activeRenderEpoch) || currentFrame != activeRenderFrame
                || session == null) {
            try { DebugSink.text("render", "frame changed mid-render; dropped"); }
            catch (Throwable ignored) { }
            rendering = false;
            clearBusyPlate();
            if (pendingRender) {
                pendingRender = false;
                if (live().filmStock != null && session != null) {
                    renderImage();
                }
            }
            return;
        }
        showingOriginal = false;
        compare.setAfter(bmp);
        setStage(1, null);
        setStep(2);
        lastRenderedBitmap = bmp;
        tagPreviewSpace(bmp);
        updateHistogram(bmp);
        setOpticsUnlocked(true);
        setOriginalVisual(false);
        refreshFilmPanel();
        final int[] bands = blackBands(bmp);
        statusText.setText("Preview ready " + fw + "×" + fh);
        showSnackbar("预览已更新 " + fw + "×" + fh, Snackbar.LENGTH_SHORT);
        try {
            DebugSink.text("bands", "render " + fw + "x" + fh
                    + " black rows top=" + bands[0] + " bottom=" + bands[1]
                    + " source=" + sourceWidth + "x" + sourceHeight);
        } catch (Throwable ignored) {
        }
        progressBar.setVisibility(View.GONE);
        busyLabel.setVisibility(View.GONE);
        rendering = false;
        if (pendingRender) {
            pendingRender = false;
            renderImage();
        }
    }

    /** Show a failed render with the size and probe context needed to report it. */
    private void applyRenderFailure(String msg) {
        statusText.setText("渲染失败 " + SpkError.shortForm(msg));
        progressBar.setVisibility(View.GONE);
        busyLabel.setVisibility(View.GONE);
        rendering = false;
        // The foot is one 7 sp line: a failure has to be readable, and it has
        // to arrive with enough context to be reported.
        showErrorSnackbar("渲染失败", SpkError.format(msg)
                + "\n\n" + sourceWidth + "×" + sourceHeight
                + "\n" + probe.describe());
        if (pendingRender) {
            pendingRender = false;
            renderImage();
        }
    }

    /**
     * Re-import at a size this device can actually bind, then render again.
     * One offer per import: a loop of dialogs is worse than the error.
     */
    private void offerSmallerImport() {
        if (sourceUri == null || e1001Offered) {
            return;
        }
        e1001Offered = true;
        // The same ceiling the import uses, at 75 % of it. When the probe had
        // failed this used to fall back to the dispatch cap -- 16.5 MP -- which
        // on a 128 MiB device is still three times too big, so the retry failed
        // the same way and the offer was a dead end.
        long target = maxImportPixels();
        target = (long) (target * 0.75);
        target = Math.max(1_000_000L, Math.min(target, 100_000_000L));
        final long picking = target;
        runOnUiThread(() -> showActionSheet("这张图超出本机的单缓冲上限",
                "按约 " + String.format(java.util.Locale.US, "%.1f", picking / 1e6)
                        + " MP 重新导入并渲染？画面会小一些，但能出图。",
                new String[]{"重试", "取消"}, which -> {
                    if (which == 0) {
                        pendingCapPixels = (int) picking;
                        loadImage(sourceUri);
                    }
                }));
    }

    private void exportImage() {
        exportImage("jpeg", "preview");
    }

    /**
     * A native crash kills the process before the export path can report, so
     * any *.prog the JNI left in the cache dir is picked up on the next start.
     */
    /** Show a finished export and clear the progress state. */
    private void applyExportResult(Uri outUri, String mime, String canvas, int fw, int fh,
                                   long renderMs, long secs, String sizeLabel,
                                   boolean tiff, boolean dng) {
        lastExportUri = outUri;
        lastExportMime = mime;
        // A separate control, and only when it has something to do: every
        // other format opens where it lands.
        if (sharePill != null) {
            sharePill.setVisibility((tiff || dng) ? View.VISIBLE : View.GONE);
        }
        final String canvasNote = canvas.isEmpty() ? "" : " · 画布 " + canvas;
        final String tiffHint = tiff
                ? "（TIFF：点导出旁边的「分享」发给手机版 Lightroom 即可使用）"
                : dng ? "（DNG：线性 16 位，给电脑上的 Lightroom / ACR；手机版打不开，已知问题，不必反馈）" : "";
        showSnackbar("已保存 " + fw + "x" + fh + canvasNote + " · " + sizeLabel + tiffHint);
        statusText.setText("已导出 " + fw + "x" + fh + canvasNote + " · " + sizeLabel
                + "（" + secs + " 秒，其中渲染 " + (renderMs / 1000.0) + " 秒）" + tiffHint);
        exportProgressBar.setVisibility(View.GONE);
        setStep(2);
    }

    /** Show an export failure with the size and probe context. */
    private void applyExportFailure(String msg) {
        statusText.setText("导出失败 " + SpkError.shortForm(msg));
        showErrorSnackbar("导出失败", SpkError.format(msg)
                + "\n\n" + sourceWidth + "×" + sourceHeight
                + "\n" + probe.describe());
        exportProgressBar.setVisibility(View.GONE);
        setStep(2);
    }

    private void uploadStaleExportProgress() {
        try {
            java.io.File[] files = getCacheDir().listFiles();
            if (files == null) {
                return;
            }
            for (java.io.File f : files) {
                if (!f.getName().endsWith(".prog")) {
                    continue;
                }
                byte[] buf = new byte[(int) f.length()];
                try (java.io.InputStream in = new java.io.FileInputStream(f)) {
                    int off = 0, n;
                    while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) {
                        off += n;
                    }
                }
                DebugSink.text("export-progress", "stale " + f.getName() + "\n"
                        + new String(buf, java.nio.charset.StandardCharsets.UTF_8));
                f.delete();
            }
        } catch (Throwable ignored) {}
    }

    private java.io.File vkLogFile() {
        return new java.io.File(getCacheDir(), "vk.log");
    }

    private void uploadVkLog() {
        try {
            java.io.File f = vkLogFile();
            if (!f.exists() || f.length() == 0) {
                DebugSink.text("vk-log", "(empty) exists=" + f.exists());
                return;
            }
            // The engine may still hold the file open; read the newest window
            // instead of truncating, so the failure is always in the tail.
            final int cap = 220 * 1024;
            long len = f.length();
            long start = Math.max(0, len - cap);
            byte[] buf = new byte[(int) (len - start)];
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
                raf.seek(start);
                raf.readFully(buf);
            }
            DebugSink.text("vk-log", "bytes=" + len + " from=" + start + "\n"
                    + new String(buf, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Throwable t) {
            try { DebugSink.text("vk-log", "read failed: " + t); } catch (Throwable ignored) {}
        }
    }

    private void uploadCapabilities() {
        try {
            String caps = engine != null ? engine.capabilities() : null;
            if (caps != null) {
                DebugSink.text("capabilities", caps);
            }
            String mem = engine != null ? engine.memoryReport() : null;
            if (mem != null) {
                DebugSink.text("memory-report", mem);
            }
            uploadStaleExportProgress();
        } catch (Throwable ignored) {}
    }

    /** Upload the native export black box, if the JNI left one. */
    private void uploadExportProgress(java.io.File out) {
        try {
            java.io.File prog = new java.io.File(out.getAbsolutePath() + ".prog");
            if (!prog.exists()) {
                return;
            }
            byte[] buf = new byte[(int) prog.length()];
            try (java.io.InputStream in = new java.io.FileInputStream(prog)) {
                int off = 0, n;
                while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) {
                    off += n;
                }
            }
            DebugSink.text("export-progress", new String(buf, java.nio.charset.StandardCharsets.UTF_8));
            prog.delete();
        } catch (Throwable ignored) {}
    }

    /** The rebate's canvas as `WxH`, or "" when this frame has no rebate.
     *  Printed next to the export's own size: when the two disagree the file
     *  carries a band that was never drawn, and that is the whole of the
     *  "the rebate flies in the export but not in the preview" report. */
    private String overscanCanvasLabel(SpektraLabEngine.Session source) {
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
        } catch (Throwable ignored) { return ""; }
    }

    /** Write a finished export into the gallery and return its URI. */
    private Uri writeExportToGallery(java.io.File tmp, String mime, String ext) throws Exception {
        final String fileName = "SpektraLab_" + System.currentTimeMillis() + ext;
        final android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, mime);
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_PICTURES + "/SpektraLab");
        values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
        final android.content.ContentResolver resolver = getContentResolver();
        final Uri outUri = resolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (outUri == null) {
            throw new RuntimeException("无法创建媒体文件");
        }
        final ExportDigest.Result copied = GalleryExport.write(tmp, new GalleryExport.Sink() {
            @Override
            public java.io.OutputStream openOutput() throws java.io.IOException {
                return resolver.openOutputStream(outUri);
            }

            @Override
            public java.io.InputStream openInput() throws java.io.IOException {
                return resolver.openInputStream(outUri);
            }

            @Override
            public void publish() throws java.io.IOException {
                values.clear();
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
                if (resolver.update(outUri, values, null, null) <= 0) {
                    throw new java.io.IOException("导出发布失败：媒体行未更新");
                }
            }

            @Override
            public void discard() {
                try {
                    resolver.delete(outUri, null, null);
                } catch (Throwable ignored) {
                    // The row may already be gone; the original failure matters more.
                }
            }
        });
        try {
            DebugSink.text("export-verify", "bytes=" + copied.length
                    + " sha256=" + ExportDigest.hex(copied.sha256));
        } catch (Throwable ignored) { }
        return outUri;
    }

    private void exportImage(String format, String tier) {
        exportImage(format, tier, null);
    }

    private void exportImage(String format, String tier, java.util.function.IntConsumer onDone) {
        final boolean tiff = "tiff".equals(format);
        final boolean dng = "dng".equals(format);
        final String ext = dng ? ".dng" : tiff ? ".tif" : ".jpg";
        final String mime = dng ? "image/x-adobe-dng" : tiff ? "image/tiff" : "image/jpeg";
        if (sharePill != null) {
            sharePill.setVisibility(View.GONE);
        }
        if (engine == null || session == null) {
            showSnackbar("请先导入照片");
            if (onDone != null) {
                onDone.accept(0);
            }
            return;
        }
        if (live().filmStock == null) {
            showSnackbar("先选一张胶片，再导出");
            if (onDone != null) {
                onDone.accept(0);
            }
            return;
        }
        // The export states the Look it was started with, not whatever the
        // controls say by the time the render thread picks it up: a Frame
        // switch or a slider drag during a long export must not move the file.
        final String exportStock = live().filmStock;
        final Look exportLook = captureLook();
        final RenderSnapshot exportSnapshot = snapshotFor(exportLook, exportStock, PREVIEW_LONG_EDGE, true);
        final double[] exportViewAdjust = exportSnapshot.viewAdjust;
        final String exportParams = exportSnapshot.params;
        final String rawExportPath = isRawFile ? (rawFilePath == null ? "" : rawFilePath) : null;
        // A bitmap export renders from the preview session; a RAW export opens
        // its own below. The render lock keeps a stock change out of either.
        startExportThread(format, tier, tiff, dng, ext, mime, exportStock,
                exportParams, exportViewAdjust, rawExportPath, onDone);
    }

    /** Export every Frame, one at a time, each through the normal reopen path. */
    private void startBatchExport(String format, String tier) {
        if (batchExportRunning) {
            showSnackbar("批量导出已经在跑");
            return;
        }
        if (frames.size() < 2) {
            showSnackbar("只有一张照片，不需要批量");
            return;
        }
        saveLookIntoFrames();
        final java.util.List<Frame> batch = new java.util.ArrayList<>(frames);
        final int home = currentFrame;
        batchExportRunning = true;
        batchExportAbort = false;
        new Thread(() -> runBatchExport(batch, home, format, tier),
                "batch-export").start();
    }

    private void runBatchExport(java.util.List<Frame> batch, int home,
                                String format, String tier) {
        int ok = 0;
        int failed = 0;
        try {
            for (int i = 0; i < batch.size() && !batchExportAbort; ++i) {
                final Frame frame = batch.get(i);
                if (frame.session() == null || frame.session().look().filmStock == null) {
                    ++failed;
                    continue;
                }
                final int index = i;
                final String label = "批量导出 " + (i + 1) + "/" + batch.size();
                runOnUiThread(() -> {
                    statusText.setText(label);
                    cancelLivePreview();
                    closeSessionForBatch();
                    reopenFrameForced(index);
                });
                if (!pollUntil(() -> session != null && !rendering, 180000)) {
                    ++failed;
                    continue;
                }
                final java.util.concurrent.CountDownLatch done =
                        new java.util.concurrent.CountDownLatch(1);
                final java.util.concurrent.atomic.AtomicBoolean exported =
                        new java.util.concurrent.atomic.AtomicBoolean(false);
                runOnUiThread(() -> exportImage(format, tier, success -> {
                    if (success == 1) {
                        exported.set(true);
                    }
                    done.countDown();
                }));
                if (!done.await(20, java.util.concurrent.TimeUnit.MINUTES) || !exported.get()) {
                    ++failed;
                } else {
                    ++ok;
                }
                runOnUiThread(this::closeSessionForBatch);
                try {
                    if (engine != null) {
                        engine.trimPool();
                    }
                } catch (Throwable ignored) { }
                try { SpektraLabEngine.clearRawCache(); } catch (Throwable ignored) { }
                System.gc();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            final int okCount = ok;
            final int failedCount = failed;
            runOnUiThread(() -> finishBatchExport(home, okCount, failedCount));
        }
    }

    private void closeSessionForBatch() {
        synchronized (renderMutex) {
            if (session != null) {
                session.close();
                session = null;
            }
        }
    }

    private void finishBatchExport(int home, int ok, int failed) {
        batchExportRunning = false;
        if (exportProgressBar != null) {
            exportProgressBar.setVisibility(View.GONE);
        }
        final String msg = "批量导出完成：" + ok + " 张成功"
                + (failed > 0 ? "，" + failed + " 张跳过或失败" : "");
        statusText.setText(msg);
        showSnackbar(msg);
        if (home >= 0 && home < frames.size()) {
            reopenFrame(home);
        }
    }

    private boolean pollUntil(java.util.function.BooleanSupplier condition, long timeoutMs) {
        final long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }


    /** Re-state the export Look and render the full-size file. */
    private ExportRender renderExportFile(java.io.File tmp, String exportParams,
                                         double[] viewAdjust, String rawExportPath,
                                         String tier, boolean dng, boolean tiff) {
        final int[] info;
        final String canvas;
        synchronized (renderMutex) {
            // Everything the preview drew with, re-stated: a stock,
            // a paper, a gate, an edge text or a date that moved
            // since the last render would otherwise be exported as
            // the *preview* had it, or not at all.
            SpektraLabEngine.Session target = session;
            SpektraLabEngine.Session rawExport = null;
            try {
                if (rawExportPath != null) {
                    if (rawExportPath.isEmpty()) {
                        throw new RuntimeException("RAW 文件路径不可用，无法导出");
                    }
                    // A RAW export decodes the file into its own
                    // session. Re-stating the preview session used
                    // to change the picture on screen and left the
                    // export sharing the preview's preview-sized
                    // decode. This session is opened with the caps
                    // the import chose, gets the same Look, and is
                    // closed before the gallery write.
                    rawExport = engine.openRawSession(rawExportPath, exportParams,
                            rawImportMaxPixels, rawImportLongEdge);
                    target = rawExport;
                }
                SpektraLabEngine.setViewAdjust(viewAdjust);
                target.setParams(exportParams);
                try {
                    DebugSink.text("overscan-geom", "tier=" + tier + " before "
                            + target.overscanGeometry());
                    // The whole parameter set this export is about to use.
                    // "the export disagrees with the preview" is a question
                    // about which of these two numbers changed.
                    DebugSink.text("export-params", "tier=" + tier
                            + " raw=" + (rawExport != null) + " " + target.getParams());
                } catch (Throwable ignored) { }
                info = dng
                        ? target.exportDng(tmp.getAbsolutePath(), tier)
                        : tiff
                        ? target.exportTiff(tmp.getAbsolutePath(), tier)
                        : target.exportJpeg(tmp.getAbsolutePath(), 95, 512, tier);
                canvas = overscanCanvasLabel(target);
                try {
                    DebugSink.text("overscan-geom", "tier=" + tier + " " + info[0] + "x" + info[1]
                            + " " + target.overscanGeometry());
                } catch (Throwable ignored) { }
            } finally {
                if (rawExport != null) {
                    rawExport.close();
                }
            }
        }
        return new ExportRender(info, canvas);
    }

    /** The engine's export result and the canvas it actually drew. */
    private static final class ExportRender {
        private final int[] info;
        private final String canvas;

        private ExportRender(int[] info, String canvas) {
            this.info = info;
            this.canvas = canvas;
        }
    }

    /**
     * Gray correlation between the exported JPEG and the canvas on screen.
     *
     * @return the score in [-1, 1], or -1 when the file or canvas cannot be read
     */
    private static double exportMatchesCanvas(java.io.File file, Bitmap canvas) {
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

    /** Write the rendered file to the gallery and publish the result. */
    private void finishExport(java.io.File tmp, int[] info, String canvas, long t0,
                                  String format, String tier, String ext, String mime,
                                  boolean tiff, boolean dng, boolean suspect,
                                  java.util.function.IntConsumer onDone) throws Exception {
        uploadExportProgress(tmp);
        uploadVkLog();
        int w = info[0], h = info[1];
        long renderMs = info[2];
        if (w <= 0 || h <= 0 || !tmp.exists() || tmp.length() == 0) {
            throw new RuntimeException("引擎没有输出图像");
        }

        final int fw = w, fh = h;
        runOnUiThread(() -> statusText.setText("导出中：写入相册 " + fw + "x" + fh + "..."));
        final Uri outUri = writeExportToGallery(tmp, mime, ext);

        try {
            long len = tmp.length();
            DebugSink.text("export", "format=" + format + " tier=" + tier
                    + " w=" + w + " h=" + h + " renderMs=" + renderMs
                    + " bytes=" + len);
            if (len > 0 && len < 30L * 1024 * 1024) {
                byte[] buf = new byte[(int) len];
                try (java.io.InputStream in = new java.io.FileInputStream(tmp)) {
                    int off = 0, n;
                    while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) {
                        off += n;
                    }
                }
                DebugSink.bytes("export", "out-" + tier + ext, buf, mime);
            }
        } catch (Throwable ignored) {}
        final long secs = Math.max(1, (System.currentTimeMillis() - t0) / 1000);
        final long fRender = renderMs;
        final long bytes = tmp.exists() ? tmp.length() : 0;
        final String sizeLabel = bytes > 0
                ? String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1048576.0) : "?";
        runOnUiThread(() -> {
            if (suspect) {
                statusText.setText("导出结构异常：可能花屏，请再导一次");
                showSnackbar("导出结构异常：文件已保存，但可能花屏，请再导一次");
                exportProgressBar.setVisibility(View.GONE);
                setStep(2);
            } else {
                applyExportResult(outUri, mime, canvas, fw, fh,
                        fRender, secs, sizeLabel, tiff, dng);
            }
            if (onDone != null) {
                onDone.accept(suspect ? 0 : 1);
            }
        });
        }

    /** Render one export, write it to the gallery and report on the UI thread. */
        private void startExportThread(String format, String tier, boolean tiff, boolean dng,
                                  String ext, String mime, String exportStock,
                                  String exportParams, double[] exportViewAdjust,
                                  String rawExportPath, java.util.function.IntConsumer onDone) {
        new Thread(() -> {
            java.io.File tmp = new java.io.File(getCacheDir(), "export_" + System.currentTimeMillis() + ext);
            try {
                runOnUiThread(() -> {
                statusText.setText("导出中：准备全尺寸渲染（分条）...");
                exportProgressBar.setVisibility(View.VISIBLE);
                setStep(3);
            });
            // A stock delta before an export is fine -- measured, not
            // assumed (`full_after_delta`: mean 104.2 std 11945 before,
            // 102.4 std 12481 after, and the two hashes differ). It is the
            // same delta the preview renders with, which is the point.
            try {
                DebugSink.text("export-params", "film=" + exportStock + " print=" + live().printStock
                        + " raw=" + (rawExportPath != null) + " " + exportParams);
            } catch (Throwable ignored) {}

            final long t0 = System.currentTimeMillis();
            final java.util.concurrent.atomic.AtomicBoolean spinning =
                    new java.util.concurrent.atomic.AtomicBoolean(true);
            final android.os.Handler ticker = new android.os.Handler(android.os.Looper.getMainLooper());
            Runnable tick = new Runnable() {
                @Override public void run() {
                    if (!spinning.get()) {
                        return;
                    }
                    long el = (System.currentTimeMillis() - t0) / 1000;
                    statusText.setText("导出中（" + tier + "）：渲染中 " + el + " 秒...");
                    ticker.postDelayed(this, 1000);
                }
            };
                runOnUiThread(tick);
                final ExportOutcome outcome;
                try {
                    outcome = renderExportAttempts(tmp, exportParams, exportViewAdjust,
                            rawExportPath, tier, dng, tiff);
                } finally {
                    spinning.set(false);
                }
                finishExport(tmp, outcome.info, outcome.canvas, t0, format, tier, ext, mime,
                        tiff, dng, outcome.suspect, onDone);
            } catch (Throwable e) {
                final String msg = describe(e);
                try { DebugSink.text("export-error", "tier=" + tier + " " + msg); } catch (Throwable ignored) {}
                uploadExportProgress(tmp);
                uploadVkLog();
                runOnUiThread(() -> {
                    applyExportFailure(msg);
                    if (onDone != null) {
                        onDone.accept(0);
                    }
                });
                final int code = SpkError.codeOf(msg);
                if (code == 1001 || code == 1009) {
                    offerSmallerImport();
                }
            } finally {
                if (tmp.exists()) {
                    tmp.delete();
                }
            }
        }).start();
    }

    /** Render, and retry once when the written JPEG does not match the canvas. */
    private ExportOutcome renderExportAttempts(java.io.File tmp, String exportParams,
                                               double[] viewAdjust, String rawPath,
                                               String tier, boolean dng, boolean tiff) {
        ExportOutcome outcome = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            final ExportRender rendered = renderExportFile(tmp, exportParams, viewAdjust,
                    rawPath, tier, dng, tiff);
            outcome = new ExportOutcome(rendered.info, rendered.canvas, false);
            if (tiff || dng) {
                return outcome;
            }
            final double ncc = exportMatchesCanvas(tmp, lastRenderedBitmap);
            if (!ExportMatch.suspect(ncc)) {
                logExportNcc(ncc);
                return outcome;
            }
            outcome = new ExportOutcome(rendered.info, rendered.canvas, true);
            logExportCorrupt(ncc, rendered.info, tier, attempt);
            if (attempt == 1) {
                return outcome;
            }
            tmp.delete();
        }
        return outcome;
    }

    private static void logExportNcc(double ncc) {
        if (ncc >= 0) {
            final String line = String.format(java.util.Locale.US, "canvas NCC %.3f", ncc);
            android.util.Log.i("SpektraLabExport", line);
            try {
                DebugSink.text("export-selfcheck", line);
            } catch (Throwable ignored) { }
        }
    }

    private static void logExportCorrupt(double ncc, int[] info, String tier, int attempt) {
        final String line = String.format(java.util.Locale.US,
                "canvas NCC %.3f at %dx%d tier=%s attempt=%d",
                ncc, info[0], info[1], tier, attempt);
        android.util.Log.w("SpektraLabExport", line);
        try {
            DebugSink.textNow("export-corrupt", line);
        } catch (Throwable ignored) { }
    }

    /** The engine's export result, the canvas it drew, and the self-check verdict. */
    private static final class ExportOutcome {
        private final int[] info;
        private final String canvas;
        private final boolean suspect;

        private ExportOutcome(int[] info, String canvas, boolean suspect) {
            this.info = info;
            this.canvas = canvas;
            this.suspect = suspect;
        }
    }

    /**
     * `Class: message`, then its cause chain. A native library that fails to
     * load says *why* in the cause (`dlopen failed: library "libz.so.1" not
     * found`, `cannot locate symbol ...`), and that sentence is the entire bug
     * report. The old form printed only the outer class name.
     */
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

    private static void deleteTree(java.io.File f) {
        if (f == null) {
            return;
        }
        java.io.File[] kids = f.listFiles();
        if (kids != null) {
            for (java.io.File k : kids) {
                deleteTree(k);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private static final Object RESOURCE_LOCK = new Object();

    /**
     * The asset tree is ~30 MB; copying it on every import froze the picker for
     * seconds and was why a second photo only worked after a restart. The copy
     * now runs once per installed build, keyed by the version code, so an
     * upgrade still refreshes a stale .spv (which would otherwise show up as a
     * wrong-sized picture rather than an error).
     */
    private String getResourcesDir() throws java.io.IOException {
        java.io.File dir = new java.io.File(getFilesDir(), "resources");
        java.io.File marker = new java.io.File(dir, ".bundle");
        final String want = bundleVersion();
        if (marker.isFile() && want.equals(readSmallFile(marker))) {
            return dir.getAbsolutePath();
        }
        synchronized (RESOURCE_LOCK) {
            if (marker.isFile() && want.equals(readSmallFile(marker))) {
                return dir.getAbsolutePath();
            }
            final long t0 = System.currentTimeMillis();
            deleteTree(dir);
            copyAssetTree("resources", dir);
            try (java.io.FileWriter fw = new java.io.FileWriter(marker)) { fw.write(want); }
            try {
                DebugSink.text("resources", "copied in " + (System.currentTimeMillis() - t0)
                        + " ms (bundle " + want + ")");
            } catch (Throwable ignored) {}
        }
        return dir.getAbsolutePath();
    }

    private String bundleVersion() {
        try {
            return String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
        } catch (Throwable t) {
            return "0";
        }
    }

    private static String readSmallFile(java.io.File f) {
        try (java.io.InputStream in = new java.io.FileInputStream(f)) {
            byte[] b = new byte[(int) Math.min(f.length(), 32)];
            int off = 0, n;
            while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) {
                off += n;
            }
            return new String(b, 0, off, java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Throwable t) {
            return "";
        }
    }

    private void copyAssetTree(String assetPath, java.io.File outDir) throws java.io.IOException {
        String[] files = getAssets().list(assetPath);
        if (files == null || files.length == 0) {
            java.io.File parent = outDir.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (InputStream in = getAssets().open(assetPath);
                 java.io.OutputStream out = new java.io.FileOutputStream(outDir)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            return;
        }
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new java.io.IOException("Cannot create directory: " + outDir);
        }
        for (String name : files) {
            copyAssetTree(assetPath + "/" + name, new java.io.File(outDir, name));
        }
    }

    @Override
    protected void onStop() {
        saveSessionIfAny();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // A sheet or snackbar outliving its Activity would keep this instance
        // reachable through a window the system still owns.
        dismissOpenSheet();
        if (currentSnackbar != null) {
            currentSnackbar.dismiss();
        }
        currentSnackbar = null;
        snackbarHost = null;
        stopLivePreview();
        // Session *and* engine go down under the same lock the render runs
        // under: a render that is mid-`renderBegin` is holding device buffers
        // owned by this engine, and tearing it down outside the lock is the
        // same destroyed-buffer SIGSEGV one layer up.
        synchronized (renderMutex) {
            if (session != null) { session.close(); session = null; }
            if (engine != null) { engine.close(); engine = null; }
        }
        super.onDestroy();
    }
}

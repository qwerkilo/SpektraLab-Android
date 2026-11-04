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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import app.spektralab.engine.SpektraLabEngine;
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
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
    private SpektraLabEngine.Session session;
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
    private Bitmap lastRenderedBitmap;
    private ByteBuffer sourcePixels;
    private int sourceWidth, sourceHeight;
    private CompareView compare;
    /** What the import put on the stage: the frame before any film. */
    private Bitmap beforeBitmap;
    private TextView statusText;
    private ProgressBar progressBar;
    private ProgressBar exportProgressBar;
    private final List<String> filmIds = new ArrayList<>(), filmLabels = new ArrayList<>();
    private final List<String> filmTypes = new ArrayList<>();
    private final List<String> printIds = new ArrayList<>(), printLabels = new ArrayList<>();
    /** The catalogue's film records, keyed by stock id: the shipped spectral
     *  series, its declared source, and the real scalar `info` fields. An
     *  absent record is the only truthful "unavailable"; it is never filled
     *  with a placeholder or a curve. */
    private final java.util.Map<String, FilmStock> filmStocks = new java.util.HashMap<>();
    /** null until the user picks a film: a fresh launch must not look like
     *  a Portra 400 session, and must not render one behind their back. */
    private String filmStock = null;
    private String printStock = "kodak_portra_endura";
    private float filmFormatMm = 36.0f;    // frame's long edge, 135 = 36 mm
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
        
        buildUi();
        uiReady = true;

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
    private View emptyState, rawState;
    private TextView fileLabel, rawName, busyLabel, originalLabel, paperValue, edgeValue, formatFootValue;
    /**
     * The live Optics values for the current Frame: all 22 contract
     * parameters plus the camera filter and protection norm. The Look owns
     * them; the strip and the one slider only read and write this object, so a
     * capture, a preview, and a restored JSON cannot drift apart.
     */
    private final Look optics = new Look();
    private int geometryTurns;
    private float geometryRotation;
    private float geometryCropX, geometryCropY, geometryCropW = 1f, geometryCropH = 1f;
    private boolean geometryFlipH, geometryFlipV;
    private final LookHistory lookHistory = new LookHistory();
    private MaterialButton undoButton;
    private boolean restoringUndo;
    private int activeOptics = 0;
    /** True while the slider is being parked on a new parameter; no commit. */
    private boolean syncingOptics = false;
    /** True between a real touch-down and touch-up; mid-drag never commits. */
    private boolean trackingOpticsTouch = false;
    private HorizontalScrollView opticsStripScroll;
    private LinearLayout opticsStrip;
    private final List<Chip> opticsChips = new ArrayList<>();
    private Slider opticsSlider;
    private final android.os.Handler viewAdjustHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable viewAdjustCommit = this::renderImage;
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
    private boolean overscanOn = false;
    private String overscanFormat = "135";
    private boolean dateOn = false;
    /** A typed Date back text; null/empty means "follow the Frame's EXIF date". */
    private String dateText = null;
    /** The current file's own imprint date, cached until another image opens. */
    private String defaultDateText = null;
    /** The Rebate page's own controls. Dimming while off is presentation only:
     *  these values stay live so re-enabling or switching Frame finds them. */
    private boolean edgeTextOn = true;
    private String edgeMode = "strip", edgeGate = "auto", edgeHoles = "white", edgeCarrier = "none";
    private float edgeFog = 1f, edgeLeaks, frameNumber, edgeFNumber;
    private int cameraSeed = 1, frameSeed = 1;
    /** Date-back values; the page lands in a later ticket, the Look already
     *  carries them and switching Frame must not drop them. */
    private String datePlacement = "frame", dateCorner = "br", dateStyle = "lcd";
    private float dateInsetX = 3f, dateInsetY = 2.4f, dateSize = 1f, dateEv = 3.5f;
    private String dataText = "", dateTextB = "";
    /** -1 = the two Edge entry cards, 0 = Rebate, 1 = Date back. */
    private int edgePageIndex = -1;
    /** True while a Look restore fills the page; its listeners must not render. */
    private boolean syncingRebate = false;
    private LinearLayout edgeHome, rebatePage, datePage, rebateControls, dateControls;
    private MaterialSwitch rebateSwitch, edgeTextSwitch;
    private MaterialCardView rebateGateCard;
    private TextView rebateGateValue, rebateModeValue, rebateGateStyleValue;
    private TextView rebateHolesValue, rebateCarrierValue;
    private TextView rebateCameraSeedValue, rebateFrameSeedValue;
    private TextView rebateFogValue, rebateLeaksValue, rebateFrameNoValue, rebateFNumberValue;
    private Slider rebateFogSlider, rebateLeaksSlider, rebateFrameNoSlider, rebateFNumberSlider;
    /** Each chosen row repaints its value and description from the live field. */
    private final List<Runnable> rebateRefreshers = new ArrayList<>();
    /** True while a Look restore sets switch state; the listener must not render. */
    private boolean syncingToggles = false;
    private MaterialSwitch dateSwitch;
    /** The date back's own line: the text shown while the imprint is on. */
    private TextView dateDetail;
    private TextView dateTextValue, datePlacementValue, dateCornerValue, dateStyleValue;
    private TextView dateDataValue, dateTextBValue;
    private TextView dateInsetXValue, dateInsetYValue, dateSizeValue, dateEvValue;
    private Slider dateInsetXSlider, dateInsetYSlider, dateSizeSlider, dateEvSlider;

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
        if (gate == null) return;
        overscanFormat = gate.id;
        filmFormatMm = gate.longEdgeMm;
        updateEdgeValue();
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(gate.longEdgeMm) + " mm");
        }
        rebuildFormatCards();
        syncRebatePage();
    }
    /** The shell: `body` stacks or splits into `mainCol` and `controls`. */
    private LinearLayout body, mainCol, controls, panelHolder, formatCardStrip;
    private View stageView;
    /** True while the window is at the two-column Console width. */
    private boolean wideLayout = false;
    private HorizontalScrollView formatScroll;

    /** The Gate the Scale/Edge readout is showing, or the honest empty state. */
    private void updateEdgeValue() {
        if (edgeValue == null) return;
        final int selected = edgeIds.indexOf(overscanFormat);
        if (selected >= 0) {
            edgeValue.setText(edgeLabels.get(selected));
        } else {
            edgeValue.setText(edgeIds.isEmpty() ? "无可用片门" : gateLabel(overscanFormat));
        }
    }

    /** The Gate a reset lands on: 135 when this Frame fits it, else its closest fit. */
    private String defaultGateId() {
        if (edgeIds.isEmpty() || edgeIds.contains("135")) return "135";
        final double aspect = (sourceWidth > 0 && sourceHeight > 0)
                ? (double) Math.max(sourceWidth, sourceHeight) / Math.min(sourceWidth, sourceHeight)
                : 0.0;
        String bestId = edgeIds.get(0);
        double bestError = Double.MAX_VALUE;
        for (String id : edgeIds) {
            final GateScale.Gate gate = GateScale.byId(id);
            if (gate == null) continue;
            final double error = Math.abs(aspect / gate.aspect - 1.0);
            if (error < bestError) { bestError = error; bestId = id; }
        }
        return bestId;
    }
    /**
     * One photograph this session has opened, kept so the strip under the
     * canvas can bring it back without a trip through the picker.
     *
     * <p>`thumb` is its own small bitmap, not a reference to `beforeBitmap`:
     * the live source is replaced on every import, and a list holding those
     * would pin one 1024-px ARGB buffer per photograph.
     */
    private static final class Frame {
        final Uri uri;
        final boolean raw;
        final String name;
        Bitmap thumb;
        Look look = new Look();
        /** Scan inspect belongs to the Frame it was moved on, not to the Look. */
        float scanFraction = ScanSample.DEFAULT_FRACTION;
        boolean scanVisible = false;
        Frame(Uri uri, boolean raw, String name) {
            this.uri = uri; this.raw = raw; this.name = name;
        }
    }
    /** One catalogue film record, in the form the panel reads. */
    private static final class FilmStock {
        final SpectrumSamples samples;
        final SpectrumCurve curve;
        /** "LOG SENSITIVITY", "DENSITY", or "" when nothing was shipped. */
        final String seriesLabel;
        final java.util.Map<String, String> info;
        /** The paper this film declares it prints on; null when it declares none. */
        final String targetPrint;

        FilmStock(SpectrumSamples samples, SpectrumCurve curve, String seriesLabel,
                  java.util.Map<String, String> info, String targetPrint) {
            this.samples = samples;
            this.curve = curve;
            this.seriesLabel = seriesLabel;
            this.info = info;
            this.targetPrint = targetPrint;
        }
    }
    private final java.util.List<Frame> frames = new java.util.ArrayList<>();
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
     *  of `filmStock`. */
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
    private static final int PREVIEW_LONG_EDGE = 2048;
    private final List<View> formatCards = new ArrayList<>();
    private static final int TOP_APP_BAR_HEIGHT_DP = 64;
    private static final int READOUT_HEIGHT_DP = 52;
    private static final int SOURCE_HEIGHT_DP = 54;
    private static final int TABS_HEIGHT_DP = 48;
    private static final int SECTION_HEAD_HEIGHT_DP = 48;
    /** 48 dp sliders plus their M3 label/line-height rows need the panel's
     *  height; the section head, paper row, and brand row are all 48 dp targets.
     *  336 leaves 8 dp for the panel's bottom breathing room. */
    private static final int PANEL_HEIGHT_DP = 336;
    private static final int PAPER_ROW_HEIGHT_DP = 48;
    private static final int FILM_METRICS_HEIGHT_DP = 36;
    /** Breathing room above the gesture bar; the root also carries the inset. */
    private static final int PANEL_BOTTOM_DP = 8;
    private static final int CONTROLS_OPEN_DP = PANEL_HEIGHT_DP + TABS_HEIGHT_DP;
    private static final int CONTROLS_CLOSED_DP = TABS_HEIGHT_DP;
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

    /** The M3 surface role, resolved from the active theme (dynamic on API 31+). */
    private int m3Surface() {
        return MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurface,
                palette.color(Palette.Role.SYSTEM_BAR_BACKGROUND));
    }

    /** The M3 background role, used by the shell instead of Palette.BACKGROUND. */
    private int m3Background() {
        return m3Role(android.R.attr.colorBackground, Palette.Role.BACKGROUND);
    }

    private int m3OutlineVariant() {
        return m3Role(com.google.android.material.R.attr.colorOutlineVariant, Palette.Role.LINE);
    }

    private int m3OnSurface() {
        return m3Role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK);
    }

    private int m3OnSurfaceVariant() {
        return m3Role(com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
    }

    private int m3Primary() {
        return m3Role(com.google.android.material.R.attr.colorPrimary, Palette.Role.ACCENT);
    }

    /** A control colour role, with the Palette only as the pre-theme fallback. */
    private int m3Role(int attr, Palette.Role fallback) {
        return MaterialColors.getColor(this, attr, palette.color(fallback));
    }

    /** A two-state colour for a real checkable control (checked, then default). */
    private static ColorStateList checkedColors(int checked, int unchecked) {
        return new ColorStateList(
                new int[][] { { android.R.attr.state_checked }, {} },
                new int[] { checked, unchecked });
    }

    /** Replace a colour's alpha channel; RGB is kept. */
    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /** Apply an M3 opacity token to a colour without changing its RGB. */
    private static int withOpacity(int color, float opacity) {
        return withAlpha(color, Math.round(opacity * 255f));
    }

    /** An M3 weight on the brand family; API 24–27 synthesises medium as normal. */
    private static Typeface weighted(Typeface family, int weight) {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            return Typeface.create(family, weight, false);
        }
        return Typeface.create(family, weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
    }
    /** The window behind the first frame, so the launch never flashes paper in dark. */
    private void applyWindowPalette() {
        try {
            getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    m3Background()));
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
            getWindow().setStatusBarColor(m3Surface());
            getWindow().setNavigationBarColor(m3Surface());
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
        if (isFinishing()) return;
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
    private static final class UiState {
        int stageMode;
        int activeTab;
        boolean panelCollapsed, opticsUnlocked, showingOriginal;
        boolean busy, progress, exportProgress, shareVisible;
        String fileName, rawNameText, status;
        float scanFraction;
        boolean scanVisible;
        int edgePageIndex = -1;
    }

    private UiState captureUiState() {
        final UiState s = new UiState();
        s.stageMode = stageMode;
        s.activeTab = activeTabIndex;
        s.panelCollapsed = panelCollapsed;
        s.opticsUnlocked = opticsUnlocked;
        s.showingOriginal = showingOriginal;
        s.busy = busyLabel != null && busyLabel.getVisibility() == View.VISIBLE;
        s.progress = progressBar != null && progressBar.getVisibility() == View.VISIBLE;
        s.exportProgress = exportProgressBar != null && exportProgressBar.getVisibility() == View.VISIBLE;
        s.shareVisible = sharePill != null && sharePill.getVisibility() == View.VISIBLE;
        s.fileName = fileLabel == null ? null : String.valueOf(fileLabel.getText());
        s.rawNameText = rawName == null ? null : String.valueOf(rawName.getText());
        s.status = statusText == null ? null : String.valueOf(statusText.getText());
        s.scanFraction = scanFraction;
        s.scanVisible = scanVisible;
        s.edgePageIndex = edgePageIndex;
        return s;
    }

    private void restoreUiState(UiState s) {
        if (s == null) return;
        setStage(s.stageMode, null);
        if (compare != null) {
            compare.setBefore(beforeBitmap);
            compare.setAfter(s.showingOriginal ? null : lastRenderedBitmap);
        }
        if (s.fileName != null && fileLabel != null) fileLabel.setText(s.fileName);
        if (s.rawNameText != null && rawName != null) rawName.setText(s.rawNameText);
        if (s.status != null && statusText != null) statusText.setText(s.status);
        showingOriginal = s.showingOriginal;
        setOriginalVisual(showingOriginal);
        if (busyLabel != null) busyLabel.setVisibility(s.busy ? View.VISIBLE : View.GONE);
        if (progressBar != null) progressBar.setVisibility(s.progress ? View.VISIBLE : View.GONE);
        if (exportProgressBar != null) {
            exportProgressBar.setVisibility(s.exportProgress ? View.VISIBLE : View.GONE);
        }
        if (sharePill != null) sharePill.setVisibility(s.shareVisible ? View.VISIBLE : View.GONE);
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(filmFormatMm) + " mm");
        }
        setTab(s.activeTab);
        setPanelCollapsed(s.panelCollapsed);
        setOpticsUnlocked(s.opticsUnlocked);
        edgePageIndex = s.edgePageIndex;
        openEdgePage(edgePageIndex);
        scanFraction = s.scanFraction;
        scanVisible = s.scanVisible;
        refreshFilmPanel();
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
                || navigationRail == null) return;
        wideLayout = wide;
        body.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        // The M3 navigation rail takes the leading edge only on a wide window;
        // the phone keeps the secondary tabs under the stage.
        LinearLayout.LayoutParams railLp =
                (LinearLayout.LayoutParams) navigationRail.getLayoutParams();
        railLp.width = dp(ConsoleLayout.railWidthDp(widthDp));
        railLp.height = -1;
        railLp.weight = 0f;
        navigationRail.setLayoutParams(railLp);
        navigationRail.setVisibility(wide ? View.VISIBLE : View.GONE);
        if (tabLayout != null) tabLayout.setVisibility(wide ? View.GONE : View.VISIBLE);

        LinearLayout.LayoutParams mainLp = (LinearLayout.LayoutParams) mainCol.getLayoutParams();
        mainLp.width = wide ? 0 : -1;
        mainLp.height = wide ? -1 : 0;
        mainLp.weight = 1f;
        mainCol.setLayoutParams(mainLp);

        LinearLayout.LayoutParams controlLp =
                (LinearLayout.LayoutParams) controls.getLayoutParams();
        controlLp.width = wide ? dp(ConsoleLayout.controlColumnWidthDp(widthDp)) : -1;
        controlLp.height = wide
                ? -1 : dp(panelCollapsed ? CONTROLS_CLOSED_DP : CONTROLS_OPEN_DP);
        controlLp.weight = 0f;
        controls.setLayoutParams(controlLp);

        LinearLayout.LayoutParams panelLp =
                (LinearLayout.LayoutParams) panelHolder.getLayoutParams();
        panelLp.height = wide ? 0 : dp(PANEL_HEIGHT_DP);
        panelLp.weight = wide ? 1f : 0f;
        panelHolder.setLayoutParams(panelLp);

        // On a wide window the control column is a distinct instrument rail:
        // the page colour would otherwise run on below the panel and read as
        // an unfinished half-column. The film curve then takes the rest of the
        // rail on the default tab instead of leaving its lower half empty.
        final int rail = wide ? m3Surface() : m3Background();
        controls.setBackgroundColor(rail);
        panelHolder.setBackgroundColor(rail);
        applyFilmCurveHeight(wide);
        updateStageReadoutWidths();
    }

    /** Let the spectral curve's M3 surface own the film panel's spare height. */
    private void applyFilmCurveHeight(boolean wide) {
        final View surface = filmCurveCard != null ? filmCurveCard : filmCurve;
        if (surface == null) return;
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) surface.getLayoutParams();
        if (lp == null) return;
        lp.height = wide ? 0 : dp(FILM_CURVE_HEIGHT_DP);
        lp.weight = wide ? 1f : 0f;
        surface.setLayoutParams(lp);
    }

    /** Keep the status line inside the stage when the stage is not full width. */
    private void updateStageReadoutWidths() {
        if (statusText == null || stageView == null) return;
        final int stageWidth = stageView.getWidth();
        if (stageWidth <= 0) return;
        final int byScreen = Math.round(currentWidthDp() * 0.60f
                * getResources().getDisplayMetrics().density);
        final int wanted = Math.max(dp(120), Math.min(byScreen, stageWidth - dp(48)));
        if (statusText.getMaxWidth() != wanted) statusText.setMaxWidth(wanted);
    }

    /** Rotations and foldable resizes arrive here because the manifest keeps
     *  the Activity alive; no Frame, Look, or render is touched. */
    /** System Back leaves a secondary page before it leaves the Console. */
    @Override
    public void onBackPressed() {
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

    private TextView label(String s, float sp, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (tf != null) t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        return t;
    }

    /** A Mixed run keeps Latin on Inter and Chinese on OPPO Sans, so a title
     *  like "PHYSICAL SCALE / 光学尺度" never falls back to the platform font. */
    private static boolean isCjk(char c) {
        return (c >= 0x2E80 && c <= 0x9FFF)
                || (c >= 0x3000 && c <= 0x303F)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFF00 && c <= 0xFF65);
    }

    private static class TfSpan extends android.text.style.MetricAffectingSpan {
        private final Typeface tf;
        private final int weight;
        TfSpan(Typeface tf) { this(tf, 400); }
        TfSpan(Typeface tf, int weight) { this.tf = tf; this.weight = weight; }
        @Override public void updateDrawState(android.text.TextPaint p) { apply(p); }
        @Override public void updateMeasureState(android.text.TextPaint p) { apply(p); }
        private void apply(android.text.TextPaint p) {
            p.setTypeface(weighted(tf, weight));
        }
    }

    private CharSequence mixedText(String text) {
        return mixedText(text, 400);
    }

    private CharSequence mixedText(String text, int weight) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder(text);
        int i = 0;
        while (i < text.length()) {
            final boolean cjk = isCjk(text.charAt(i));
            int j = i + 1;
            while (j < text.length() && isCjk(text.charAt(j)) == cjk) ++j;
            sb.setSpan(new TfSpan(cjk ? tfHan : tfBody, weight), i, j,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            i = j;
        }
        return sb;
    }

    /** Mixed Chinese/Latin runs with one M3 role: size, weight, tracking, line height. */
    private TextView mixedM3Label(String text, M3Tokens.TypeRole role, int color) {
        final TextView t = new TextView(this);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, role.sizeSp);
        t.setTextColor(color);
        t.setText(mixedText(text, role.weight));
        t.setIncludeFontPadding(false);
        t.setLetterSpacing(role.letterSpacingEm);
        applyM3LineHeight(t, role);
        return t;
    }

    /** Text with one M3 type role: size, weight, tracking, line height, brand family. */
    private TextView m3Label(String text, M3Tokens.TypeRole role, int color, Typeface family) {
        final TextView view = label(text, role.sizeSp, color, family);
        applyTypeRole(view, role, family);
        return view;
    }

    /** Apply a role to an existing TextView, including a Material component's label. */
    private void applyTypeRole(TextView view, M3Tokens.TypeRole role, Typeface family) {
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, role.sizeSp);
        view.setTypeface(weighted(family, role.weight));
        view.setLetterSpacing(role.letterSpacingEm);
        applyM3LineHeight(view, role);
    }

    /** Make each line exactly the role's M3 line height, not the font's default. */
    private void applyM3LineHeight(TextView view, M3Tokens.TypeRole role) {
        final android.graphics.Paint.FontMetrics metrics = view.getPaint().getFontMetrics();
        final float natural = metrics.descent - metrics.ascent;
        final float target = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, role.lineHeightSp,
                getResources().getDisplayMetrics());
        view.setLineSpacing(Math.max(0f, target - natural), 1f);
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
            v.setBackground(new EdgeBackdrop(m3Background()));
            return insets;
        });
    }

    /**
     * The M3 snackbar's parent. A CoordinatorLayout is what Snackbar expects
     * for anchoring and swipe behaviour; the width cap is the M3 600 dp
     * maximum, so a tablet gets a readable bar rather than a full-window strip.
     */
    private static final class SnackbarHost extends CoordinatorLayout {
        private final int maxWidthPx;

        SnackbarHost(android.content.Context context) {
            super(context);
            maxWidthPx = Math.round(600f * context.getResources().getDisplayMetrics().density);
        }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int mode = MeasureSpec.getMode(widthMeasureSpec);
            if (mode != MeasureSpec.UNSPECIFIED) {
                final int width = Math.min(MeasureSpec.getSize(widthMeasureSpec), maxWidthPx);
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private static class EdgeBackdrop extends android.graphics.drawable.Drawable {
        private final int background;
        private final android.graphics.Paint paint = new android.graphics.Paint();
        EdgeBackdrop(int background) {
            this.background = background;
        }
        @Override public void draw(android.graphics.Canvas canvas) {
            paint.setColor(background);
            canvas.drawRect(getBounds(), paint);
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
    }

    /**
     * The Spectrum's spatial reference: 380–780 nm along the preview's right
     * edge. It is a scale, not a measurement -- no sample, look, or render is
     * read here.
     */
    private static class SpectralRail extends View {
        private static final int TICKS = 40;   // one per 10 nm
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int lineColor, majorColor, textColor, shadowColor;
        private final float density;

        SpectralRail(android.content.Context context, Palette palette) {
            super(context);
            density = getResources().getDisplayMetrics().density;
            lineColor = palette.color(Palette.Role.GUIDE_LINE);
            majorColor = palette.color(Palette.Role.GUIDE_CORNER);
            textColor = palette.color(Palette.Role.STAGE_INK);
            shadowColor = palette.color(Palette.Role.STAGE_SHADOW);
            setContentDescription("光谱参考刻度，380 至 780 纳米，不是测量值");
        }

        private float dp(float v) {
            return v * density;
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            final float h = getHeight();
            final float pad = dp(14);
            paint.setStrokeWidth(Math.max(1f, density));
            paint.setStyle(android.graphics.Paint.Style.STROKE);

            paint.setColor(lineColor);
            canvas.drawLine(0.5f, 0, 0.5f, h, paint);

            for (int i = 0; i <= TICKS; ++i) {
                final int nm = SpectrumSamples.RAIL_MIN_NM + i * 10;
                final boolean major = nm % 50 == 0;
                final float y = pad + (h - 2f * pad) * i / (float) TICKS;
                paint.setColor(major ? majorColor : lineColor);
                canvas.drawLine(0.5f, y, dp(major ? 7 : 3), y, paint);
            }

            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setColor(textColor);
            paint.setShadowLayer(dp(2), 0, dp(1), shadowColor);
            paint.setTextSize(dp(5.5f));
            paint.setTypeface(android.graphics.Typeface.create("sans-serif", Typeface.NORMAL));
            final float x = dp(8);
            canvas.drawText(String.valueOf(SpectrumSamples.RAIL_MIN_NM), x,
                    pad + dp(2), paint);
            canvas.drawText(String.valueOf(SpectrumSamples.RAIL_MID_NM), x,
                    h * 0.5f + dp(2), paint);
            canvas.drawText(String.valueOf(SpectrumSamples.RAIL_MAX_NM), x,
                    h - pad + dp(2), paint);
            paint.clearShadowLayer();
        }
    }

    private void buildUi() {
        if (tfBody == null) loadUiFonts();
        applyWindowPalette();
        applySystemBars();

        final android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(m3Background());
        applySystemInsets(root);
        setContentView(root);
        uiRoot = root;

        LinearLayout app = new LinearLayout(this);
        app.setOrientation(LinearLayout.VERTICAL);
        app.setBackgroundColor(m3Background());
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

        // The Console shell: a header on the page, then the photograph as a
        // full-bleed stage, its instrument readout strip, the source strip,
        // and the four tabs that own the panel. Below the tablet breakpoint
        // those two groups stack; at or above it they become two columns.
        app.addView(buildTopAppBar(),
                new LinearLayout.LayoutParams(-1, dp(TOP_APP_BAR_HEIGHT_DP)));

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(m3Background());
        app.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        navigationRail = buildNavigationRail();
        body.addView(navigationRail, new LinearLayout.LayoutParams(
                dp(ConsoleLayout.RAIL_WIDTH_DP), -1));

        mainCol = new LinearLayout(this);
        mainCol.setOrientation(LinearLayout.VERTICAL);
        mainCol.setBackgroundColor(m3Background());
        body.addView(mainCol, new LinearLayout.LayoutParams(-1, 0, 1f));

        android.widget.FrameLayout stage = new android.widget.FrameLayout(this);
        stageView = stage;
        stage.setBackgroundColor(palette.color(Palette.Role.STAGE));
        stage.setClipChildren(false);
        stage.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                updateStageReadoutWidths());

        compare = new CompareView(this,
                m3Role(com.google.android.material.R.attr.colorOutline, Palette.Role.LINE),
                m3Surface(), m3Primary());
        compare.setVisibility(View.GONE);
        stage.addView(compare, new android.widget.FrameLayout.LayoutParams(-1, -1));

        // Scan inspect draws over the picture but under the corner readouts and
        // the shelf handle. It handles touches only while the mode is on; when
        // it is off, CompareView keeps the divider and the long-press hold.
        scanOverlay = new ScanOverlayView(this, palette, compare, this::onScanSample);
        scanOverlay.setVisibility(View.GONE);
        stage.addView(scanOverlay, new android.widget.FrameLayout.LayoutParams(-1, -1));

        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(dp(16), dp(16), dp(16), dp(16));
        // One control, in the middle of the frame. The index line, the NO IMAGE
        // headline and the "select a RAW or JPEG" subtitle said the same thing
        // three times over, in three type sizes, and only the last of them
        // could be acted on.
        LinearLayout load = new LinearLayout(this);
        load.setOrientation(LinearLayout.HORIZONTAL);
        load.setGravity(android.view.Gravity.CENTER);
        load.setBackground(pressableFill(Palette.Role.SURFACE_ELEVATED));
        load.setPadding(dp(18), 0, dp(18), 0);
        load.setOnClickListener(v -> showImportMenu());
        // At 2.0× the centred pill runs into the PRINT EXPOSURE readout at the
        // stage's right edge; bias it left rather than shrinking the label.
        if (veryLargeText()) load.setTranslationX(-dp(44));
        load.addView(m3Label("选择照片",
                veryLargeText() ? M3Tokens.TypeRole.LABEL_MEDIUM : M3Tokens.TypeRole.LABEL_LARGE,
                m3Primary(), tfHan));
        ArrowView loadArrow = new ArrowView(this, palette.color(Palette.Role.ACCENT));
        LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(dp(14), dp(14));
        arrowLp.leftMargin = dp(8);
        load.addView(loadArrow, arrowLp);
        empty.addView(load, new LinearLayout.LayoutParams(-2, dp(48)));
        stage.addView(empty, new android.widget.FrameLayout.LayoutParams(-1, -1));
        emptyState = empty;

        LinearLayout raw = new LinearLayout(this);
        raw.setOrientation(LinearLayout.VERTICAL);
        raw.setGravity(android.view.Gravity.CENTER);
        raw.setPadding(dp(24), dp(16), dp(24), dp(16));
        raw.setVisibility(View.GONE);
        TextView rawTitle = label("RAW LOADED", 34, palette.color(Palette.Role.STAGE_INK), tfDisplay);
        rawTitle.setLetterSpacing(-0.055f);
        raw.addView(rawTitle);
        rawName = label("", 9, palette.color(Palette.Role.STAGE_INK), tfHan);
        rawName.setAlpha(0.7f);
        rawName.setLineSpacing(dp(4), 1f);
        rawName.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams rawNameLp = new LinearLayout.LayoutParams(-2, -2);
        rawNameLp.topMargin = dp(13);
        raw.addView(rawName, rawNameLp);
        stage.addView(raw, new android.widget.FrameLayout.LayoutParams(-1, -1));
        rawState = raw;

        busyLabel = label("RENDERING", 18, palette.color(Palette.Role.STAGE_INK), tfDisplay);
        busyLabel.setLetterSpacing(0.14f);
        busyLabel.setGravity(android.view.Gravity.CENTER);
        busyLabel.setBackgroundColor(palette.color(Palette.Role.STAGE_BUSY));
        busyLabel.setVisibility(View.GONE);
        stage.addView(busyLabel, new android.widget.FrameLayout.LayoutParams(-1, -1));

        addStageReadouts(stage);
        addScanControls(stage);
        // TalkBack reaches the scan line after the chip that turns it on,
        // even though the overlay was added before the corner controls.
        if (scanToggleChip != null) {
            scanToggleChip.setId(View.generateViewId());
            scanOverlay.setId(View.generateViewId());
            scanOverlay.setAccessibilityTraversalAfter(scanToggleChip.getId());
        }

        shelfHandle = buildShelfHandle();
        android.widget.FrameLayout.LayoutParams handleLp = new android.widget.FrameLayout.LayoutParams(
                dp(62), dp(62), android.view.Gravity.BOTTOM | android.view.Gravity.START);
        handleLp.leftMargin = dp(12);
        handleLp.bottomMargin = dp(12);
        stage.addView(shelfHandle, handleLp);

        android.widget.FrameLayout jobs = new android.widget.FrameLayout(this);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(
                palette.color(Palette.Role.STAGE_INK)));
        progressBar.setVisibility(View.GONE);
        jobs.addView(progressBar, new android.widget.FrameLayout.LayoutParams(-1, -1));
        exportProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        exportProgressBar.setIndeterminate(true);
        exportProgressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(
                palette.color(Palette.Role.STAGE_INK)));
        exportProgressBar.setVisibility(View.GONE);
        jobs.addView(exportProgressBar, new android.widget.FrameLayout.LayoutParams(-1, -1));
        stage.addView(jobs, new android.widget.FrameLayout.LayoutParams(-1, dp(2), android.view.Gravity.BOTTOM));

        mainCol.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1f));
        mainCol.addView(buildReadoutStrip(),
                new LinearLayout.LayoutParams(-1, -2));
        mainCol.addView(buildSourceBar(),
                new LinearLayout.LayoutParams(-1, dp(SOURCE_HEIGHT_DP)));

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setBackgroundColor(m3Background());

        panelHolder = new LinearLayout(this);
        panelHolder.setOrientation(LinearLayout.VERTICAL);
        panelHolder.setPadding(dp(16), 0, dp(16), dp(PANEL_BOTTOM_DP));
        panelHolder.addView(scrollablePanel(buildFilmPanel()),
                new LinearLayout.LayoutParams(-1, -1));
        panelHolder.addView(scrollablePanel(buildOpticsPanel()),
                new LinearLayout.LayoutParams(-1, -1));
        panelHolder.addView(scrollablePanel(buildFormatPanel()),
                new LinearLayout.LayoutParams(-1, -1));
        panelHolder.addView(scrollablePanel(buildEdgePanel()),
                new LinearLayout.LayoutParams(-1, -1));
        for (int i = 0; i < 4; ++i) panels[i] = panelHolder.getChildAt(i);

        controls.addView(buildTabs(), new LinearLayout.LayoutParams(-1, dp(TABS_HEIGHT_DP)));
        controls.addView(panelHolder, new LinearLayout.LayoutParams(-1, dp(PANEL_HEIGHT_DP)));
        body.addView(controls, new LinearLayout.LayoutParams(-1, dp(CONTROLS_OPEN_DP)));
        applyResponsiveLayout(ConsoleLayout.isWide(currentWidthDp()), currentWidthDp());

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
            if (previous != null) previous.uncaughtException(thread, error);
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
        if (message == null || message.length() == 0) return;
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            runOnUiThread(() -> showSnackbar(message, action, onAction, duration));
            return;
        }
        if (snackbarHost == null) return;
        if (currentSnackbar != null) currentSnackbar.dismiss();
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
            if (actionView != null) actionView.setTypeface(tfHan);
        }
        bar.addCallback(new Snackbar.Callback() {
            @Override public void onDismissed(Snackbar dismissed, int event) {
                if (currentSnackbar == dismissed) currentSnackbar = null;
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

        final MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle(typed("SpektraLab", tfDisplay));
        // At very large text the wordmark and the two actions compete for one
        // 48 dp row; the subtitle is the first thing to go so the name and
        // the actions both stay legible. At 1.3× the tagline still fits.
        if (!veryLargeText()) toolbar.setSubtitle(mixedText("/ CONSOLE"));
        toolbar.setLongClickable(true);
        toolbar.setOnLongClickListener(v -> { showAbout(); return true; });

        final LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.CENTER_VERTICAL);

        sharePill = actionButton(mixedText("分享"), "分享最近一次导出");
        sharePill.setVisibility(View.GONE);
        sharePill.setOnClickListener(v -> shareLastExport());
        actions.addView(sharePill, actionButtonParams());

        undoButton = actionButton(mixedText(veryLargeText() ? "撤" : "撤销"),
                "撤销上一次调整");
        undoButton.setVisibility(View.GONE);
        undoButton.setOnClickListener(v -> undoLook());
        actions.addView(undoButton, actionButtonParams());

        final MaterialButton themeButton = actionButton(
                mixedText(veryLargeText()
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

        final MaterialButton export = actionButton(mixedText("导出"), "导出 EXPORT 照片");
        export.setOnClickListener(v -> showExportDialog());
        actions.addView(export, actionButtonParams());

        toolbar.addView(actions, new Toolbar.LayoutParams(
                -2, dp(48), android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL));
        appBar.addView(toolbar, new AppBarLayout.LayoutParams(-1, -1));
        return appBar;
    }

    /** The M3 undo action is visible only while a previous Look exists. */
    private void updateUndoAction() {
        if (undoButton == null) return;
        final boolean can = lookHistory.canUndo();
        undoButton.setVisibility(can ? View.VISIBLE : View.GONE);
        undoButton.setContentDescription(can
                ? "撤销上一次调整，共 " + (lookHistory.size() - 1) + " 步"
                : "没有可撤销的调整");
    }

    /** Restore the previous settled Look for this Frame and repaint it. */
    private void undoLook() {
        if (rendering) {
            showSnackbar("正在渲染，完成后才能撤销…");
            return;
        }
        final Look previous = lookHistory.undo();
        if (previous == null) {
            updateUndoAction();
            return;
        }
        restoringUndo = true;
        try {
            applyLook(previous);
        } finally {
            restoringUndo = false;
        }
        updateUndoAction();
        renderImage();
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
                withOpacity(color, M3Tokens.StateLayer.PRESSED.opacity)));
        button.setElevation(0f);
        button.setStateListAnimator(null);
        button.setContentDescription(description);
        applyTypeRole(button, M3Tokens.TypeRole.LABEL_LARGE, tfHan);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    /** A run with one family, for the wordmark's Inter Light. */
    private CharSequence typed(String text, Typeface face) {
        final android.text.SpannableString span = new android.text.SpannableString(text);
        span.setSpan(new TfSpan(face), 0, span.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
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
        outer.setBackgroundColor(m3Surface());
        outer.setMinimumHeight(dp(READOUT_HEIGHT_DP));
        outer.addView(hairline(m3OutlineVariant()),
                new LinearLayout.LayoutParams(-1, dp(1)));

        final String[] titles = {
                "FILM / 胶片", "PAPER / 相纸", "EXPOSURE / 曝光", "SPECTRUM / 光谱"};
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
                if (i >= titles.length) break;
                if (column > 0) {
                    row.addView(hairline(m3OutlineVariant()),
                            new LinearLayout.LayoutParams(dp(1), -1));
                }
                row.addView(readoutCell(i, titles[i]),
                        new LinearLayout.LayoutParams(0, -2, 1f));
            }
            grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            if ((rowIndex + 1) * columns < titles.length) {
                grid.addView(hairline(m3OutlineVariant()),
                        new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        outer.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        return outer;
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

        TextView title = m3Label(titleText, M3Tokens.TypeRole.LABEL_SMALL,
                m3OnSurfaceVariant(), tfHan);
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
        TextView value = m3Label("—", M3Tokens.TypeRole.LABEL_MEDIUM,
                m3OnSurface(), i == 2 ? tfDisplay : tfHan);
        value.setSingleLine(true);
        value.setEllipsize(android.text.TextUtils.TruncateAt.END);
        valueRow.addView(value, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView unit = m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                m3OnSurfaceVariant(), tfBody);
        LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(-2, -2);
        unitLp.leftMargin = dp(2);
        unitLp.bottomMargin = dp(1);
        valueRow.addView(unit, unitLp);
        LinearLayout.LayoutParams valueRowLp = new LinearLayout.LayoutParams(-1, -2);
        valueRowLp.topMargin = dp(1);
        cell.addView(valueRow, valueRowLp);

        TextView detail = m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                m3OnSurfaceVariant(), tfBody);
        detail.setSingleLine(false);
        detail.setMaxLines(2);
        detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.addView(detail, new LinearLayout.LayoutParams(-1, -2));

        if (i == 0) readoutFilm = value;
        if (i == 1) readoutPaper = value;
        if (i == 2) readoutExposure = value;
        if (i == 3) {
            readoutSpectrumValue = value;
            readoutSpectrumUnit = unit;
            readoutSpectrumDetail = detail;
            readoutSpectrumCell = cell;
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
    private void refreshReadoutStrip() {
        final String exposure = OpticsDelta.valueText(
                optics.get(OpticsDelta.Param.EXPOSURE));
        if (stageExposureValue != null) stageExposureValue.setText(exposure);
        if (readoutExposure == null) return;
        readoutExposure.setText(exposure);
        readoutFilm.setText(stockLabel(filmStock, filmIds, filmLabels));
        readoutPaper.setText(isPositiveFilm(filmStock)
                ? "DIRECT SCAN" : stockLabel(printStock, printIds, printLabels));

        final FilmStock stock = currentFilmStock();
        final SpectrumSamples spectrum = stock == null
                ? SpectrumSamples.unavailable() : stock.samples;
        readoutSpectrumValue.setText(spectrum.valueText());
        readoutSpectrumUnit.setText(spectrum.unitText());
        readoutSpectrumDetail.setText(spectrum.detailText());
        if (readoutSpectrumCell != null) {
            final String description;
            if (filmStock == null) {
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
        if (id == null || id.isEmpty()) return "\u2014";
        final int i = ids.indexOf(id);
        if (i < 0 || i >= labels.size()) return id;
        return StockNames.readout(labels.get(i));
    }

    /** The picker's full name for the same record. */
    private String fullStockLabel(String id, List<String> ids, List<String> labels) {
        if (id == null || id.isEmpty()) return "\u2014";
        final int i = ids.indexOf(id);
        if (i < 0 || i >= labels.size()) return id;
        return StockNames.full(labels.get(i));
    }

    /**
     * Corner readouts over the full-bleed photograph: file and source at the
     * top left, print exposure at the top right, live state at the lower
     * right. The Spectral rail takes the right edge.
     */
    private void addStageReadouts(android.widget.FrameLayout stage) {
        View topScrim = new View(this);
        topScrim.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        palette.color(Palette.Role.STAGE_SCRIM_TOP),
                        palette.color(Palette.Role.STAGE_SCRIM_MID),
                        palette.color(Palette.Role.STAGE_SCRIM_END)}));
        topScrim.setClickable(false);
        stage.addView(topScrim, new android.widget.FrameLayout.LayoutParams(
                -1, dp(76), android.view.Gravity.TOP));

        View bottomScrim = new View(this);
        bottomScrim.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{
                        palette.color(Palette.Role.STAGE_SCRIM_TOP),
                        palette.color(Palette.Role.STAGE_SCRIM_MID),
                        palette.color(Palette.Role.STAGE_SCRIM_END)}));
        bottomScrim.setClickable(false);
        stage.addView(bottomScrim, new android.widget.FrameLayout.LayoutParams(
                -1, dp(68), android.view.Gravity.BOTTOM));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setClickable(false);
        fileLabel = label("未选择照片", 6.5f, palette.color(Palette.Role.STAGE_INK), tfHan);
        fileLabel.setAlpha(0.82f);
        fileLabel.setSingleLine(true);
        fileLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        fileLabel.setShadowLayer(dp(2), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        info.addView(fileLabel, new LinearLayout.LayoutParams(-1, -2));
        stageMeta = label("NO FRAME", 5.5f,
                palette.color(Palette.Role.STAGE_INK), tfBody);
        stageMeta.setAlpha(0.66f);
        stageMeta.setLetterSpacing(0.12f);
        stageMeta.setSingleLine(true);
        stageMeta.setShadowLayer(dp(2), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
        metaLp.topMargin = dp(2);
        info.addView(stageMeta, metaLp);
        android.widget.FrameLayout.LayoutParams infoLp = new android.widget.FrameLayout.LayoutParams(
                -1, -2, android.view.Gravity.TOP | android.view.Gravity.START);
        infoLp.leftMargin = dp(14);
        // Keep the file name clear of the exposure block in the other corner.
        infoLp.rightMargin = dp(122);
        infoLp.topMargin = dp(10);
        stage.addView(info, infoLp);

        LinearLayout exposure = new LinearLayout(this);
        exposure.setOrientation(LinearLayout.VERTICAL);
        exposure.setGravity(android.view.Gravity.END);
        exposure.setClickable(false);
        TextView exposureCaption = label("PRINT EXPOSURE", 5.5f,
                palette.color(Palette.Role.STAGE_INK), tfBody);
        exposureCaption.setAlpha(0.66f);
        exposureCaption.setLetterSpacing(0.12f);
        exposureCaption.setGravity(android.view.Gravity.END);
        exposureCaption.setShadowLayer(dp(2), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        exposure.addView(exposureCaption);
        LinearLayout exposureRow = new LinearLayout(this);
        exposureRow.setOrientation(LinearLayout.HORIZONTAL);
        exposureRow.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.END);
        stageExposureValue = label("1.00", 27,
                palette.color(Palette.Role.STAGE_INK), tfDisplay);
        stageExposureValue.setLetterSpacing(-0.05f);
        stageExposureValue.setSingleLine(true);
        stageExposureValue.setShadowLayer(dp(3), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        exposureRow.addView(stageExposureValue);
        TextView ev = label("EV", 7,
                palette.color(Palette.Role.STAGE_INK), tfBody);
        ev.setAlpha(0.8f);
        LinearLayout.LayoutParams evLp = new LinearLayout.LayoutParams(-2, -2);
        evLp.leftMargin = dp(4);
        evLp.bottomMargin = dp(3);
        exposureRow.addView(ev, evLp);
        exposure.addView(exposureRow);
        android.widget.FrameLayout.LayoutParams exposureLp = new android.widget.FrameLayout.LayoutParams(
                -2, -2, android.view.Gravity.TOP | android.view.Gravity.END);
        exposureLp.rightMargin = dp(34);
        exposureLp.topMargin = dp(8);
        stage.addView(exposure, exposureLp);

        statusText = label("等待导入", 6.5f,
                palette.color(Palette.Role.STAGE_INK), tfHan);
        statusText.setSingleLine(true);
        statusText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        statusText.setGravity(android.view.Gravity.END);
        statusText.setAlpha(0.76f);
        statusText.setShadowLayer(dp(2), 0, dp(1), palette.color(Palette.Role.STAGE_SHADOW));
        statusText.setMaxWidth(Math.round(getResources().getConfiguration().screenWidthDp
                * 0.60f * getResources().getDisplayMetrics().density));
        android.widget.FrameLayout.LayoutParams statusLp = new android.widget.FrameLayout.LayoutParams(
                -2, -2, android.view.Gravity.BOTTOM | android.view.Gravity.END);
        statusLp.rightMargin = dp(34);
        statusLp.bottomMargin = dp(10);
        stage.addView(statusText, statusLp);

        spectralRail = new SpectralRail(this, palette);
        spectralRail.setBackgroundColor(palette.color(Palette.Role.STAGE_SCRIM_TOP));
        stage.addView(spectralRail, new android.widget.FrameLayout.LayoutParams(
                dp(26), -1, android.view.Gravity.END));
    }

    /**
     * Scan inspect's two controls: a checkable M3 chip under the file readout,
     * and an M3 filled-card readout above the shelf handle. The readout is
     * deliberately away from the print-exposure corner, the status line, and
     * the folded shelf so the three never share a corner.
     */
    private void addScanControls(android.widget.FrameLayout stage) {
        final int chipSurface = m3Role(
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE_ELEVATED);
        final int chipSelected = m3Role(
                com.google.android.material.R.attr.colorSecondaryContainer,
                Palette.Role.ACCENT_SOFT);
        final int chipText = m3Role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
        final int chipTextSelected = m3Role(
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                Palette.Role.ACCENT);
        final int chipOutline = m3Role(
                com.google.android.material.R.attr.colorOutlineVariant,
                Palette.Role.LINE);
        final Chip chip = new Chip(this, null,
                com.google.android.material.R.attr.chipStyle);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipIconVisible(false);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setText(mixedText("SCAN · 扫描", M3Tokens.TypeRole.LABEL_LARGE.weight));
        applyTypeRole(chip, M3Tokens.TypeRole.LABEL_LARGE, tfHan);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setChipBackgroundColor(checkedColors(chipSelected, chipSurface));
        chip.setChipStrokeWidth(Math.max(1, dp(1)));
        chip.setChipStrokeColor(checkedColors(chipSelected, chipOutline));
        chip.setTextColor(checkedColors(chipTextSelected, chipText));
        chip.setContentDescription("扫描检视，点击开启");
        chip.setOnClickListener(v -> toggleScan());
        scanToggleChip = chip;
        android.widget.FrameLayout.LayoutParams toggleLp =
                new android.widget.FrameLayout.LayoutParams(
                        -2, -2, android.view.Gravity.TOP | android.view.Gravity.START);
        toggleLp.leftMargin = dp(14);
        toggleLp.topMargin = dp(34);
        stage.addView(chip, toggleLp);

        // A filled M3 card is the readout surface; the numbers inside stay
        // measured text, and the card remains a non-interactive announcement.
        final int cardSurface = m3Role(
                com.google.android.material.R.attr.colorSurfaceContainer,
                Palette.Role.SURFACE_ELEVATED);
        final int cardText = m3Role(
                com.google.android.material.R.attr.colorOnSurface,
                Palette.Role.INK);
        final int cardMuted = m3Role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
        final MaterialCardView card = new MaterialCardView(this, null,
                com.google.android.material.R.attr.materialCardViewFilledStyle);
        card.setCardBackgroundColor(cardSurface);
        card.setRadius(dp(M3Tokens.ShapeRole.SMALL.radiusDp));
        card.setCardElevation(0f);
        card.setUseCompatPadding(false);
        card.setClickable(false);
        card.setFocusable(false);
        card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        card.setVisibility(View.GONE);
        scanReadout = card;

        final LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(12), dp(7), dp(12), dp(8));
        card.addView(block, new android.widget.FrameLayout.LayoutParams(-2, -2));

        // A bare addView in a vertical LinearLayout takes MATCH_PARENT width,
        // so the caption used to be measured against the narrower RGB line and
        // clipped at 390 dp. Wrapping it makes the caption the block's widest
        // child and gives the readout one stable width while the numbers change.
        final TextView caption = m3Label("SCAN INSPECT · CENTRE ROW",
                M3Tokens.TypeRole.LABEL_SMALL, cardMuted, tfBody);
        caption.setSingleLine(true);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        block.addView(caption, new LinearLayout.LayoutParams(-2, -2));

        scanReadoutRgb = m3Label("R \u2014  G \u2014  B \u2014",
                M3Tokens.TypeRole.BODY_SMALL, cardText, tfBody);
        scanReadoutRgb.setSingleLine(true);
        scanReadoutRgb.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        final LinearLayout.LayoutParams rgbLp = new LinearLayout.LayoutParams(-2, -2);
        rgbLp.topMargin = dp(3);
        block.addView(scanReadoutRgb, rgbLp);

        scanReadoutLambda = m3Label("\u03bb \u2014",
                M3Tokens.TypeRole.BODY_SMALL, cardText, tfDisplay);
        scanReadoutLambda.setSingleLine(true);
        scanReadoutLambda.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        block.addView(scanReadoutLambda, new LinearLayout.LayoutParams(-2, -2));

        android.widget.FrameLayout.LayoutParams readoutLp =
                new android.widget.FrameLayout.LayoutParams(
                        -2, -2, android.view.Gravity.BOTTOM | android.view.Gravity.START);
        readoutLp.leftMargin = dp(14);
        // Clear of the 62 dp shelf handle and its 12 dp margin.
        readoutLp.bottomMargin = dp(84);
        stage.addView(card, readoutLp);
        updateScanToggleVisual(false);
    }

    /** Turn Scan inspect on or off. It never renders and never edits a Look. */
    private void toggleScan() {
        if (scanBitmap() == null) {
            showSnackbar("先导入一张照片");
            return;
        }
        scanVisible = !scanVisible;
        syncScanAvailability();
        if (scanVisible && scanOverlay != null) scanOverlay.requestFocus();
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
    private Bitmap scanBitmap() {
        if (stageMode != 1) return null;
        return showingOriginal ? beforeBitmap : lastRenderedBitmap;
    }

    /** Put the overlay, its readout, and the curve mark on one state. */
    private void syncScanAvailability() {
        if (scanOverlay == null) return;
        final Bitmap bitmap = scanBitmap();
        final boolean available = bitmap != null;
        // Put the Activity's fraction onto the overlay first: a null bitmap
        // still reports, and must not carry the previous frame's position back.
        scanOverlay.setFraction(scanFraction);
        scanOverlay.setPreviewBitmap(bitmap);
        final boolean show = scanVisible && available;
        scanOverlay.setScanVisible(show);
        if (scanReadout != null) {
            scanReadout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        updateScanToggleVisual(available);
    }

    /** The toggle's chip state: enabled only when there is a preview to read. */
    private void updateScanToggleVisual(boolean enabled) {
        if (scanToggleChip == null) return;
        scanToggleChip.setChecked(scanVisible);
        scanToggleChip.setEnabled(enabled);
        scanToggleChip.setAlpha(enabled ? 1f : M3Tokens.DISABLED_CONTENT_ALPHA);
        // The empty stage has no preview to inspect; leaving the pill visible
        // lets it collide with the central import button at 2.0× text.
        scanToggleChip.setVisibility(enabled || (scanVisible && sourceUri != null)
                ? View.VISIBLE : View.GONE);
        final String state = scanVisible
                ? (enabled ? "扫描检视已开启，点击关闭"
                        : "扫描检视已开启，等待预览")
                : (enabled ? "扫描检视，点击开启"
                        : "扫描检视，需先导入照片");
        scanToggleChip.setContentDescription(state);
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
        if (currentFrame < 0 || currentFrame >= frames.size()) return;
        final Frame frame = frames.get(currentFrame);
        frame.scanFraction = scanFraction;
        frame.scanVisible = scanVisible;
    }

    private void restoreScanStateFromFrame(Frame frame) {
        scanFraction = frame == null ? ScanSample.DEFAULT_FRACTION : frame.scanFraction;
        scanVisible = frame != null && frame.scanVisible;
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
        bar.setBackgroundColor(m3Surface());
        sourceBar = bar;
        bar.addView(hairline(m3OutlineVariant()),
                new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(4), 0);
        sourceRow = row;

        TextView add = m3Label("\uff0b", M3Tokens.TypeRole.TITLE_LARGE,
                m3Primary(), tfDisplay);
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

        TextView fold = m3Label("\u2039", M3Tokens.TypeRole.TITLE_SMALL,
                m3OnSurfaceVariant(), tfHan);
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

        TextView chevron = label("\u203a", 14, palette.color(Palette.Role.STAGE_INK), tfDisplay);
        chevron.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        chevron.setPadding(0, 0, dp(5), 0);
        chevron.setShadowLayer(dp(2), 0, 0, palette.color(Palette.Role.STAGE_SHADOW));
        handle.addView(chevron, new android.widget.FrameLayout.LayoutParams(card, card));

        handle.setFocusable(true);
        handle.setContentDescription("展开胶片条");
        handle.setOnClickListener(v -> setShelfCollapsed(false));
        return handle;
    }

    private Look captureLook() {
        return captureLook(false);
    }

    /**
     * Capture the live controls into the Frame's Look. A save from an existing
     * Frame starts from a detached copy of that Frame's stored Look, so a field
     * this build does not yet expose is carried through instead of being reset
     * by a Frame switch. A brand-new photograph starts from a fresh Look and
     * inherits nothing. Nothing is clamped on the way in.
     */
    private Look captureLook(boolean preserveHidden) {
        final Look look = preserveHidden
                && currentFrame >= 0 && currentFrame < frames.size()
                && frames.get(currentFrame).look != null
                ? frames.get(currentFrame).look.copy()
                : new Look();
        look.filmStock = filmStock;
        look.printStock = printStock;
        look.filmFormatMm = filmFormatMm;
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            look.set(param, optics.get(param));
        }
        look.cameraFilter = optics.cameraFilter;
        look.sceneNorm = optics.sceneNorm;
        look.view = optics.view.copy();
        look.geometryTurns = geometryTurns;
        look.geometryRotation = geometryRotation;
        look.geometryCropX = geometryCropX;
        look.geometryCropY = geometryCropY;
        look.geometryCropW = geometryCropW;
        look.geometryCropH = geometryCropH;
        look.geometryFlipH = geometryFlipH;
        look.geometryFlipV = geometryFlipV;
        look.overscanOn = overscanOn;
        look.overscanFormat = overscanFormat;
        look.dateOn = dateOn;
        look.edgeTextOn = edgeTextOn;
        look.edgeMode = edgeMode;
        look.edgeGate = edgeGate;
        look.edgeHoles = edgeHoles;
        look.edgeCarrier = edgeCarrier;
        look.edgeFog = edgeFog;
        look.edgeLeaks = edgeLeaks;
        look.frameNumber = frameNumber;
        look.edgeFNumber = edgeFNumber;
        look.cameraSeed = cameraSeed;
        look.frameSeed = frameSeed;
        look.datePlacement = datePlacement;
        look.dateCorner = dateCorner;
        look.dateStyle = dateStyle;
        look.dateInsetX = dateInsetX;
        look.dateInsetY = dateInsetY;
        look.dateSize = dateSize;
        look.dateEv = dateEv;
        look.dataText = dataText;
        look.dateTextB = dateTextB;
        // Only a *typed* text is part of the Look; the EXIF default stays with
        // the Frame's file, so a Frame with no typed date keeps following it.
        look.dateText = dateText == null ? "" : dateText;
        return look;
    }

    /** Put a frame's own look back, controls included: a slider that says 1.00
     *  while the render used 1.40 is worse than no slider. The stored value is
     *  restored as-is; the slider only clamps the thumb it can reach, so a
     *  glare above its 12.0 cap still reaches the engine. */
    private void applyLook(Look look) {
        if (look == null) return;
        filmStock = look.filmStock;
        printStock = look.printStock;
        filmFormatMm = look.filmFormatMm;
        for (OpticsDelta.Param param : OpticsDelta.Param.values()) {
            optics.set(param, look.get(param));
        }
        // Unknown choice ids stay on the Look so a later save does not erase
        // them; the page shows the documented fallback until the user picks.
        optics.cameraFilter = look.cameraFilter;
        optics.sceneNorm = look.sceneNorm;
        optics.view = look.view.copy();
        geometryTurns = look.geometryTurns;
        geometryRotation = look.geometryRotation;
        geometryCropX = look.geometryCropX;
        geometryCropY = look.geometryCropY;
        geometryCropW = look.geometryCropW;
        geometryCropH = look.geometryCropH;
        geometryFlipH = look.geometryFlipH;
        geometryFlipV = look.geometryFlipV;
        overscanOn = look.overscanOn;
        overscanFormat = look.overscanFormat;
        dateOn = look.dateOn;
        edgeTextOn = look.edgeTextOn;
        edgeMode = look.edgeMode;
        edgeGate = look.edgeGate;
        edgeHoles = look.edgeHoles;
        edgeCarrier = look.edgeCarrier;
        edgeFog = look.edgeFog;
        edgeLeaks = look.edgeLeaks;
        frameNumber = look.frameNumber;
        edgeFNumber = look.edgeFNumber;
        cameraSeed = look.cameraSeed;
        frameSeed = look.frameSeed;
        datePlacement = look.datePlacement;
        dateCorner = look.dateCorner;
        dateStyle = look.dateStyle;
        dateInsetX = look.dateInsetX;
        dateInsetY = look.dateInsetY;
        dateSize = look.dateSize;
        dateEv = look.dateEv;
        dataText = look.dataText == null ? "" : look.dataText;
        dateTextB = look.dateTextB == null ? "" : look.dateTextB;
        dateText = look.dateText == null || look.dateText.isEmpty() ? null : look.dateText;
        final GateScale.Gate pair = GateScale.pair(overscanFormat, filmFormatMm);
        // One piece of film geometry: a stored Gate and Scale that disagree are
        // pulled onto the Gate's measured edge before either control can draw.
        overscanFormat = pair.id;
        filmFormatMm = pair.longEdgeMm;
        if (opticsSlider != null) bindActiveOptics();
        syncOpticsChoices();
        syncToggleRows();
        syncDateDetail();
        if (edgeValue != null) edgeValue.setText(pair.label);
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
        if (currentFrame < 0 || currentFrame >= frames.size()) return;
        frames.get(currentFrame).look = captureLook(true);
    }

    private void updateShelfHandle() {
        if (shelfHandleImage == null) return;
        Bitmap thumb = (currentFrame >= 0 && currentFrame < frames.size())
                ? frames.get(currentFrame).thumb : null;
        shelfHandleImage.setImageBitmap(thumb);
    }

    /** Fold or unfold the photograph strip. Unfolded it costs 62 dp above the
     *  panels; folded it costs 20, and the canvas takes the difference -- the
     *  stage is the weighted child of the column, so nothing else has to move. */
    private void setShelfCollapsed(boolean collapsed) {
        if (shelfCollapsed == collapsed) return;
        // Nothing to fold into: with no photographs the handle would not be
        // drawn, and ＋ would go with the row -- a one-way trip.
        if (collapsed && frames.isEmpty()) return;
        shelfCollapsed = collapsed;
        updateShelfVisibility();
        if (collapsed) updateShelfHandle();
    }

    /** The strip is furniture: it exists only when there is something on it.
     *  Before the first import it is not folded, it is absent -- the canvas's
     *  own 选择照片 button is the way in, and a 62 dp row of nothing above the
     *  panels is just a gap. */
    private void updateShelfVisibility() {
        if (sourceBar == null) return;
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

    /** File the frame that just finished importing, once. */
    private void rememberFrame(Uri uri) {
        if (uri == null) return;
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
            rebuildFrameChips();
            return;
        }
        final Frame frame = new Frame(uri, isRawFile, name);
        frame.look = captureLook();
        frame.scanFraction = scanFraction;
        frame.scanVisible = scanVisible;
        if (beforeBitmap != null) {
            try {
                final int w = Math.max(1, dp(42));
                final int h = Math.max(1, Math.round(
                        w * beforeBitmap.getHeight() / (float) beforeBitmap.getWidth()));
                frame.thumb = Bitmap.createScaledBitmap(beforeBitmap, w, h, true);
            } catch (Throwable ignored) { }
        }
        frames.add(frame);
        currentFrame = frames.size() - 1;
        rebuildFrameChips();
    }

    /** One chip per opened frame, newest at the right, the open one ringed. */
    private void rebuildFrameChips() {
        if (thumbStrip == null) return;
        thumbStrip.removeAllViews();
        for (int i = 0; i < frames.size(); ++i) {
            final int index = i;
            final Frame frame = frames.get(i);
            final boolean active = index == currentFrame;
            android.widget.FrameLayout box = new android.widget.FrameLayout(this);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(m3Surface());
            bg.setStroke(dp(active ? 2 : 1),
                    active ? m3Primary() : m3OutlineVariant());
            box.setBackground(bg);
            box.setPadding(dp(3), dp(3), dp(3), dp(3));
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (frame.thumb != null) img.setImageBitmap(frame.thumb);
            box.addView(img, new android.widget.FrameLayout.LayoutParams(-1, -1));
            box.setOnClickListener(v -> reopenFrame(index));
            box.setOnLongClickListener(v -> {
                showActionSheet("删除这张照片？",
                        (frame.name == null ? "照片" : frame.name) + "\n\n只从这条栏里移除，文件本身不动。",
                        new String[]{"删除", "取消"}, which -> {
                            if (which == 0) removeFrame(index);
                        });
                return true;
            });
            box.setFocusable(true);
            box.setSelected(active);
            box.setContentDescription((active ? "当前照片，" : "")
                    + (frame.name == null ? "照片" : frame.name));
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
        if (index < 0 || index >= frames.size()) return;
        frames.remove(index);
        if (index == currentFrame) {
            currentFrame = -1;
            lastRenderedBitmap = null;
            scanVisible = false;
            scanFraction = ScanSample.DEFAULT_FRACTION;
            if (compare != null) compare.clear();
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
        if (index < 0 || index >= frames.size()) return;
        if (index == currentFrame && session != null) return;
        final Frame frame = frames.get(index);
        saveLookIntoFrames();
        saveScanStateIntoFrame();
        currentFrame = index;
        reopeningFrame = true;
        suppressBudgetDialog = true;
        rebuildFrameChips();
        loadImage(frame.uri);
    }

    private void setOriginalVisual(boolean on) {
        if (originalAction == null) return;
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
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(largeText() ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setMinimumHeight(dp(SECTION_HEAD_HEIGHT_DP));
        TextView titleView = mixedM3Label(title, M3Tokens.TypeRole.TITLE_SMALL, m3OnSurface());
        titleView.setMaxLines(2);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        head.addView(titleView, new LinearLayout.LayoutParams(
                largeText() ? -1 : 0, -2, largeText() ? 0f : 1f));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.END);
        if (actionLabel != null && action != null) {
            MaterialButton actionView = m3TextButton(actionLabel, actionLabel,
                    m3Primary(), action);
            actions.addView(actionView, new LinearLayout.LayoutParams(-2, dp(48)));
        }
        MaterialButton resetView = m3TextButton("重置当前页", "重置当前页",
                m3OnSurfaceVariant(), reset);
        actions.addView(resetView, new LinearLayout.LayoutParams(-2, dp(48)));
        head.addView(actions, largeText()
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(-2, -2));
        return head;
    }

    private View buildFilmPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.addView(sectionHead("FILM STOCK / 胶片",
                        "全部胶片", this::showFilmPicker, this::resetFilmPanel),
                new LinearLayout.LayoutParams(-1, -2));

        HorizontalScrollView brandScroll = new HorizontalScrollView(this);
        brandScroll.setHorizontalScrollBarEnabled(false);
        brandScroll.setClipToPadding(false);
        brandScroll.setFillViewport(false);
        brandRow = new ChipGroup(this);
        brandRow.setSingleLine(true);
        brandRow.setSingleSelection(true);
        brandRow.setSelectionRequired(true);
        brandRow.setChipSpacingHorizontal(dp(4));
        brandRow.setChipSpacingVertical(0);
        brandScroll.addView(brandRow, new android.widget.FrameLayout.LayoutParams(-2, -2));
        panel.addView(brandScroll, new LinearLayout.LayoutParams(-1, dp(MIN_TOUCH_DP)));

        filmChipScroll = new HorizontalScrollView(this);
        filmChipScroll.setHorizontalScrollBarEnabled(false);
        filmChipScroll.setClipToPadding(false);
        filmChipScroll.setFillViewport(false);
        filmChipRow = new LinearLayout(this);
        filmChipRow.setOrientation(LinearLayout.HORIZONTAL);
        filmChipRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        filmChipScroll.addView(filmChipRow, new android.widget.FrameLayout.LayoutParams(-2, -1));
        panel.addView(filmChipScroll, new LinearLayout.LayoutParams(-1, dp(FILM_CARD_HEIGHT_DP)));

        // The plot is a custom view inside a real M3 card: the surface, radius,
        // and outline are Material's; ticket 08 owns the plot's geometry.
        filmCurveCard = new MaterialCardView(this);
        filmCurveCard.setCardElevation(0f);
        filmCurveCard.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
        filmCurveCard.setCardBackgroundColor(m3Role(
                com.google.android.material.R.attr.colorSurfaceContainer,
                Palette.Role.SURFACE_ELEVATED));
        filmCurveCard.setStrokeWidth(dp(1));
        filmCurveCard.setStrokeColor(m3Role(
                com.google.android.material.R.attr.colorOutlineVariant,
                Palette.Role.LINE));
        filmCurve = new SpectrumCurveView(this, palette, tfBody);
        filmCurveCard.addView(filmCurve, new android.widget.FrameLayout.LayoutParams(-1, -1));
        panel.addView(filmCurveCard, new LinearLayout.LayoutParams(-1, dp(FILM_CURVE_HEIGHT_DP)));

        filmMetricsRow = new LinearLayout(this);
        filmMetricsRow.setOrientation(LinearLayout.HORIZONTAL);
        filmMetricsRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        panel.addView(filmMetricsRow,
                new LinearLayout.LayoutParams(-1, -2));
        filmMetricsRow.setMinimumHeight(dp(FILM_METRICS_HEIGHT_DP));

        filmNote = m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                m3OnSurfaceVariant(), tfBody);
        filmNote.setSingleLine(false);
        filmNote.setMaxLines(2);
        filmNote.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(filmNote, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout paperRow = new LinearLayout(this);
        paperRow.setOrientation(LinearLayout.HORIZONTAL);
        paperRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        paperRow.addView(mixedM3Label("相纸 / 拷贝片",
                M3Tokens.TypeRole.LABEL_LARGE, m3OnSurfaceVariant()));
        MaterialCardView paperChoice = new MaterialCardView(this);
        paperChoice.setCardElevation(0f);
        paperChoice.setRadius(dp(M3Tokens.ShapeRole.SMALL.radiusDp));
        paperChoice.setCardBackgroundColor(m3Role(
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Palette.Role.SURFACE_ELEVATED));
        paperChoice.setStrokeWidth(0);
        paperChoice.setClickable(true);
        paperChoice.setFocusable(true);
        paperChoice.setContentDescription("选择相纸 / 拷贝片");
        paperChoice.setRippleColor(ColorStateList.valueOf(
                withOpacity(m3Primary(), M3Tokens.StateLayer.PRESSED.opacity)));
        paperChoice.setOnClickListener(v -> showPaperPicker());
        LinearLayout paperInner = new LinearLayout(this);
        paperInner.setOrientation(LinearLayout.HORIZONTAL);
        paperInner.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        paperInner.setPadding(dp(12), 0, dp(10), 0);
        paperValue = m3Label("Kodak Portra Endura",
                M3Tokens.TypeRole.BODY_MEDIUM, m3OnSurface(), tfHan);
        paperValue.setSingleLine(true);
        paperValue.setEllipsize(android.text.TextUtils.TruncateAt.END);
        paperValue.setGravity(android.view.Gravity.END);
        paperInner.addView(paperValue, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView paperArrow = m3Label("↓", M3Tokens.TypeRole.LABEL_LARGE,
                m3Primary(), tfBody);
        LinearLayout.LayoutParams paperArrowLp = new LinearLayout.LayoutParams(-2, -2);
        paperArrowLp.leftMargin = dp(7);
        paperInner.addView(paperArrow, paperArrowLp);
        paperChoice.addView(paperInner,
                new android.widget.FrameLayout.LayoutParams(-1, -1));
        paperRow.addView(paperChoice,
                new LinearLayout.LayoutParams(0, -2, 1f));
        paperChoice.setMinimumHeight(dp(PAPER_ROW_HEIGHT_DP));
        panel.addView(paperRow,
                new LinearLayout.LayoutParams(-1, -2));
        paperRow.setMinimumHeight(dp(PAPER_ROW_HEIGHT_DP));
        return panel;
    }

    /** A film record currently on the strip, or null when none is chosen. */
    private FilmStock currentFilmStock() {
        return filmStock == null ? null : filmStocks.get(filmStock);
    }

    /** Rebuild the chips, curve, measured values, and note from the Look. */
    private void refreshFilmPanel() {
        refreshFilmPanel(false);
    }

    /** A brand change starts a new shelf; other refreshes keep the chip scroll. */
    private void refreshFilmPanel(boolean resetScroll) {
        if (filmChipRow == null) return;
        final int keepScroll = resetScroll || filmChipScroll == null
                ? 0 : filmChipScroll.getScrollX();
        refreshBrandRow();
        filmChipRow.removeAllViews();
        addFilmChip("original", "原图", "ORIGINAL", showingOriginal);

        // The active stock is the one the curve and the readout describe. If
        // another brand is selected, FilmBrand.shelf pins it first so the
        // shelf never hides the Look that is still in force.
        final String activeId = filmStock == null || showingOriginal ? null : filmStock;
        for (String id : FilmBrand.shelf(filmIds, brandFilter, activeId)) {
            final int i = filmIds.indexOf(id);
            if (i < 0) continue;
            addFilmChip(id, StockNames.shelf(filmLabels.get(i)),
                    chipSubtitle(i, filmStocks.get(id)),
                    activeId != null && id.equals(activeId));
        }
        if (filmChipScroll != null) filmChipScroll.post(() -> filmChipScroll.scrollTo(keepScroll, 0));

        final FilmStock stock = currentFilmStock();
        final SpectrumCurve nextCurve =
                stock == null ? SpectrumCurve.unavailable() : stock.curve;
        // A new curve reveals once. A render-state refresh re-sends the same
        // object, and a theme rebuild finds it already revealed, so neither
        // replays the plotter. Reduced motion shortens the reveal to zero.
        final boolean revealNow = nextCurve.available() && nextCurve != revealedCurve;
        filmCurve.setCurve(nextCurve,
                stock == null ? "" : stock.seriesLabel,
                filmStock == null ? "NO FILM STOCK SELECTED" : SpectrumCurve.UNAVAILABLE_LABEL,
                revealNow ? motionDuration(FILM_CURVE_REVEAL_MS) : 0);
        if (revealNow) revealedCurve = nextCurve;
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
        if (brandRow == null) return;
        brandRow.removeAllViews();
        for (String id : FilmBrand.order()) {
            final int count = FilmBrand.count(filmIds, id);
            if (count == 0 && !FilmBrand.ALL.equals(id)) continue;
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
            final int chipSurface = m3Role(
                    com.google.android.material.R.attr.colorSurfaceContainerLow,
                    Palette.Role.SURFACE_ELEVATED);
            final int chipSelected = m3Role(
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    Palette.Role.ACCENT_SOFT);
            final int chipText = m3Role(
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    Palette.Role.MUTED_INK);
            final int chipTextSelected = m3Role(
                    com.google.android.material.R.attr.colorOnSecondaryContainer,
                    Palette.Role.ACCENT);
            final int chipOutline = m3Role(
                    com.google.android.material.R.attr.colorOutlineVariant,
                    Palette.Role.LINE);
            chip.setChipBackgroundColor(checkedColors(chipSelected, chipSurface));
            chip.setChipStrokeWidth(Math.max(1, dp(1)));
            chip.setChipStrokeColor(checkedColors(chipSelected, chipOutline));
            chip.setTextColor(checkedColors(chipTextSelected, chipText));
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setChecked(active);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setText(mixedText(FilmBrand.label(id) + " " + count));
            chip.setContentDescription(FilmBrand.label(id) + "，" + count + " 支胶片"
                    + (active ? "，已选中" : "，筛选"));
            chip.setOnClickListener(v -> {
                if (id.equals(brandFilter)) return;
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
            final String iso = stock.info.get("iso");
            if (iso != null) return "ISO " + iso;
            final String type = stock.info.get("type");
            if (type != null) return type.toUpperCase(java.util.Locale.US);
        }
        if (index >= 0 && index < filmTypes.size()
                && "positive".equals(filmTypes.get(index))) return "REVERSAL · SCAN";
        return "FILM";
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
        final int checkedBg = m3Role(com.google.android.material.R.attr.colorSecondaryContainer,
                Palette.Role.ACCENT_SOFT);
        final int restBg = m3Role(com.google.android.material.R.attr.colorSurfaceContainerHighest,
                Palette.Role.SURFACE_ELEVATED);
        final int disabledBg = m3Role(com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE);
        final int checkedInk = m3Role(com.google.android.material.R.attr.colorOnSecondaryContainer,
                Palette.Role.ON_ACCENT);
        final int restInk = m3Role(com.google.android.material.R.attr.colorOnSurface,
                Palette.Role.INK);
        final int restMuted = m3Role(com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
        final int disabledInk = withOpacity(restMuted, M3Tokens.DISABLED_CONTENT_ALPHA);
        final android.content.res.ColorStateList ink = new android.content.res.ColorStateList(
                states, new int[]{checkedInk, restInk, disabledInk});
        final android.content.res.ColorStateList muted = new android.content.res.ColorStateList(
                states, new int[]{withOpacity(checkedInk, M3Tokens.MEDIUM_EMPHASIS_ALPHA),
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
                m3Role(com.google.android.material.R.attr.colorOutline, Palette.Role.ACCENT),
                m3Role(com.google.android.material.R.attr.colorOutlineVariant, Palette.Role.LINE),
                withOpacity(restMuted, M3Tokens.StateLayer.DRAGGED.opacity)}));
        card.setCheckedIconSize(dp(16));
        card.setCheckedIconMargin(dp(4));
        card.setCheckedIconGravity(MaterialCardView.CHECKED_ICON_GRAVITY_TOP_END);
        card.setCheckedIconTint(android.content.res.ColorStateList.valueOf(checkedInk));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(android.view.Gravity.CENTER_VERTICAL);
        content.setPadding(dp(8), dp(4), dp(24), dp(4));

        TextView name = m3Label(title, M3Tokens.TypeRole.LABEL_MEDIUM, restInk, tfHan);
        name.setTextColor(ink);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        content.addView(name, new LinearLayout.LayoutParams(-1, -2));

        TextView kind = m3Label(subtitle, M3Tokens.TypeRole.LABEL_SMALL, restMuted, tfBody);
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
            if ("original".equals(id)) toggleOriginal();
            else pickFilmChip(id);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(FILM_CARD_WIDTH_DP + (largeText() ? 20 : 0)), dp(FILM_CARD_HEIGHT_DP));
        lp.rightMargin = dp(FILM_CARD_GAP_DP);
        filmChipRow.addView(card, lp);
    }

    /** A stock chip is a Look choice: same path as any other stock change. */
    private void pickFilmChip(String id) {
        final int index = filmIds.indexOf(id);
        if (index < 0) return;
        if (id.equals(filmStock) && !showingOriginal) return;
        filmStock = id;
        // Upstream's `selectFilmStock` rule: the Film the user picks brings its
        // own paper. An explicit paper chosen afterwards stays until the next
        // Film selection; a Frame restore keeps its stored Film/paper pair.
        final FilmStock chosen = filmStocks.get(id);
        printStock = PrintTarget.choose(printStock,
                chosen == null ? null : chosen.targetPrint, printIds);
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
        if (filmMetricsRow == null) return;
        filmMetricsRow.removeAllViews();
        if (stock == null) {
            final String label = filmStock == null ? "—" : stockLabel(
                    filmStock, filmIds, filmLabels);
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
        final String value = stock.info.get(key);
        if (value == null || value.isEmpty()) return;
        rows.add(new String[]{caption, value.toUpperCase(java.util.Locale.US)});
    }

    private void addMetricCell(String caption, String value) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(dp(1), dp(2), dp(6), dp(1));
        TextView head = m3Label(caption, M3Tokens.TypeRole.LABEL_SMALL,
                m3OnSurfaceVariant(), tfBody);
        head.setSingleLine(true);
        head.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.addView(head, new LinearLayout.LayoutParams(-1, -2));
        TextView text = m3Label(value, M3Tokens.TypeRole.LABEL_MEDIUM,
                m3OnSurface(), tfHan);
        text.setSingleLine(true);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.addView(text, new LinearLayout.LayoutParams(-1, -2));
        filmMetricsRow.addView(cell, new LinearLayout.LayoutParams(0, -1, 1f));
    }

    /** A short factual line from the profile; a missing field is not invented. */
    private String filmNoteText(FilmStock stock) {
        if (filmStock == null) return "SELECT A STOCK TO SEE ITS MEASURED RESPONSE.";
        if (stock == null) {
            return "CATALOGUE RECORD MISSING FOR " + filmStock.toUpperCase(java.util.Locale.US);
        }
        final String description = stock.info.get("description");
        if (isPositiveFilm(filmStock)) {
            final String scan = "REVERSAL · DIRECT SCAN · PAPER NOT USED";
            return description != null && !description.isEmpty()
                    ? scan + " · " + description : scan;
        }
        if (description != null && !description.isEmpty()) return description;
        final java.util.List<String> parts = new java.util.ArrayList<>();
        // The same declaration the Film choice applies, so the note cannot
        // claim a paper the Look would not move to.
        final String target = stock.targetPrint;
        if (target != null && !target.isEmpty()) {
            parts.add("TARGET " + stockLabel(target, printIds, printLabels)
                    .toUpperCase(java.util.Locale.US));
        }
        final String antihalation = stock.info.get("antihalation");
        if (antihalation != null && !antihalation.isEmpty()) {
            parts.add("ANTIHALATION " + antihalation.toUpperCase(java.util.Locale.US));
        }
        if (parts.isEmpty()) {
            final String support = stock.info.get("support");
            final String stage = stock.info.get("stage");
            if (support != null) parts.add(support.toUpperCase(java.util.Locale.US));
            if (stage != null) parts.add(stage.toUpperCase(java.util.Locale.US));
        }
        if (parts.isEmpty()) return "NO STOCK NOTES SHIPPED.";
        final StringBuilder note = new StringBuilder();
        for (String part : parts) {
            if (note.length() > 0) note.append(" · ");
            note.append(part);
        }
        return note.toString();
    }

    private String curveDescription(FilmStock stock) {
        if (filmStock == null) return "光谱：未选择胶片";
        if (stock == null || !stock.curve.available()) {
            return "光谱：所选胶片没有随附采样，曲线不可用";
        }
        return "光谱：" + stock.seriesLabel + "，" + stock.curve.sampleCount()
                + " 个采样，" + Math.round(stock.curve.minNm()) + " 至 "
                + Math.round(stock.curve.maxNm()) + " 纳米";
    }

    private void refreshPaperLabel() {
        if (paperValue != null) {
            // The picker value is the full catalogue name; only the Readout
            // strip shortens it. Both read the same printLabels record.
            paperValue.setText(fullStockLabel(printStock, printIds, printLabels));
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
        final int filmIndex = filmStock == null ? -1 : filmIds.indexOf(filmStock);
        final int current = showingOriginal ? 0 : (filmIndex < 0 ? -1 : 1 + filmIndex);
        showChoiceSheet("选择胶片", "共 " + filmIds.size() + " 支，品牌标签只筛片架",
                names, current, picked -> {
                    if (picked == 0) {
                        if (!showingOriginal) toggleOriginal();
                        return;
                    }
                    final int index = picked - 1;
                    if (index >= 0 && index < filmIds.size()) {
                        pickFilmChip(filmIds.get(index));
                    }
                });
    }

    private void showPaperPicker() {
        if (printIds.isEmpty()) {
            showSnackbar("没有可用的相纸");
            return;
        }
        final int current = printIds.indexOf(printStock);
        showChoiceSheet("选择相纸 / 拷贝片", "当前 Look 会沿这张相纸显影",
                printLabels, current, picked -> {
                    if (picked < 0 || picked >= printIds.size()
                            || printStock.equals(printIds.get(picked))) return;
                    printStock = printIds.get(picked);
                    refreshPaperLabel();
                    showingOriginal = false;
                    onStockChanged();
                });
    }

    private LinearLayout buildOpticsPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        opticsPanel = panel;
        opticsPanel.setOnClickListener(v -> {
            if (!opticsUnlocked) showSnackbar(opticsLockReason());
        });
        panel.addView(buildOpticsHead(),
                new LinearLayout.LayoutParams(-1, -2));

        // The strip: one M3 chip per contract parameter, in the engine's own
        // order. It scrolls sideways instead of clipping, so 390 dp and large
        // text both keep every parameter reachable.
        opticsStrip = new LinearLayout(this);
        opticsStrip.setOrientation(LinearLayout.HORIZONTAL);
        opticsStrip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        opticsChips.clear();
        for (int i = 0; i < OpticsDelta.Param.values().length; ++i) {
            final int index = i;
            final OpticsDelta.Param spec = OpticsDelta.paramAt(i);
            final Chip chip = new Chip(this);
            styleOpticsChip(chip);
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setSingleLine(true);
            chip.setText(mixedText(spec.glyph + "  " + spec.label));
            chip.setContentDescription(spec.label + "，参数 " + (i + 1) + " / "
                    + OpticsDelta.Param.values().length);
            chip.setMinimumHeight(dp(MIN_TOUCH_DP));
            chip.setOnClickListener(v -> {
                if (!opticsUnlocked) {
                    showSnackbar(opticsLockReason());
                    return;
                }
                selectOptics(index);
            });
            final LinearLayout.LayoutParams chipLp =
                    new LinearLayout.LayoutParams(-2, -2);
            chipLp.rightMargin = dp(6);
            opticsStrip.addView(chip, chipLp);
            opticsChips.add(chip);
        }
        opticsStripScroll = new HorizontalScrollView(this);
        opticsStripScroll.setHorizontalScrollBarEnabled(false);
        opticsStripScroll.setClipToPadding(false);
        opticsStripScroll.setFillViewport(false);
        opticsStripScroll.addView(opticsStrip,
                new android.widget.FrameLayout.LayoutParams(-2, -2));
        panel.addView(opticsStripScroll, new LinearLayout.LayoutParams(-1, -2));

        // The one active row: name, value, unit, display-only Δ, and reset.
        panel.addView(buildOpticsReadout(), new LinearLayout.LayoutParams(-1, -2));

        opticsSlider = new Slider(this);
        opticsSlider.setId(View.generateViewId());
        opticsSlider.setMinimumHeight(dp(SLIDER_HEIGHT_DP));
        // The head already shows the live value; a floating tooltip would be
        // clipped by the row and repeat it.
        opticsSlider.setLabelBehavior(
                com.google.android.material.slider.LabelFormatter.LABEL_GONE);
        opticsSlider.setLabelFormatter(value -> {
            final OpticsDelta.Param spec = activeOptics();
            return OpticsDelta.valueText(value) + " " + spec.unit;
        });
        opticsSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (syncingOptics) return;
            final OpticsDelta.Param spec = activeOptics();
            optics.set(spec, value);
            showActiveOptics(value);
            refreshReadoutStrip();
            // One render per completed gesture: a touch release commits at
            // once; keyboard, switch access, and TalkBack changes coalesce so
            // a held arrow key does not queue a render per key. The guard at
            // the top keeps a Look restore and a chip rebind silent.
            if (!trackingOpticsTouch) scheduleOpticsCommit(slider);
        });
        opticsSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(Slider slider) {
                trackingOpticsTouch = true;
                slider.removeCallbacks(opticsCommitRunnable);
            }
            @Override public void onStopTrackingTouch(Slider slider) {
                trackingOpticsTouch = false;
                slider.removeCallbacks(opticsCommitRunnable);
                commitOptics();
            }
        });
        panel.addView(opticsSlider,
                new LinearLayout.LayoutParams(-1, dp(SLIDER_HEIGHT_DP)));

        // The two choices the engine models separately from the 22 numbers.
        opticsFilterButton = opticsChoiceRow(panel, "镜头滤镜 / CAMERA FILTER",
                this::showCameraFilterPicker);
        opticsNormButton = opticsChoiceRow(panel, "保护基准 / PROTECTION NORM",
                this::showProtectionNormPicker);

        bindActiveOptics();
        syncOpticsChoices();
        return panel;
    }

    /**
     * The view-adjust layer as an M3 sheet: four category chips, then one
     * labelled slider per field. Values live in {@code optics.view}, so the
     * Frame switch and undo see them with the rest of the Look.
     */
    private void showViewAdjustSheet() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));

        root.addView(m3Label("视图调整 / VIEW", M3Tokens.TypeRole.TITLE_MEDIUM,
                m3Role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK),
                tfDisplay));

        final ChipGroup groups = new ChipGroup(this);
        groups.setSingleSelection(true);
        groups.setSelectionRequired(true);
        final LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);

        for (ViewAdjust.Group group : ViewAdjust.Group.values()) {
            final Chip chip = new Chip(this);
            styleOpticsChip(chip);
            chip.setText(mixedText(group.label + " / " + group.english));
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
     * Composition as an M3 sheet. The values are part of the Look and go
     * through the engine's geometry node; no bitmap transform is baked in.
     */
    private void showComposeSheet() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.addView(m3Label("构图 / COMPOSE", M3Tokens.TypeRole.TITLE_MEDIUM,
                m3OnSurface(), tfDisplay));

        final ChipGroup turns = new ChipGroup(this);
        turns.setSingleSelection(true);
        turns.setSelectionRequired(true);
        final String[] labels = {"0°", "90°", "180°", "270°"};
        for (int i = 0; i < labels.length; ++i) {
            final int quarter = i;
            final Chip chip = new Chip(this);
            styleOpticsChip(chip);
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setChecked(geometryTurns == quarter);
            chip.setMinimumHeight(dp(MIN_TOUCH_DP));
            chip.setOnClickListener(v -> {
                geometryTurns = quarter;
                renderImage();
            });
            turns.addView(chip);
        }
        root.addView(turns, new LinearLayout.LayoutParams(-1, -2));

        composeSlider(root, "旋转 / ROTATION", -45f, 45f, geometryRotation,
                v -> geometryRotation = v);
        composeSlider(root, "裁切 X / CROP X", 0f, 1f, geometryCropX,
                v -> geometryCropX = v);
        composeSlider(root, "裁切 Y / CROP Y", 0f, 1f, geometryCropY,
                v -> geometryCropY = v);
        composeSlider(root, "裁切宽 / CROP W", 0.05f, 1f, geometryCropW,
                v -> geometryCropW = v);
        composeSlider(root, "裁切高 / CROP H", 0.05f, 1f, geometryCropH,
                v -> geometryCropH = v);

        final MaterialSwitch flipH = new MaterialSwitch(this);
        flipH.setText(mixedText("水平翻转 / FLIP H"));
        flipH.setChecked(geometryFlipH);
        flipH.setOnCheckedChangeListener((b, checked) -> {
            geometryFlipH = checked;
            renderImage();
        });
        root.addView(flipH, new LinearLayout.LayoutParams(-1, dp(48)));

        final MaterialSwitch flipV = new MaterialSwitch(this);
        flipV.setText(mixedText("垂直翻转 / FLIP V"));
        flipV.setChecked(geometryFlipV);
        flipV.setOnCheckedChangeListener((b, checked) -> {
            geometryFlipV = checked;
            renderImage();
        });
        root.addView(flipV, new LinearLayout.LayoutParams(-1, dp(48)));

        final MaterialButton reset = actionButton(mixedText("重置构图"), "重置旋转、裁切和翻转");
        reset.setOnClickListener(v -> {
            geometryTurns = 0;
            geometryRotation = 0f;
            geometryCropX = geometryCropY = 0f;
            geometryCropW = geometryCropH = 1f;
            geometryFlipH = geometryFlipV = false;
            dialog.dismiss();
            renderImage();
        });
        root.addView(reset, new LinearLayout.LayoutParams(-2, dp(48)));

        dialog.setContentView(root);
        dialog.show();
    }

    private void composeSlider(LinearLayout parent, String label, float min, float max,
                               float value, java.util.function.Consumer<Float> apply) {
        final LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.addView(m3Label(label, M3Tokens.TypeRole.LABEL_LARGE, m3OnSurface(), tfHan),
                new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView readout = m3Label(String.format(java.util.Locale.US, "%.2f", value),
                M3Tokens.TypeRole.BODY_MEDIUM, m3Primary(), tfBody);
        head.addView(readout);
        parent.addView(head, new LinearLayout.LayoutParams(-1, -2));

        final Slider slider = new Slider(this);
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setStepSize(0.01f);
        slider.setValue(Math.max(min, Math.min(max, value)));
        slider.setLabelBehavior(com.google.android.material.slider.LabelFormatter.LABEL_GONE);
        slider.setContentDescription(label);
        slider.addOnChangeListener((s, v, fromUser) -> {
            apply.accept(v);
            readout.setText(String.format(java.util.Locale.US, "%.2f", v));
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(Slider s) { }
            @Override public void onStopTrackingTouch(Slider s) { renderImage(); }
        });
        parent.addView(slider, new LinearLayout.LayoutParams(-1, dp(SLIDER_HEIGHT_DP)));
    }

    private void populateViewFields(LinearLayout fields, ViewAdjust.Group group) {
        fields.removeAllViews();
        for (ViewAdjust.Field field : ViewAdjust.Field.values()) {
            if (field.group != group) continue;
            final LinearLayout block = new LinearLayout(this);
            block.setOrientation(LinearLayout.VERTICAL);
            block.setPadding(0, dp(10), 0, dp(4));

            final LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(android.view.Gravity.CENTER_VERTICAL);
            head.addView(m3Label(field.label, M3Tokens.TypeRole.LABEL_LARGE,
                    m3Role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK),
                    tfHan), new LinearLayout.LayoutParams(0, -2, 1f));
            final TextView value = m3Label(
                    ViewAdjust.valueText(field.get(optics.view), field.unit),
                    M3Tokens.TypeRole.BODY_MEDIUM,
                    m3Role(com.google.android.material.R.attr.colorPrimary, Palette.Role.ACCENT),
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
                if (!fromUser) return;
                viewAdjustHandler.removeCallbacks(viewAdjustCommit);
                viewAdjustHandler.postDelayed(viewAdjustCommit, 180);
            });
            slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override public void onStartTrackingTouch(Slider s) {
                    viewAdjustHandler.removeCallbacks(viewAdjustCommit);
                }
                @Override public void onStopTrackingTouch(Slider s) {
                    viewAdjustHandler.removeCallbacks(viewAdjustCommit);
                    renderImage();
                }
            });
            block.addView(slider, new LinearLayout.LayoutParams(-1, dp(SLIDER_HEIGHT_DP)));
            fields.addView(block, new LinearLayout.LayoutParams(-1, -2));
        }

        final MaterialButton reset = actionButton(mixedText("重置本页"), "重置当前视图调整分类");
        reset.setOnClickListener(v -> {
            optics.view.reset(group);
            populateViewFields(fields, group);
            renderImage();
        });
        fields.addView(reset, new LinearLayout.LayoutParams(-2, dp(48)));
    }

    /** Set a chip's M3 filter style from the current theme's roles. */
    private void styleOpticsChip(Chip chip) {        chip.setChipDrawable(ChipDrawable.createFromAttributes(this, null, 0,
                com.google.android.material.R.style.Widget_Material3_Chip_Filter));
        final int chipSurface = m3Role(
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE_ELEVATED);
        final int chipSelected = m3Role(
                com.google.android.material.R.attr.colorSecondaryContainer,
                Palette.Role.ACCENT_SOFT);
        final int chipText = m3Role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);
        final int chipTextSelected = m3Role(
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                Palette.Role.ACCENT);
        final int chipOutline = m3Role(
                com.google.android.material.R.attr.colorOutlineVariant,
                Palette.Role.LINE);
        chip.setChipBackgroundColor(checkedColors(chipSelected, chipSurface));
        chip.setChipStrokeWidth(Math.max(1, dp(1)));
        chip.setChipStrokeColor(checkedColors(chipSelected, chipOutline));
        chip.setTextColor(checkedColors(chipTextSelected, chipText));
        chip.setEnsureMinTouchTargetSize(true);
    }

    /**
     * The active parameter's readout, above the slider: name and reset on one
     * line, then value, unit, display-only Δ, and the contract's visible range.
     */
    private View buildOpticsReadout() {
        final LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(6), 0, 0);

        final LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        opticsName = m3Label("", M3Tokens.TypeRole.TITLE_SMALL,
                m3OnSurface(), tfHan);
        opticsName.setSingleLine(true);
        opticsName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ViewCompat.setAccessibilityHeading(opticsName, true);
        top.addView(opticsName, new LinearLayout.LayoutParams(0, -2, 1f));
        opticsReset = m3TextButton("重置", "重置当前参数",
                m3OnSurfaceVariant(), this::resetActiveOptics);
        top.addView(opticsReset, new LinearLayout.LayoutParams(-2, dp(48)));
        block.addView(top, new LinearLayout.LayoutParams(-1, -2));

        final LinearLayout values = new LinearLayout(this);
        values.setOrientation(LinearLayout.HORIZONTAL);
        values.setGravity(android.view.Gravity.BOTTOM);
        opticsValue = m3Label("", M3Tokens.TypeRole.HEADLINE_SMALL,
                m3Primary(), tfDisplay);
        values.addView(opticsValue);
        opticsUnit = m3Label("", M3Tokens.TypeRole.LABEL_MEDIUM,
                m3OnSurfaceVariant(), tfBody);
        final LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(-2, -2);
        unitLp.leftMargin = dp(4);
        unitLp.bottomMargin = dp(4);
        values.addView(opticsUnit, unitLp);
        opticsDelta = m3Label("", M3Tokens.TypeRole.LABEL_MEDIUM,
                m3OnSurfaceVariant(), tfDisplay);
        final LinearLayout.LayoutParams deltaLp = new LinearLayout.LayoutParams(-2, -2);
        deltaLp.leftMargin = dp(10);
        deltaLp.bottomMargin = dp(4);
        values.addView(opticsDelta, deltaLp);
        block.addView(values, new LinearLayout.LayoutParams(-1, -2));

        opticsRange = m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                m3OnSurfaceVariant(), tfHan);
        opticsRange.setSingleLine(false);
        block.addView(opticsRange, new LinearLayout.LayoutParams(-1, -2));
        return block;
    }

    /** A label + current choice + arrow, the same shape as the paper picker. */
    private MaterialButton opticsChoiceRow(LinearLayout parent, String title,
                                           Runnable open) {
        final boolean stacked = largeText();
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(MIN_TOUCH_DP));
        final TextView titleView = m3Label(title, M3Tokens.TypeRole.LABEL_MEDIUM,
                m3OnSurfaceVariant(), tfHan);
        row.addView(titleView, stacked
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -2, 1f));
        final MaterialButton button = m3TextButton("", title, m3Primary(), open);
        button.setGravity(stacked
                ? (android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL)
                : (android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL));
        button.setSingleLine(false);
        button.setMaxLines(2);
        row.addView(button, stacked
                ? new LinearLayout.LayoutParams(-1, dp(MIN_TOUCH_DP))
                : new LinearLayout.LayoutParams(-2, dp(MIN_TOUCH_DP)));
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private OpticsDelta.Param activeOptics() {
        return OpticsDelta.paramAt(activeOptics);
    }

    /** Bind the one slider and the readout to the strip's active parameter. */
    private void bindActiveOptics() {
        if (opticsSlider == null) return;
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

    /** Show a value in the head without moving the slider or committing. */
    private void showActiveOptics(float value) {
        final OpticsDelta.Param spec = activeOptics();
        final String number = OpticsDelta.valueText(value);
        final String delta = OpticsDelta.text(value, spec.defaultValue);
        final boolean moved = !spec.isDefault(value);
        final int color = moved ? m3Primary() : m3OnSurfaceVariant();
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
        if (spec.isDefault(optics.get(spec))) return;
        optics.reset(spec);
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
        for (OpticsDelta.CameraFilter filter : all) labels.add(filter.label);
        final int selected = OpticsDelta.CameraFilter.fromWire(optics.cameraFilter).ordinal();
        showChoiceSheet("镜头滤镜 / CAMERA FILTER",
                "Wratten 滤镜在镜头前；放大机的滤色片属于相纸阶段",
                labels, selected, picked -> {
                    if (picked < 0 || picked >= all.length) return;
                    final OpticsDelta.CameraFilter chosen = all[picked];
                    if (chosen.wire.equals(optics.cameraFilter)) return;
                    optics.cameraFilter = chosen.wire;
                    syncOpticsChoices();
                    commitOptics();
                });
    }

    private void showProtectionNormPicker() {
        if (!opticsUnlocked) {
            showSnackbar(opticsLockReason());
            return;
        }
        final OpticsDelta.ProtectionNorm[] all = OpticsDelta.ProtectionNorm.values();
        final List<String> labels = new ArrayList<>();
        for (OpticsDelta.ProtectionNorm norm : all) labels.add(norm.label);
        final int selected = OpticsDelta.ProtectionNorm.fromWire(optics.sceneNorm).ordinal();
        showChoiceSheet("保护基准 / PROTECTION NORM",
                "高光与暗部保护按这个场景测量计算，不改变保护量",
                labels, selected, picked -> {
                    if (picked < 0 || picked >= all.length) return;
                    final OpticsDelta.ProtectionNorm chosen = all[picked];
                    if (chosen.wire.equals(optics.sceneNorm)) return;
                    optics.sceneNorm = chosen.wire;
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
    private View buildOpticsHead() {
        final LinearLayout head = new LinearLayout(this);
        head.setOrientation(largeText() ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);

        final LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView titleView = mixedM3Label("OPTICS / 光学调整",
                M3Tokens.TypeRole.TITLE_SMALL, m3OnSurface());
        titles.addView(titleView, new LinearLayout.LayoutParams(-1, -2));

        opticsLockNote = m3Label("", M3Tokens.TypeRole.LABEL_SMALL,
                m3OnSurfaceVariant(), tfHan);
        opticsLockNote.setSingleLine(true);
        opticsLockNote.setEllipsize(android.text.TextUtils.TruncateAt.END);
        opticsLockNote.setVisibility(View.GONE);
        titles.addView(opticsLockNote, new LinearLayout.LayoutParams(-1, -2));
        head.addView(titles, largeText()
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -2, 1f));
        final MaterialButton viewAction = actionButton(
                mixedText("视图"), "打开视图调整：白平衡、亮度、颜色、分级");
        viewAction.setOnClickListener(v -> showViewAdjustSheet());
        head.addView(viewAction, new LinearLayout.LayoutParams(-2, dp(48)));
        return head;
    }

    /** The one sentence a locked Optics panel shows and repeats on tap. */
    private String opticsLockReason() {
        return session == null ? "先导入一张照片" : "先选择胶片或相纸，完成一次渲染";
    }

    private LinearLayout buildFormatPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.addView(sectionHead("PHYSICAL SCALE / 光学尺度", this::resetFormatPanel),
                new LinearLayout.LayoutParams(-1, -2));
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        formatCardStrip = new LinearLayout(this);
        formatCardStrip.setOrientation(LinearLayout.HORIZONTAL);
        formatCardStrip.setPadding(0, dp(10), 0, 0);
        formatScroll = scroll;
        scroll.addView(formatCardStrip, new android.widget.FrameLayout.LayoutParams(-2, dp(127)));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, dp(127)));

        final MaterialButton compose = actionButton(
                mixedText("构图 / COMPOSE"), "打开构图：旋转、翻转、裁切");
        compose.setOnClickListener(v -> showComposeSheet());
        panel.addView(compose, new LinearLayout.LayoutParams(-2, dp(48)));

        LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        foot.setGravity(android.view.Gravity.CENTER_VERTICAL);
        foot.addView(m3Label("片门与尺度联动 · 不裁切构图",
                M3Tokens.TypeRole.LABEL_LARGE, m3OnSurfaceVariant(), tfHan),
                new LinearLayout.LayoutParams(0, -2, 1f));
        formatFootValue = m3Label(mmText(GateScale.resolve(overscanFormat, filmFormatMm).longEdgeMm) + " mm",
                M3Tokens.TypeRole.LABEL_LARGE, m3Primary(), tfBody);
        foot.addView(formatFootValue);
        panel.addView(foot, new LinearLayout.LayoutParams(-1, dp(28)));
        panel.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        rebuildFormatCards();
        return panel;
    }

    private void rebuildFormatCards() {
        if (formatCardStrip == null) return;
        formatCardStrip.removeAllViews();
        formatCards.clear();
        if (edgeIds.isEmpty()) {
            TextView none = m3Label("这张长宽比没有可用片门",
                    M3Tokens.TypeRole.LABEL_LARGE, m3OnSurfaceVariant(), tfHan);
            LinearLayout.LayoutParams noneLp = new LinearLayout.LayoutParams(-2, dp(105));
            noneLp.leftMargin = dp(2);
            formatCardStrip.addView(none, noneLp);
            return;
        }
        for (int i = 0; i < edgeIds.size(); ++i) {
            final String gateId = edgeIds.get(i);
            final GateScale.Gate gate = GateScale.byId(gateId);
            if (gate == null) continue;
            final boolean active = gate.id.equals(overscanFormat);

            final int surface = m3Role(
                    com.google.android.material.R.attr.colorSurfaceContainerLow,
                    Palette.Role.SURFACE_ELEVATED);
            final int selectedSurface = m3Role(
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    Palette.Role.SURFACE_ELEVATED);
            final int outline = m3Role(
                    com.google.android.material.R.attr.colorOutlineVariant,
                    Palette.Role.LINE);
            final int selectedOutline = m3Role(
                    com.google.android.material.R.attr.colorPrimary,
                    Palette.Role.ACCENT);
            final int onSurface = m3Role(
                    com.google.android.material.R.attr.colorOnSurface,
                    Palette.Role.INK);
            final int onSelected = m3Role(
                    com.google.android.material.R.attr.colorOnSecondaryContainer,
                    Palette.Role.INK);
            final int onSurfaceVariant = m3Role(
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    Palette.Role.MUTED_INK);

            final MaterialCardView card = new MaterialCardView(this);
            card.setCardElevation(0f);
            card.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
            card.setStrokeWidth(dp(1));
            card.setCardBackgroundColor(checkedColors(selectedSurface, surface));
            card.setStrokeColor(checkedColors(selectedOutline, outline));
            card.setCheckable(true);
            card.setChecked(active);
            card.setCheckedIconResource(
                    com.google.android.material.R.drawable.mtrl_ic_check_mark);
            card.setCheckedIconTint(android.content.res.ColorStateList.valueOf(onSelected));
            card.setCheckedIconSize(dp(14));
            card.setCheckedIconMargin(dp(4));
            card.setCheckedIconGravity(MaterialCardView.CHECKED_ICON_GRAVITY_TOP_END);
            card.setClickable(true);
            card.setFocusable(true);
            card.setContentDescription(gate.label + "，" + mmText(gate.longEdgeMm) + " 毫米"
                    + (active ? "，已选" : ""));

            LinearLayout stack = new LinearLayout(this);
            stack.setOrientation(LinearLayout.VERTICAL);
            stack.setPadding(dp(9), dp(11), dp(8), dp(7));

            LinearLayout valueRow = new LinearLayout(this);
            valueRow.setOrientation(LinearLayout.HORIZONTAL);
            valueRow.setGravity(android.view.Gravity.BOTTOM);
            TextView value = m3Label(mmText(gate.longEdgeMm),
                    M3Tokens.TypeRole.TITLE_LARGE, active ? onSelected : onSurface, tfDisplay);
            valueRow.addView(value);
            TextView unit = m3Label("mm", M3Tokens.TypeRole.LABEL_SMALL,
                    onSurfaceVariant, tfBody);
            LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(-2, -2);
            unitLp.leftMargin = dp(2);
            unitLp.bottomMargin = dp(2);
            valueRow.addView(unit, unitLp);
            stack.addView(valueRow);

            TextView kind = m3Label(gate.panoramic() ? "宽幅" : "常规",
                    M3Tokens.TypeRole.LABEL_SMALL, onSurfaceVariant, tfHan);
            LinearLayout.LayoutParams kindLp = new LinearLayout.LayoutParams(-2, -2);
            kindLp.topMargin = dp(4);
            stack.addView(kind, kindLp);

            View spacer = new View(this);
            stack.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

            TextView name = m3Label(gate.label, M3Tokens.TypeRole.LABEL_MEDIUM,
                    active ? onSelected : onSurface, tfHan);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            stack.addView(name);

            card.addView(stack, new android.widget.FrameLayout.LayoutParams(-1, -1));

            card.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override public void onInitializeAccessibilityNodeInfo(
                        View host, android.view.accessibility.AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(android.widget.RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(active);
                }
            });
            card.setOnClickListener(v -> pickScale(gate.id));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(dp(84), dp(105));
            cardLp.rightMargin = dp(9);
            formatCardStrip.addView(card, cardLp);
            formatCards.add(card);
        }
    }

    /** A Scale card and its Gate are one choice, so one tap moves both. */
    private void pickScale(String gateId) {
        final GateScale.Gate gate = GateScale.byId(gateId);
        if (gate == null) return;
        if (gate.id.equals(overscanFormat)
                && Math.abs(gate.longEdgeMm - filmFormatMm) < 0.01f) {
            return;
        }
        overscanFormat = gate.id;
        filmFormatMm = gate.longEdgeMm;
        updateEdgeValue();
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(gate.longEdgeMm) + " mm");
        }
        rebuildFormatCards();
        final int index = edgeIds.indexOf(gate.id);
        if (index >= 0 && index < formatCards.size() && formatScroll != null) {
            final View target = formatCards.get(index);
            formatScroll.post(() -> formatScroll.smoothScrollTo(Math.max(0, target.getLeft() - dp(8)), 0));
        }
        syncRebatePage();
        onEdgeChanged();
    }

    private LinearLayout buildEdgePanel() {
        rebateRefreshers.clear();
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        edgeHome = buildEdgeHome();
        rebatePage = buildRebatePage();
        datePage = buildDatePage();
        root.addView(edgeHome, new LinearLayout.LayoutParams(-1, -2));
        root.addView(rebatePage, new LinearLayout.LayoutParams(-1, -2));
        root.addView(datePage, new LinearLayout.LayoutParams(-1, -2));
        openEdgePage(edgePageIndex);
        syncRebatePage();
        syncDatePage();
        return root;
    }

    /** The Edge landing page: one card per edge feature. */
    private LinearLayout buildEdgeHome() {
        final LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.addView(sectionHead("FRAME & IMPRINT / 边框", this::resetEdgePanel),
                new LinearLayout.LayoutParams(-1, -2));

        final HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        final LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(0, dp(4), 0, dp(4));

        final MaterialCardView rebateCard = edgeEntryCard("片边  REBATE",
                this::rebateSummary, "打开片边设置", () -> openEdgePage(0));
        strip.addView(rebateCard, edgeCardParams());
        final MaterialCardView dateCard = edgeEntryCard("日期水印  DATE BACK",
                this::dateSummary, "打开日期水印设置", () -> openEdgePage(1));
        strip.addView(dateCard, edgeCardParams());

        scroll.addView(strip, new android.widget.FrameLayout.LayoutParams(-2, -2));
        home.addView(scroll, new LinearLayout.LayoutParams(-1, -2));

        final TextView note = m3Label("片门与尺度联动；片边只在这张图可用的片门里选",
                M3Tokens.TypeRole.LABEL_SMALL, m3OnSurfaceVariant(), tfHan);
        final LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.topMargin = dp(8);
        home.addView(note, noteLp);
        return home;
    }

    private LinearLayout.LayoutParams edgeCardParams() {
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(176), -2);
        lp.rightMargin = dp(8);
        return lp;
    }

    /**
     * One Edge entry card: the feature name, a live summary, and a chevron.
     * The summary is a supplier so the card never keeps a second copy of the
     * Look; {@link #refreshEdgeCards()} repaints it after every change.
     */
    private MaterialCardView edgeEntryCard(String title,
                                           java.util.function.Supplier<String> summary,
                                           String description, Runnable onOpen) {
        final MaterialCardView card = new MaterialCardView(this);
        card.setCardElevation(0f);
        card.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(m3Role(
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE_ELEVATED));
        card.setStrokeColor(m3OutlineVariant());
        card.setClickable(true);
        card.setFocusable(true);

        final LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setPadding(dp(12), dp(10), dp(12), dp(10));

        final LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        final TextView name = mixedM3Label(title, M3Tokens.TypeRole.TITLE_SMALL, m3OnSurface());
        name.setMaxLines(2);
        titleRow.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        titleRow.addView(m3Label("›", M3Tokens.TypeRole.TITLE_SMALL, m3OnSurfaceVariant(), tfBody));
        stack.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        final TextView summaryView = m3Label(summary.get(),
                M3Tokens.TypeRole.LABEL_SMALL, m3OnSurfaceVariant(), tfHan);
        final LinearLayout.LayoutParams summaryLp = new LinearLayout.LayoutParams(-1, -2);
        summaryLp.topMargin = dp(6);
        stack.addView(summaryView, summaryLp);

        card.addView(stack, new android.widget.FrameLayout.LayoutParams(-1, -2));
        card.setOnClickListener(v -> onOpen.run());
        rebateRefreshers.add(() -> {
            final String text = summary.get();
            summaryView.setText(text);
            card.setContentDescription(description + "，" + text);
        });
        return card;
    }

    /**
     * The Rebate secondary page. Its switch owns whether the controls are
     * enabled; they stay visible and dimmed while off, so the page explains
     * what is available without pretending the controls work.
     */
    private LinearLayout buildRebatePage() {
        final LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setVisibility(View.GONE);
        page.addView(edgePageHead("片边 / REBATE"), new LinearLayout.LayoutParams(-1, -2));

        final int onSurface = m3Role(
                com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK);
        final int onSurfaceVariant = m3Role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);

        rebateSwitch = new MaterialSwitch(this);
        page.addView(switchRow("开启片边", null, rebateSwitch, () -> overscanOn, on -> {
            if (on && !GateScale.canHostRebate(sourceWidth, sourceHeight)) {
                showSnackbar("这张图的长宽比不对应任何片门");
                syncRebatePage();
                return;
            }
            overscanOn = on;
            syncRebatePage();
            commitRebate();
        }), new LinearLayout.LayoutParams(-1, dp(56)));

        rebateControls = new LinearLayout(this);
        rebateControls.setOrientation(LinearLayout.VERTICAL);

        rebateGateValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateGateCard = new MaterialCardView(this);
        rebateGateCard.setCardElevation(0f);
        rebateGateCard.setRadius(dp(M3Tokens.ShapeRole.MEDIUM.radiusDp));
        rebateGateCard.setStrokeWidth(dp(1));
        rebateGateCard.setCardBackgroundColor(m3Role(
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                Palette.Role.SURFACE_ELEVATED));
        rebateGateCard.setStrokeColor(m3OutlineVariant());
        rebateGateCard.setClickable(true);
        rebateGateCard.setFocusable(true);
        rebateGateCard.setOnClickListener(v -> showEdgePicker());
        final LinearLayout gateRow = new LinearLayout(this);
        gateRow.setOrientation(LinearLayout.HORIZONTAL);
        gateRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        gateRow.setPadding(dp(14), 0, dp(14), 0);
        gateRow.addView(m3Label("片门格式", M3Tokens.TypeRole.BODY_MEDIUM,
                onSurfaceVariant, tfHan), new LinearLayout.LayoutParams(0, -2, 1f));
        rebateGateValue.setGravity(android.view.Gravity.END);
        gateRow.addView(rebateGateValue, new LinearLayout.LayoutParams(-2, -2));
        final TextView gateArrow = m3Label("↓", M3Tokens.TypeRole.LABEL_LARGE,
                onSurfaceVariant, tfBody);
        final LinearLayout.LayoutParams gateArrowLp = new LinearLayout.LayoutParams(-2, -2);
        gateArrowLp.leftMargin = dp(7);
        gateRow.addView(gateArrow, gateArrowLp);
        rebateGateCard.addView(gateRow,
                new android.widget.FrameLayout.LayoutParams(-1, -1));
        rebateRefreshers.add(() -> {
            final String label = edgeIds.isEmpty()
                    ? "无可用片门"
                    : GateScale.resolve(overscanFormat, filmFormatMm).label;
            rebateGateValue.setText(label);
            rebateGateCard.setContentDescription("片门格式，" + label);
        });
        final LinearLayout.LayoutParams gateLp = new LinearLayout.LayoutParams(-1, dp(52));
        gateLp.bottomMargin = dp(4);
        rebateControls.addView(rebateGateCard, gateLp);

        edgeTextSwitch = new MaterialSwitch(this);
        rebateControls.addView(switchRow("片边文字（厂标 / 张数）", null,
                edgeTextSwitch, () -> edgeTextOn, on -> {
                    edgeTextOn = on;
                    syncRebatePage();
                    commitRebate();
                }), new LinearLayout.LayoutParams(-1, dp(56)));

        rebateFogValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateFogSlider = rebateSliderRow(rebateControls, "片基雾",
                "片基本身的灰雾。0 是完全透明，越大越脏",
                RebateControls.FOG_MIN, RebateControls.FOG_MAX, 0.05f,
                RebateControls.clampFog(edgeFog),
                value -> String.format(java.util.Locale.US, "%.2f", value),
                value -> edgeFog = RebateControls.clampFog(value),
                () -> edgeFog, rebateFogValue);

        rebateLeaksValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateLeaksSlider = rebateSliderRow(rebateControls, "漏光",
                "卷轴漏进来的光，从片边渗向画面",
                RebateControls.LEAKS_MIN, RebateControls.LEAKS_MAX, 0.05f,
                RebateControls.clampLeaks(edgeLeaks),
                value -> String.format(java.util.Locale.US, "%.2f", value),
                value -> edgeLeaks = RebateControls.clampLeaks(value),
                () -> edgeLeaks, rebateLeaksValue);

        rebateFrameNoValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateFrameNoSlider = rebateSliderRow(rebateControls, "片边张数",
                "片边上印的张数。0 = 按 seed 随机",
                RebateControls.FRAME_NUMBER_MIN, RebateControls.FRAME_NUMBER_MAX, 1f,
                RebateControls.clampFrameNumber(frameNumber),
                value -> value < 0.5f ? "自动" : String.valueOf(Math.round(value)),
                value -> frameNumber = RebateControls.clampFrameNumber(value),
                () -> frameNumber, rebateFrameNoValue);

        rebateFNumberValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateFNumberSlider = rebateSliderRow(rebateControls, "光圈值",
                "相机记录的 f 值。决定片门边缘的虚化。0 = 未知（按 f/5.6）",
                RebateControls.F_NUMBER_MIN, RebateControls.F_NUMBER_MAX, 1f,
                RebateControls.clampFNumber(edgeFNumber),
                value -> value < 0.5f
                        ? "未知"
                        : "f/" + String.format(java.util.Locale.US, "%.1f", value),
                value -> edgeFNumber = RebateControls.clampFNumber(value),
                () -> edgeFNumber, rebateFNumberValue);

        rebateModeValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateControls.addView(edgeChoiceRow("片边模式", rebateModeValue, RebateControls.MODES,
                () -> edgeMode, id -> {
                    edgeMode = id;
                    syncRebatePage();
                    commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        rebateGateStyleValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateControls.addView(edgeChoiceRow("片门形状", rebateGateStyleValue,
                RebateControls.GATE_STYLES, () -> edgeGate, id -> {
                    edgeGate = id;
                    syncRebatePage();
                    commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        rebateHolesValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateControls.addView(edgeChoiceRow("齿孔", rebateHolesValue, RebateControls.HOLES,
                () -> edgeHoles, id -> {
                    edgeHoles = id;
                    syncRebatePage();
                    commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        rebateCarrierValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        rebateControls.addView(edgeChoiceRow("片夹", rebateCarrierValue,
                RebateControls.CARRIERS, () -> edgeCarrier, id -> {
                    edgeCarrier = id;
                    syncRebatePage();
                    commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        rebateCameraSeedValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateControls.addView(rebateValueRow("片边种子", rebateCameraSeedValue,
                () -> String.valueOf(cameraSeed),
                () -> askSeed("片边随机种子（0 – 2147483647）", cameraSeed, value -> {
                    cameraSeed = value;
                    syncRebatePage();
                    commitRebate();
                })), new LinearLayout.LayoutParams(-1, -2));

        rebateFrameSeedValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        rebateControls.addView(rebateValueRow("帧种子", rebateFrameSeedValue,
                () -> String.valueOf(frameSeed),
                () -> askSeed("帧随机种子（0 – 2147483647）", frameSeed, value -> {
                    frameSeed = value;
                    syncRebatePage();
                    commitRebate();
                })), new LinearLayout.LayoutParams(-1, -2));

        page.addView(rebateControls, new LinearLayout.LayoutParams(-1, -2));
        return page;
    }

    /**
     * The Date back secondary page. It follows the Rebate page's shape: its
     * switch owns whether the controls are enabled, and the controls stay
     * visible and dimmed while off. The default text comes from the Frame's
     * EXIF capture date; a custom text is sent to the engine exactly as typed.
     */
    private LinearLayout buildDatePage() {
        final LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setVisibility(View.GONE);
        page.addView(edgePageHead("日期水印 / DATE BACK"), new LinearLayout.LayoutParams(-1, -2));

        final int onSurface = m3Role(
                com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK);
        final int onSurfaceVariant = m3Role(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Palette.Role.MUTED_INK);

        dateSwitch = new MaterialSwitch(this);
        dateDetail = m3Label("", M3Tokens.TypeRole.LABEL_SMALL, onSurfaceVariant, tfHan);
        page.addView(switchRow("开启日期水印", dateDetail, dateSwitch, () -> dateOn, on -> {
            dateOn = on;
            syncDatePage();
            commitRebate();
        }), new LinearLayout.LayoutParams(-1, dp(56)));

        dateControls = new LinearLayout(this);
        dateControls.setOrientation(LinearLayout.VERTICAL);

        // The one field the app used to decide for the user: the EXIF date is
        // the default, but the imprint has always been editable.
        dateTextValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(rebateValueRow("日期文字", dateTextValue,
                this::effectiveDateText, () -> {
                    askText("印在画面上的日期（可以自己写）", effectiveDateText(), value -> {
                        dateText = value == null || value.trim().isEmpty()
                                ? null : value.trim();
                        syncDatePage();
                        commitRebate();
                    });
                }), new LinearLayout.LayoutParams(-1, -2));

        dateInsetXValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        dateInsetXSlider = rebateSliderRow(dateControls, "左右内边距",
                "字到片门左右边的距离，毫米",
                DateBack.INSET_MIN, DateBack.INSET_MAX, 0.1f,
                DateBack.clampInset(dateInsetX),
                value -> String.format(java.util.Locale.US, "%.1f mm", value),
                value -> dateInsetX = DateBack.clampInset(value),
                () -> dateInsetX, dateInsetXValue);

        dateInsetYValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        dateInsetYSlider = rebateSliderRow(dateControls, "上下内边距",
                "字到片门上下边的距离，毫米",
                DateBack.INSET_MIN, DateBack.INSET_MAX, 0.1f,
                DateBack.clampInset(dateInsetY),
                value -> String.format(java.util.Locale.US, "%.1f mm", value),
                value -> dateInsetY = DateBack.clampInset(value),
                () -> dateInsetY, dateInsetYValue);

        dateSizeValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        dateSizeSlider = rebateSliderRow(dateControls, "日期大小",
                "相对该样式的字高",
                DateBack.SIZE_MIN, DateBack.SIZE_MAX, 0.05f,
                DateBack.clampSize(dateSize),
                value -> String.format(java.util.Locale.US, "%.2f×", value),
                value -> dateSize = DateBack.clampSize(value),
                () -> dateSize, dateSizeValue);

        dateEvValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfBody);
        dateEvSlider = rebateSliderRow(dateControls, "日期亮度",
                "红光在 18% 灰之上的曝光，单位挡",
                DateBack.EV_MIN, DateBack.EV_MAX, 0.1f,
                DateBack.clampEv(dateEv),
                value -> String.format(java.util.Locale.US, "%+.1f EV", value),
                value -> dateEv = DateBack.clampEv(value),
                () -> dateEv, dateEvValue);

        datePlacementValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(edgeChoiceRow("日期位置", datePlacementValue,
                DateBack.PLACEMENTS, () -> datePlacement, id -> {
                    datePlacement = id;
                    syncDatePage();
                    commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        dateCornerValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(edgeChoiceRow("日期角位", dateCornerValue,
                DateBack.CORNERS, () -> dateCorner, id -> {
                    dateCorner = id;
                    syncDatePage();
                    commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        dateStyleValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(edgeChoiceRow("日期样式", dateStyleValue,
                DateBack.STYLES, () -> dateStyle, id -> {
                    dateStyle = id;
                    syncDatePage();
                    commitRebate();
                }), new LinearLayout.LayoutParams(-1, -2));

        dateDataValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(rebateValueRow("拍摄数据", dateDataValue,
                () -> dataText.isEmpty() ? "（空）" : dataText,
                () -> askText("拍摄数据（和日期一起印在片边）", dataText, value -> {
                    dataText = value == null ? "" : value.trim();
                    syncDatePage();
                    commitRebate();
                })), new LinearLayout.LayoutParams(-1, -2));

        dateTextBValue = m3Label("", M3Tokens.TypeRole.TITLE_SMALL, onSurface, tfHan);
        dateControls.addView(rebateValueRow("第二帧日期", dateTextBValue,
                () -> dateTextB.isEmpty() ? "（空）" : dateTextB,
                () -> askText("半格对的第二帧日期（用不到就留空）", dateTextB, value -> {
                    dateTextB = value == null ? "" : value.trim();
                    syncDatePage();
                    commitRebate();
                })), new LinearLayout.LayoutParams(-1, -2));

        page.addView(dateControls, new LinearLayout.LayoutParams(-1, -2));
        return page;
    }

    /** Back plus the page name; the two pages share one header shape. */
    private View edgePageHead(String title) {
        final LinearLayout head = new LinearLayout(this);
        head.setOrientation(largeText() ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        final MaterialButton back = m3TextButton("‹ 返回", "返回边框页",
                m3Primary(), () -> openEdgePage(-1));
        head.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        final TextView name = mixedM3Label(title, M3Tokens.TypeRole.TITLE_SMALL,
                m3OnSurfaceVariant());
        final LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                largeText() ? -1 : 0, -2, largeText() ? 0f : 1f);
        nameLp.leftMargin = largeText() ? 0 : dp(8);
        head.addView(name, nameLp);
        return head;
    }

    /** -1 = the two cards, 0 = the Rebate page, 1 = the Date back page. */
    private void openEdgePage(int which) {
        edgePageIndex = which;
        if (edgeHome != null) {
            edgeHome.setVisibility(which < 0 ? View.VISIBLE : View.GONE);
        }
        if (rebatePage != null) {
            rebatePage.setVisibility(which == 0 ? View.VISIBLE : View.GONE);
        }
        if (datePage != null) {
            datePage.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
        }
        if (which == 0) syncRebatePage();
        if (which == 1) syncDatePage();
        refreshEdgeCards();
        // The panel is one ScrollView; a page opens at its own top.
        if (panels != null && activeTabIndex == 3 && panels[3] instanceof ScrollView) {
            ((ScrollView) panels[3]).scrollTo(0, 0);
        }
    }

    /** Repaint the entry cards and every chosen row from the live Look. */
    private void refreshEdgeCards() {
        for (int i = 0; i < rebateRefreshers.size(); ++i) rebateRefreshers.get(i).run();
    }

    private String rebateSummary() {
        if (!GateScale.canHostRebate(sourceWidth, sourceHeight)) return "这张图没有可用片门";
        if (!overscanOn) return "未开启 · 点进去设置";
        final String gate = edgeIds.isEmpty()
                ? gateLabel(overscanFormat)
                : GateScale.resolve(overscanFormat, filmFormatMm).label;
        return gate + " · 雾 " + fmt1(edgeFog) + " · 漏光 " + fmt1(edgeLeaks)
                + " · 张数 " + (frameNumber < 0.5f
                        ? "自动" : String.valueOf(Math.round(frameNumber)));
    }

    private String dateSummary() {
        if (!dateOn) return "未开启 · 点进去设置";
        return ControlChoice.labelOf(DateBack.PLACEMENTS, datePlacement)
                + ControlChoice.labelOf(DateBack.CORNERS, dateCorner)
                + " · " + ControlChoice.labelOf(DateBack.STYLES, dateStyle)
                + " · " + effectiveDateText();
    }

    private static String fmt1(float value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    /**
     * Fill the Rebate page from the live fields. It runs after a Look restore,
     * a Gate change, or a page open, so the page never keeps a second copy of
     * the Look. The switch guard keeps the slider listeners from committing a
     * render while they are being moved into place.
     */
    private void syncRebatePage() {
        if (rebateSwitch == null) return;
        syncingRebate = true;
        syncingToggles = true;
        try {
            rebateSwitch.setChecked(overscanOn);
            rebateSwitch.setContentDescription(toggleDescription("开启片边", overscanOn));
            if (edgeTextSwitch != null) {
                edgeTextSwitch.setChecked(edgeTextOn);
                edgeTextSwitch.setContentDescription(
                        toggleDescription("片边文字（厂标 / 张数）", edgeTextOn));
            }
            refreshEdgeCards();
        } finally {
            syncingRebate = false;
            syncingToggles = false;
        }
        setEnabledDeep(rebateControls, overscanOn);
    }

    /**
     * Fill the Date back page from the live fields. Like the Rebate page it
     * runs after a Look restore or a page open, and its guard keeps the slider
     * listeners from committing a render while they are being moved into place.
     */
    private void syncDatePage() {
        if (dateSwitch == null) return;
        syncingRebate = true;
        syncingToggles = true;
        try {
            dateSwitch.setChecked(dateOn);
            dateSwitch.setContentDescription(toggleDescription("开启日期水印", dateOn));
            syncDateDetail();
            refreshEdgeCards();
        } finally {
            syncingRebate = false;
            syncingToggles = false;
        }
        setEnabledDeep(dateControls, dateOn);
    }

    /** Dim and disable a whole control group without hiding what it offers. */
    private static void setEnabledDeep(View view, boolean enabled) {
        if (view == null) return;
        setEnabledRecursive(view, enabled);
        // One 38 % overlay for the group; setting it on every child too would
        // compound the opacity and make nested labels unreadable.
        view.setAlpha(enabled ? 1f : M3Tokens.DISABLED_CONTENT_ALPHA);
    }

    private static void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            final android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); ++i) {
                setEnabledRecursive(group.getChildAt(i), enabled);
            }
        }
    }

    /** A slider, its label, and its live value; the page owns four of them. */
    private Slider rebateSliderRow(LinearLayout parent, String name, String hint,
                                   float min, float max, float step, float initial,
                                   java.util.function.Function<Float, String> format,
                                   java.util.function.Consumer<Float> apply,
                                   java.util.function.Supplier<Float> current,
                                   TextView valueView) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(4), 0, 0);

        final LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.addView(m3Label(name, M3Tokens.TypeRole.BODY_MEDIUM,
                m3Role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK),
                tfHan), new LinearLayout.LayoutParams(0, -2, 1f));
        valueView.setText(format.apply(initial));
        applyTypeRole(valueView, M3Tokens.TypeRole.TITLE_SMALL, tfBody);
        valueView.setTextColor(m3Primary());
        head.addView(valueView, new LinearLayout.LayoutParams(-2, -2));
        row.addView(head, new LinearLayout.LayoutParams(-1, -2));

        if (hint != null && !hint.isEmpty()) {
            final TextView note = m3Label(hint, M3Tokens.TypeRole.LABEL_SMALL,
                    m3OnSurfaceVariant(), tfHan);
            note.setLineSpacing(dp(2), 1f);
            final LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
            noteLp.topMargin = dp(1);
            row.addView(note, noteLp);
        }

        final Slider bar = new Slider(this);
        bar.setId(View.generateViewId());
        bar.setValueFrom(min);
        bar.setValueTo(max);
        bar.setStepSize(step);
        bar.setValue(snapToStep(clampToSlider(initial, min, max), min, step));
        bar.setMinimumHeight(dp(48));
        bar.setLabelFormatter(value -> format.apply(value));
        bar.setLabelBehavior(com.google.android.material.slider.LabelFormatter.LABEL_GONE);
        bar.setContentDescription(name);
        valueView.setLabelFor(bar.getId());
        bar.addOnChangeListener((slider, value, fromUser) -> {
            valueView.setText(format.apply(value));
            // A programmatic park (a Look restore, a page refresh) moves the
            // thumb only. Writing the snapped step back would replace a stored
            // 2.43 mm with 2.4 before the user touched anything.
            if (!fromUser) return;
            apply.accept(value);
            if (!syncingRebate) {
                slider.removeCallbacks(rebateCommit);
                slider.postDelayed(rebateCommit, OPTICS_COMMIT_DELAY_MS);
            }
        });
        bar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(Slider slider) {
                slider.removeCallbacks(rebateCommit);
            }
            @Override public void onStopTrackingTouch(Slider slider) {
                slider.removeCallbacks(rebateCommit);
                commitRebate();
            }
        });
        rebateRefreshers.add(() -> bar.setValue(snapToStep(
                clampToSlider(current.get(), min, max), min, step)));
        row.addView(bar, new LinearLayout.LayoutParams(-1, dp(48)));
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return bar;
    }

    /** A chosen value row backed by the M3 radio sheet; the id is the Look's. */
    private View edgeChoiceRow(String labelText, TextView valueView,
                               List<? extends ControlChoice> choices,
                               java.util.function.Supplier<String> current,
                               java.util.function.Consumer<String> setter) {
        return rebateValueRow(labelText, valueView,
                () -> String.valueOf(ControlChoice.labelOf(choices, current.get())),
                () -> {
                    final int selected = ControlChoice.indexOf(choices, current.get());
                    showChoiceSheet(labelText, "只列出这一版支持的选项",
                            java.util.Arrays.asList(ControlChoice.labels(choices)),
                            selected, picked -> {
                                if (picked < 0 || picked >= choices.size()) return;
                                setter.accept(choices.get(picked).id);
                            });
                });
    }

    /** A non-sheet row: a label, the live value, and a chevron-like arrow. */
    private View rebateValueRow(String labelText, TextView valueView,
                                java.util.function.Supplier<String> current,
                                Runnable onClick) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(52));
        row.setPadding(dp(2), 0, dp(2), 0);
        row.addView(m3Label(labelText, M3Tokens.TypeRole.BODY_MEDIUM,
                m3Role(com.google.android.material.R.attr.colorOnSurfaceVariant,
                        Palette.Role.MUTED_INK), tfHan),
                new LinearLayout.LayoutParams(0, -2, 1f));
        applyTypeRole(valueView, M3Tokens.TypeRole.TITLE_SMALL, tfHan);
        valueView.setTextColor(m3OnSurface());
        valueView.setGravity(android.view.Gravity.END);
        row.addView(valueView, new LinearLayout.LayoutParams(-2, -2));
        final TextView arrow = m3Label("↓", M3Tokens.TypeRole.LABEL_LARGE,
                m3OnSurfaceVariant(), tfBody);
        final LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(-2, -2);
        arrowLp.leftMargin = dp(7);
        row.addView(arrow, arrowLp);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> onClick.run());
        rebateRefreshers.add(() -> {
            final String value = current.get();
            valueView.setText(value);
            row.setContentDescription(labelText + "，" + value);
        });
        return row;
    }

    /** The date text and the capture-data line are free text, not numbers. */
    private void askText(String title, String initial, java.util.function.Consumer<String> onValue) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setText(initial == null ? "" : initial);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(3);
        final int pad = dp(16);
        input.setPadding(pad, dp(8), pad, dp(8));
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) ->
                        onValue.accept(input.getText().toString()))
                .show();
    }

    /** The seeds are arbitrary ints, so they ask for a number rather than a slider. */
    private void askSeed(String title, int initial, java.util.function.IntConsumer onValue) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(initial));
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        final int pad = dp(16);
        input.setPadding(pad, dp(8), pad, dp(8));
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(input)
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
        if (syncingRebate) return;
        onEdgeChanged();
    }

    private final Runnable rebateCommit = () -> {
        if (!syncingRebate) onEdgeChanged();
    };

    private static float clampToSlider(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Put a stored value on the slider's own step grid. An older Look may hold
     * a raw engine value (2.43 mm) that is not a multiple of the page's step;
     * Material's Slider rejects that instead of nudging it.
     */
    private static float snapToStep(float value, float min, float step) {
        if (step <= 0f) return value;
        final float snapped = min + Math.round((value - min) / step) * step;
        return Math.round(snapped * 1000f) / 1000f;
    }

    private void showEdgePicker() {
        if (edgeIds.isEmpty()) {
            showSnackbar("这张图没有可用片门");
            return;
        }
        final int current = edgeIds.indexOf(overscanFormat);
        showChoiceSheet("选择片门格式", "只列出与这张图形状相符的片门",
                edgeLabels, current, picked -> {
                    if (picked < 0 || picked >= edgeIds.size()) return;
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
            if (item == null) continue;
            // A span keeps the Chinese label on OPPOSans through the rail's
            // active/inactive text-appearance changes.
            item.setTitle(typed(TAB_NAMES[i], tfHan));
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                item.setContentDescription(TAB_NAMES[i] + " " + TAB_ENGLISH[i]);
            }
        }
        rail.setOnItemSelectedListener(item -> {
            if (suppressWorkspaceSelection) return true;
            final int index = workspaceIndex(item.getItemId());
            if (index < 0) return false;
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
            if (TAB_ITEM_IDS[i] == itemId) return i;
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
                }
                return false;
            });
            // The tab text appearance is the M3 title-small role; the family
            // stays OPPOSans for the Chinese labels.
            applyTypeface(tab.view, tfHan);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (suppressWorkspaceSelection) return;
                final int index = tab.getPosition();
                if (index == activeTabIndex) return;
                setTab(index);
                setPanelCollapsed(false);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }

            @Override public void onTabReselected(TabLayout.Tab tab) {
                if (suppressWorkspaceSelection) return;
                // A real tap just set lastTabTouchAt; a reselect from a layout
                // pass must not expand the panel the user collapsed. Keyboard,
                // switch-access, and TalkBack activation carry focus instead.
                final boolean activated = tab.view.isPressed()
                        || tab.view.isFocused()
                        || tab.view.isAccessibilityFocused()
                        || android.os.SystemClock.uptimeMillis() - lastTabTouchAt <= 500L;
                if (!activated) return;
                setPanelCollapsed(!panelCollapsed);
                showSnackbar(panelCollapsed ? "参数面板已收起，再点当前页签展开" : "参数面板已展开");
            }
        });
        return tabLayout;
    }

    private void setPanelCollapsed(boolean collapsed) {
        panelCollapsed = collapsed;
        if (panelHolder != null) panelHolder.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        if (controls != null) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) controls.getLayoutParams();
            if (wideLayout) {
                // The column keeps the window height; collapsing hides only the
                // panel, leaving the tabs where they are.
                lp.width = dp(ConsoleLayout.controlColumnWidthDp(currentWidthDp()));
                lp.height = -1;
            } else {
                lp.width = -1;
                lp.height = dp(collapsed ? CONTROLS_CLOSED_DP : CONTROLS_OPEN_DP);
            }
            lp.weight = 0f;
            controls.setLayoutParams(lp);
        }
        if (panelHolder != null) {
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) panelHolder.getLayoutParams();
            lp.height = wideLayout ? 0 : dp(PANEL_HEIGHT_DP);
            lp.weight = wideLayout ? 1f : 0f;
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
        final TextView titleView = m3Label(String.valueOf(title),
                M3Tokens.TypeRole.TITLE_MEDIUM, m3OnSurface(), tfHan);
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
        applyTypeRole(close, M3Tokens.TypeRole.LABEL_LARGE, tfHan);
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
            if (openSheet == d) openSheet = null;
            if (invoker != null && invoker.isAttachedToWindow()) invoker.requestFocus();
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
            if (openSheet.isShowing()) openSheet.dismiss();
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
        if (items == null || items.length == 0 || isFinishing()) return;
        dismissOpenSheet();
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setTitle(title);
        final LinearLayout column = sheetColumn(dialog, title);

        final NestedScrollView scroll = new NestedScrollView(this);
        scroll.setFillViewport(false);
        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        if (note != null && !note.isEmpty()) {
            final TextView noteView = m3Label(note, M3Tokens.TypeRole.BODY_MEDIUM,
                    m3OnSurfaceVariant(), tfHan);
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
            applyTypeRole(row, M3Tokens.TypeRole.BODY_LARGE, tfHan);
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
        if (p.getBoolean(PROMO_MUTED, false)) return;
        if (p.getInt(PROMO_SEEN_CODE, 0) == UpdateCheck.versionCode(this)) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || !uiReady) return;
            showFeedbackGroup(true);
        }, 1800);
    }

    void showFeedbackGroup(final boolean auto) {
        if (isFinishing()) return;
        final android.content.SharedPreferences p = getSharedPreferences(PROMO_PREFS, MODE_PRIVATE);
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(m3Role(
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Palette.Role.SURFACE_ELEVATED));
        View cap = new View(this);
        cap.setBackgroundColor(m3Primary());
        card.addView(cap, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), 0, dp(16), 0);
        head.addView(mixedM3Label("FEEDBACK / 反馈与交流",
                        M3Tokens.TypeRole.LABEL_LARGE, m3Primary()),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView close = m3Label("关闭", M3Tokens.TypeRole.LABEL_LARGE,
                m3OnSurfaceVariant(), tfHan);
        close.setPaintFlags(close.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        close.setPadding(dp(12), dp(14), 0, dp(14));
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        card.addView(head, new LinearLayout.LayoutParams(-1, dp(42)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), 0, dp(16), 4);
        TextView title = m3Label("遇到问题，或者想聊冲印？",
                M3Tokens.TypeRole.HEADLINE_SMALL, m3OnSurface(), tfHan);
        body.addView(title);
        TextView copyText = m3Label("渲染报错、导出异常、想要某支胶片，直接发在群里。"
                + "问题能复现的话，把「复制错误信息」的内容一起贴上。",
                M3Tokens.TypeRole.BODY_MEDIUM, m3OnSurfaceVariant(), tfHan);
        copyText.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams copyTextLp = new LinearLayout.LayoutParams(-1, -2);
        copyTextLp.topMargin = dp(9);
        body.addView(copyText, copyTextLp);

        TextView number = m3Label(QQ_GROUP, M3Tokens.TypeRole.DISPLAY_SMALL,
                m3Primary(), tfDisplay);
        LinearLayout.LayoutParams numberLp = new LinearLayout.LayoutParams(-1, -2);
        numberLp.topMargin = dp(16);
        body.addView(number, numberLp);
        TextView hint = m3Label("QQ 群号，点下面按钮复制后搜索加入",
                M3Tokens.TypeRole.LABEL_MEDIUM, m3OnSurfaceVariant(), tfHan);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, -2);
        hintLp.topMargin = dp(5);
        body.addView(hint, hintLp);
        card.addView(body, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView open = m3Label("打开 QQ", M3Tokens.TypeRole.LABEL_LARGE,
                m3Primary(), tfHan);
        open.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable openBg = new android.graphics.drawable.GradientDrawable();
        openBg.setColor(m3Role(com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Palette.Role.SURFACE_ELEVATED));
        openBg.setStroke(dp(1), m3Primary());
        open.setBackground(openBg);
        open.setOnClickListener(v -> openQqGroup());
        LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        openLp.rightMargin = dp(8);
        actions.addView(open, openLp);

        TextView copy = m3Label("复制群号", M3Tokens.TypeRole.LABEL_LARGE,
                m3Role(com.google.android.material.R.attr.colorOnPrimary,
                        Palette.Role.ON_ACCENT), tfHan);
        copy.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable copyBg = new android.graphics.drawable.GradientDrawable();
        copyBg.setColor(m3Primary());
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

        if (auto) {
            TextView mute = m3Label("不再自动弹出", M3Tokens.TypeRole.LABEL_MEDIUM,
                    m3OnSurfaceVariant(), tfHan);
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
        if (labels == null || labels.isEmpty() || isFinishing()) return;
        dismissOpenSheet();
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setTitle(title);
        final LinearLayout column = sheetColumn(dialog, title);

        if (note != null && !note.isEmpty()) {
            final TextView noteView = m3Label(note, M3Tokens.TypeRole.BODY_MEDIUM,
                    m3OnSurfaceVariant(), tfHan);
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
            if (checkedId == View.NO_ID) return;
            for (int i = 0; i < rows.length; ++i) {
                if (rows[i].getId() != checkedId) continue;
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
        overscanOn = false;
        dateOn = false;
        dateText = null;
        edgeMode = "strip";
        edgeGate = "auto";
        edgeHoles = "white";
        edgeCarrier = "none";
        edgeTextOn = true;
        edgeFog = 1f;
        edgeLeaks = 0f;
        frameNumber = 0f;
        edgeFNumber = 0f;
        cameraSeed = 1;
        frameSeed = 1;
        datePlacement = "frame";
        dateCorner = "br";
        dateStyle = "lcd";
        dateInsetX = 3f;
        dateInsetY = 2.4f;
        dateSize = 1f;
        dateEv = 3.5f;
        dataText = "";
        dateTextB = "";
        syncToggleRows();
        syncDateDetail();
        if (session != null) onEdgeChanged();
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

    /** A drawn tool glyph: no font fallback tofu at 19 dp. */
    private static class ToolGlyph extends View {
        static final int COMPARE = 0, CAMERA = 1;
        private final int kind;
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float stroke;

        ToolGlyph(android.content.Context context, int kind, int color) {
            super(context);
            this.kind = kind;
            stroke = Math.max(1f, getResources().getDisplayMetrics().density);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(color);
        }

        void setColor(int color) {
            paint.setColor(color);
            invalidate();
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            final float w = getWidth(), h = getHeight(), p = stroke;
            if (kind == COMPARE) {
                canvas.drawRect(p, h * 0.20f, w - p, h * 0.82f, paint);
                canvas.drawLine(w * 0.5f, h * 0.20f, w * 0.5f, h * 0.82f, paint);
                canvas.drawLine(p, h * 0.68f, w * 0.42f, h * 0.38f, paint);
                canvas.drawLine(w - p, h * 0.68f, w * 0.58f, h * 0.38f, paint);
            } else {
                canvas.drawRect(p, h * 0.28f, w - p, h * 0.82f, paint);
                canvas.drawLine(w * 0.27f, h * 0.28f, w * 0.36f, h * 0.16f, paint);
                canvas.drawLine(w * 0.36f, h * 0.16f, w * 0.64f, h * 0.16f, paint);
                canvas.drawLine(w * 0.64f, h * 0.16f, w * 0.73f, h * 0.28f, paint);
                canvas.drawCircle(w * 0.5f, h * 0.55f, h * 0.16f, paint);
            }
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
            if (!edgeIds.contains(overscanFormat)) {
                overscanFormat = edgeIds.get(Math.max(0, Math.min(best, edgeIds.size() - 1)));
            }
            // The Scale is the Gate's measured long edge, never a neighbouring
            // page value: 6x17 is 168 mm, not the old 170 mm card.
            final GateScale.Gate gate = GateScale.byId(overscanFormat);
            if (gate != null) filmFormatMm = gate.longEdgeMm;
        } else {
            // No Gate fits this shape. Keep the stored pair consistent even
            // though the Rebate has nothing to cut.
            final GateScale.Gate pair = GateScale.pair(overscanFormat, filmFormatMm);
            overscanFormat = pair.id;
            filmFormatMm = pair.longEdgeMm;
            if (overscanOn) {
                // A rebate cannot be drawn for a shape no Gate can hold. Leaving
                // it on would send the engine a layout it has already refused.
                overscanOn = false;
                syncToggleRows();
            }
        }

        updateEdgeValue();
        if (formatFootValue != null) {
            formatFootValue.setText(mmText(filmFormatMm) + " mm");
        }
        rebuildFormatCards();
        // The page's Gate row and switch follow the same available set: a shape
        // no Gate can hold cannot keep the Rebate on.
        syncRebatePage();
        try {
            DebugSink.text("overscan", "aspect=" + aspect + " gates=" + edgeIds
                    + " chosen=" + overscanFormat + " scale=" + filmFormatMm);
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
        if (!uiReady || session == null || filmStock == null) return;
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
    private String imprintDate() {
        if (dateText != null && !dateText.isEmpty()) return dateText;
        return defaultImprintDate();
    }

    /**
     * The Frame's own default: its EXIF capture date, or today when the file
     * has none. Cached for the open image; a typed text never overwrites it.
     */
    private String defaultImprintDate() {
        if (defaultDateText != null && !defaultDateText.isEmpty()) return defaultDateText;
        String exif = null;
        try {
            if (sourceUri != null) {
                try (InputStream in = getContentResolver().openInputStream(sourceUri)) {
                    if (in != null) {
                        exif = new android.media.ExifInterface(in)
                                .getAttribute(android.media.ExifInterface.TAG_DATETIME_ORIGINAL);
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
        return dateText != null && !dateText.isEmpty() ? dateText : imprintDate();
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
        textCol.addView(m3Label(labelText, M3Tokens.TypeRole.LABEL_LARGE,
                m3Role(com.google.android.material.R.attr.colorOnSurface, Palette.Role.INK), tfHan));
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
            if (syncingToggles) return;
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
    private void syncDateDetail() {
        if (dateDetail == null) return;
        if (dateOn) {
            dateDetail.setText(imprintDate());
            dateDetail.setVisibility(View.VISIBLE);
        } else {
            dateDetail.setText("");
            dateDetail.setVisibility(View.GONE);
        }
    }

    /** Re-apply a restored Look to the Edge pages' switches and controls. */
    private void syncToggleRows() {
        syncRebatePage();
        syncDatePage();
    }

    /**
     * One render for a completed optics gesture. The render thread sends the
     * parameter delta under the same lock as the draw, so a gesture produces
     * one update and one picture.
     */
    private void commitOptics() {
        if (opticsSlider != null) opticsSlider.removeCallbacks(opticsCommitRunnable);
        if (uiReady && session != null) {
            showingOriginal = false;
            setOriginalVisual(false);
            renderImage();
        }
    }

    /** The old four-step ribbon is gone; the render state is in the canvas foot. */
    private void setStep(int active) { }

    private void setTab(int active) {
        final int previous = activeTabIndex;
        activeTabIndex = active;
        for (int i = 0; i < panels.length; ++i) {
            panels[i].setVisibility(i == active ? View.VISIBLE : View.GONE);
        }
        if (tabLayout != null) {
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
        if (navigationRail != null && active >= 0 && active < TAB_ITEM_IDS.length) {
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
        if (uiReady && previous != active && active >= 0 && active < panels.length) {
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
            for (int x = 0; x < w && black; ++x) black = (row[x] & 0xFFFFFF) <= 0x060606;
            if (!black) { top = y; break; }
        }
        for (int y = h - 1; y >= 0; --y) {
            b.getPixels(row, 0, w, 0, y, w, 1);
            boolean black = true;
            for (int x = 0; x < w && black; ++x) black = (row[x] & 0xFFFFFF) <= 0x060606;
            if (!black) { bottom = h - 1 - y; break; }
        }
        return new int[]{top, bottom};
    }

    /** The optics controls only mean something once there is a frame to change. */
    private void setOpticsUnlocked(boolean unlocked) {
        opticsUnlocked = unlocked;
        if (opticsPanel == null) return;
        final float alpha = unlocked ? 1f : M3Tokens.DISABLED_CONTENT_ALPHA;
        if (opticsStripScroll != null) {
            opticsStripScroll.setAlpha(alpha);
            opticsStripScroll.setEnabled(unlocked);
        }
        for (Chip chip : opticsChips) chip.setEnabled(unlocked);
        if (opticsSlider != null) opticsSlider.setEnabled(unlocked);
        if (opticsReset != null) opticsReset.setEnabled(unlocked);
        if (opticsFilterButton != null) opticsFilterButton.setEnabled(unlocked);
        if (opticsNormButton != null) opticsNormButton.setEnabled(unlocked);
        if (opticsLockNote != null) {
            opticsLockNote.setText(unlocked ? "" : "LOCKED · " + opticsLockReason());
            opticsLockNote.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        }
    }

    /** Which of the three things on the stage is showing. */
    private void setStage(int mode, String name) {
        stageMode = mode;
        if (mode == 0) compare.clear();
        emptyState.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        compare.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        rawState.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
        if (mode == 0) {
            showingOriginal = false;
            setOriginalVisual(false);
        }
        if (name != null && !name.isEmpty()) {
            fileLabel.setText(name);
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
                "从文件管理器选择（RAW / HEIC / 任意文件）"
        }, which -> {
            // A deliberate pick is a *new* photograph, not a chip coming back.
            // Cleared here so a chip whose file disappeared cannot leave the
            // flag set and make the next import silently skip the strip.
            reopeningFrame = false;
            suppressBudgetDialog = false;
            saveLookIntoFrames();
            if (which == 0) pickImage(); else pickRawFile();
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
            final double driverMp = probe.maxStorageBufferRange / 12.0 * 0.80 / 1e6;
            final double budgetMp = maxImportPixels() / 1e6;
            driverLine = String.format(java.util.Locale.US,
                    "驱动单缓冲 %.0f MiB，可达 %.0f MP；当前版本按 %.1f MP 预算（分块引擎优化中）",
                    probe.maxStorageBufferRange / 1048576.0, driverMp, budgetMp);
        } else {
            driverLine = probe.valid ? "驱动未报单缓冲上限" : "设备信息读取失败";
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
        showActionSheet("导出照片", note, new String[]{
                "JPEG · 实时画质",
                "JPEG · 预览画质 · 长边 3400",
                "JPEG · 全尺寸（原图）",
                "TIFF 16 位 · 实时画质",
                "TIFF 16 位 · 预览画质 · 长边 3400",
                "TIFF 16 位 · 全尺寸（原图）",
                "DNG · 全尺寸（电脑用 · 手机版打不开）"
        }, which -> {
            if (which == 6) { exportImage("dng", "full"); return; }
            exportImage(which >= 3 ? "tiff" : "jpeg",
                        which % 3 == 0 ? "live" : which % 3 == 1 ? "preview" : "full");
        });
    }

    /** Reset from the old single control strip: delegate to the film page. */
    private void resetLook() { resetFilmPanel(); }

    private static class ArrowView extends View {
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float stroke;

        ArrowView(android.content.Context context, int color) {
            super(context);
            paint.setColor(color);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            stroke = Math.max(1f, getResources().getDisplayMetrics().density * 1.1f);
            paint.setStrokeWidth(stroke);
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            final float w = getWidth(), h = getHeight();
            final float pad = stroke;
            canvas.drawLine(pad, h - pad, w - pad, pad, paint);
            canvas.drawLine(w - pad, pad, w - pad * 2f - w * 0.34f, pad, paint);
            canvas.drawLine(w - pad, pad, w - pad, pad * 2f + h * 0.34f, paint);
        }
    }

    /**
     * The stage's picture, and the two ways to see what the film did: hold it
     * to see the frame before, or drag the middle line to keep both in view.
     *
     * Each side is drawn fit-inside, in its own frame: a rebate makes the
     * "after" a different shape from the raw, and cropping one to match the
     * other would misrepresent what is being compared.
     */
    private static class CompareView extends View {
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
        private final android.graphics.RectF dst = new android.graphics.RectF();
        private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        private final float slop;
        private final float density;
        private final int dividerColor, handleColor, accentColor;
        private Bitmap before, after;
        private boolean holding, dragging;
        private float split, downX, downY;
        private boolean pinching;
        private float zoom = 1f, panX = 0f, panY = 0f;
        private float pinchStartSpan, pinchStartZoom, pinchStartPanX, pinchStartPanY;
        private float pinchStartCx, pinchStartCy;

        private final Runnable press = new Runnable() {
            @Override public void run() {
                if (dragging || after == null || before == null) return;
                holding = true;
                invalidate();
            }
        };

        CompareView(android.content.Context context, int dividerColor,
                    int handleColor, int accentColor) {
            super(context);
            this.dividerColor = dividerColor;
            this.handleColor = handleColor;
            this.accentColor = accentColor;
            density = getResources().getDisplayMetrics().density;
            slop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        }

        void setBefore(Bitmap b) { before = b; invalidate(); }
        void setAfter(Bitmap b) { after = b; invalidate(); }

        void clear() {
            before = null;
            after = null;
            holding = false;
            dragging = false;
            split = 0f;
            pinching = false;
            zoom = 1f;
            panX = 0f;
            panY = 0f;
            invalidate();
        }

        /** True when both sides exist, so the divider and hold are meaningful. */
        boolean canCompare() {
            return before != null && after != null;
        }

        /** A touch within 24 dp of an established divider belongs to CompareView. */
        boolean dividerHit(float x) {
            return canCompare() && split > 0f
                    && Math.abs(x - split * getWidth()) <= 24f * density;
        }

        /** Hand one gesture to the divider while Scan inspect is on top. */
        boolean forwardTouch(android.view.MotionEvent event) {
            return onTouchEvent(event);
        }

        /** Scan inspect's long-press keeps the Original hold available. */
        void beginHold() {
            if (!canCompare()) return;
            holding = true;
            invalidate();
        }

        void endHold() {
            if (!holding) return;
            holding = false;
            invalidate();
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            final Bitmap base = after != null ? after : before;
            if (base == null) return;
            drawFit(canvas, base);
            if (before == null || after == null) return;
            if (holding) {
                drawFit(canvas, before);
            } else if (split > 0f) {
                final float x = split * getWidth();
                canvas.save();
                canvas.clipRect(0f, 0f, x, getHeight());
                drawFit(canvas, before);
                canvas.restore();
                drawDivider(canvas, x);
            }
        }

        private void drawFit(android.graphics.Canvas canvas, Bitmap bmp) {
            final float vw = getWidth(), vh = getHeight();
            if (vw <= 0 || vh <= 0 || bmp.getWidth() == 0) return;
            final float scale = Math.min(vw / bmp.getWidth(), vh / bmp.getHeight()) * zoom;
            final float w = bmp.getWidth() * scale, h = bmp.getHeight() * scale;
            final float left = (vw - w) / 2f + panX, top = (vh - h) / 2f + panY;
            dst.set(left, top, left + w, top + h);
            canvas.drawBitmap(bmp, null, dst, paint);
        }

        /** Current scale, for the stage readout; never triggers a render. */
        float zoomLevel() {
            return zoom;
        }

        private static float spanOf(android.view.MotionEvent e) {
            if (e.getPointerCount() < 2) return 0f;
            return (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
        }

        private static float midXOf(android.view.MotionEvent e) {
            return (e.getX(0) + e.getX(1)) / 2f;
        }

        private static float midYOf(android.view.MotionEvent e) {
            return (e.getY(0) + e.getY(1)) / 2f;
        }

        private void drawDivider(android.graphics.Canvas canvas, float x) {
            final float r = 6f * density, cy = getHeight() / 2f;
            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setColor(dividerColor);
            canvas.drawRect(x - 0.5f, 0f, x + 0.5f, getHeight(), paint);
            paint.setColor(handleColor);
            canvas.drawRect(x - r, cy - r, x + r, cy + r, paint);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, density));
            paint.setColor(accentColor);
            canvas.drawRect(x - r, cy - r, x + r, cy + r, paint);
            // The divider is chrome, not measured content: its drag/hold state
            // uses the M3 state-layer opacities over the control colour.
            final float state = dragging ? M3Tokens.StateLayer.DRAGGED.opacity
                    : holding ? M3Tokens.StateLayer.PRESSED.opacity : 0f;
            if (state > 0f) {
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(withOpacity(accentColor, state));
                canvas.drawRect(x - r, cy - r, x + r, cy + r, paint);
            }
            paint.setStyle(android.graphics.Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            if (after == null || before == null) return false;
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    dragging = false;
                    holding = false;
                    pinching = false;
                    handler.postDelayed(press, android.view.ViewConfiguration.getLongPressTimeout());
                    return true;
                case android.view.MotionEvent.ACTION_POINTER_DOWN:
                    if (e.getPointerCount() >= 2) {
                        handler.removeCallbacks(press);
                        dragging = false;
                        holding = false;
                        pinching = true;
                        pinchStartSpan = Math.max(1f, spanOf(e));
                        pinchStartZoom = zoom;
                        pinchStartPanX = panX;
                        pinchStartPanY = panY;
                        pinchStartCx = midXOf(e);
                        pinchStartCy = midYOf(e);
                        return true;
                    }
                    return super.onTouchEvent(e);
                case android.view.MotionEvent.ACTION_MOVE:
                    if (pinching) {
                        if (e.getPointerCount() < 2) return true;
                        final float span = Math.max(1f, spanOf(e));
                        final float want = Math.max(0.2f, Math.min(8f,
                                pinchStartZoom * span / pinchStartSpan));
                        zoom = want;
                        panX = pinchStartPanX + (midXOf(e) - pinchStartCx);
                        panY = pinchStartPanY + (midYOf(e) - pinchStartCy);
                        invalidate();
                        return true;
                    }
                    if (holding) return true;
                    if (!dragging) {
                        if (Math.abs(e.getX() - downX) < slop && Math.abs(e.getY() - downY) < slop) return true;
                        dragging = true;
                        handler.removeCallbacks(press);
                    }
                    split = Math.max(0.02f, Math.min(0.98f, e.getX() / Math.max(1, getWidth())));
                    invalidate();
                    return true;
                case android.view.MotionEvent.ACTION_POINTER_UP:
                    if (pinching) return true;
                    return super.onTouchEvent(e);
                case android.view.MotionEvent.ACTION_UP:
                    handler.removeCallbacks(press);
                    pinching = false;
                    if (holding) {
                        holding = false;
                        invalidate();
                    } else if (!dragging && split > 0f) {
                        // A tap closes it: the divider is a gesture, not a mode.
                        split = 0f;
                        invalidate();
                    }
                    dragging = false;
                    return true;
                case android.view.MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(press);
                    pinching = false;
                    holding = false;
                    dragging = false;
                    invalidate();
                    return true;
                default:
                    return super.onTouchEvent(e);
            }
        }
    }

    /**
     * Scan inspect's draggable line and its touch/keyboard surface.
     *
     * <p>It sits over CompareView only while the mode is on. A touch that
     * starts on an established compare divider is forwarded, and a long press
     * still asks CompareView for the Original hold, so the overlay never makes
     * the core review gesture unreachable. The fraction is reported to
     * accessibility as a 0–100 range with scroll and set-progress actions.
     */
    private static class ScanOverlayView extends View {
        interface Listener {
            void onScanSample(ScanSample sample, float fraction);
        }

        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path triangle = new android.graphics.Path();
        private final android.graphics.RectF image = new android.graphics.RectF();
        private final android.os.Handler handler =
                new android.os.Handler(android.os.Looper.getMainLooper());
        private final float density;
        private final float slop;
        private final CompareView compare;
        private final Listener listener;
        private final int accentColor, shadowColor;
        private Bitmap preview;
        private int[] row;
        private int rowWidth;
        private float fraction = ScanSample.DEFAULT_FRACTION;
        private float downX, downY;
        private boolean dragging, holding, forwarding;

        private final Runnable hold = new Runnable() {
            @Override public void run() {
                if (dragging || compare == null || !compare.canCompare()) return;
                holding = true;
                compare.beginHold();
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            }
        };

        ScanOverlayView(android.content.Context context, Palette palette,
                        CompareView compare, Listener listener) {
            super(context);
            this.compare = compare;
            this.listener = listener;
            density = getResources().getDisplayMetrics().density;
            slop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
            accentColor = palette.color(Palette.Role.ACCENT);
            shadowColor = palette.color(Palette.Role.STAGE_SHADOW);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setContentDescription("扫描线，位置 50%");
        }

        private float dp(float v) {
            return v * density;
        }

        /** Cache the centre row once per bitmap; every move samples it. */
        void setPreviewBitmap(Bitmap bitmap) {
            if (bitmap == preview) {
                if (bitmap != null) notifySample();
                return;
            }
            preview = bitmap;
            if (bitmap == null) {
                row = null;
                rowWidth = 0;
            } else {
                rowWidth = bitmap.getWidth();
                row = new int[rowWidth];
                bitmap.getPixels(row, 0, rowWidth, 0, bitmap.getHeight() / 2, rowWidth, 1);
            }
            invalidate();
            notifySample();
        }

        void setScanVisible(boolean visible) {
            setVisibility(visible ? VISIBLE : GONE);
            if (!visible) {
                handler.removeCallbacks(hold);
                if (holding) {
                    holding = false;
                    if (compare != null) compare.endHold();
                }
                dragging = false;
                forwarding = false;
            }
            invalidate();
        }

        void setFraction(float value) {
            float next = Float.isNaN(value) ? ScanSample.DEFAULT_FRACTION : value;
            next = Math.max(0f, Math.min(1f, next));
            if (Math.abs(next - fraction) < 1e-5f) {
                notifySample();
                return;
            }
            fraction = next;
            invalidate();
            notifySample();
        }

        private void notifySample() {
            if (listener == null) return;
            listener.onScanSample(ScanSample.at(row, rowWidth, fraction), fraction);
        }

        private boolean imageRect() {
            final int vw = getWidth(), vh = getHeight();
            if (preview == null || vw <= 0 || vh <= 0) return false;
            final int bw = preview.getWidth(), bh = preview.getHeight();
            if (bw <= 0 || bh <= 0) return false;
            final float scale = Math.min(vw / (float) bw, vh / (float) bh);
            final float w = bw * scale, h = bh * scale;
            final float left = (vw - w) * 0.5f, top = (vh - h) * 0.5f;
            image.set(left, top, left + w, top + h);
            return true;
        }

        private float fractionAt(float x) {
            if (!imageRect() || image.width() <= 0f) return fraction;
            return (x - image.left) / image.width();
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            if (preview == null || !imageRect()) return;
            final float x = image.left + fraction * image.width();

            // A dark halo under the accent keeps the line readable on both a
            // bright print and a night scene, in either Theme mode.
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(shadowColor);
            paint.setAlpha(170);
            canvas.drawLine(x, image.top, x, image.bottom, paint);

            paint.setStrokeWidth(Math.max(1f, density));
            paint.setColor(accentColor);
            paint.setAlpha(255);
            canvas.drawLine(x, image.top, x, image.bottom, paint);

            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setColor(shadowColor);
            triangle.reset();
            triangle.moveTo(x, image.top);
            triangle.lineTo(x - dp(5), image.top + dp(9));
            triangle.lineTo(x + dp(5), image.top + dp(9));
            triangle.close();
            canvas.drawPath(triangle, paint);

            paint.setColor(accentColor);
            triangle.reset();
            triangle.moveTo(x, image.top);
            triangle.lineTo(x - dp(3), image.top + dp(6));
            triangle.lineTo(x + dp(3), image.top + dp(6));
            triangle.close();
            canvas.drawPath(triangle, paint);

            paint.setColor(shadowColor);
            triangle.reset();
            triangle.moveTo(x, image.bottom);
            triangle.lineTo(x - dp(5), image.bottom - dp(9));
            triangle.lineTo(x + dp(5), image.bottom - dp(9));
            triangle.close();
            canvas.drawPath(triangle, paint);

            paint.setColor(accentColor);
            triangle.reset();
            triangle.moveTo(x, image.bottom);
            triangle.lineTo(x - dp(3), image.bottom - dp(6));
            triangle.lineTo(x + dp(3), image.bottom - dp(6));
            triangle.close();
            canvas.drawPath(triangle, paint);
            paint.setAlpha(255);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent event) {
            if (getVisibility() != VISIBLE || preview == null) return false;
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    requestFocus();
                    downX = event.getX();
                    downY = event.getY();
                    dragging = false;
                    holding = false;
                    // Keep the compare divider draggable while Scan is on.
                    if (compare != null && compare.dividerHit(downX)) {
                        forwarding = true;
                        return compare.forwardTouch(event);
                    }
                    forwarding = false;
                    handler.postDelayed(hold, android.view.ViewConfiguration.getLongPressTimeout());
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (forwarding) return compare.forwardTouch(event);
                    if (holding) return true;
                    if (!dragging) {
                        if (Math.abs(event.getX() - downX) < slop
                                && Math.abs(event.getY() - downY) < slop) return true;
                        dragging = true;
                        handler.removeCallbacks(hold);
                    }
                    setFraction(fractionAt(event.getX()));
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    if (forwarding) {
                        forwarding = false;
                        return compare.forwardTouch(event);
                    }
                    handler.removeCallbacks(hold);
                    if (holding) {
                        holding = false;
                        if (compare != null) compare.endHold();
                    } else if (!dragging) {
                        setFraction(fractionAt(event.getX()));
                    }
                    dragging = false;
                    return true;
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (forwarding) {
                        forwarding = false;
                        compare.forwardTouch(event);
                        return true;
                    }
                    handler.removeCallbacks(hold);
                    if (holding) {
                        holding = false;
                        if (compare != null) compare.endHold();
                    }
                    dragging = false;
                    return true;
                default:
                    return true;
            }
        }

        /** Arrow, Home, and End handling shared by the view and the Activity. */
        boolean handleKey(int keyCode, boolean shift) {
            if (getVisibility() != VISIBLE || preview == null) return false;
            final float step = shift ? 0.05f : 0.01f;
            switch (keyCode) {
                case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                    setFraction(fraction - step);
                    return true;
                case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                    setFraction(fraction + step);
                    return true;
                case android.view.KeyEvent.KEYCODE_MOVE_HOME:
                    setFraction(0f);
                    return true;
                case android.view.KeyEvent.KEYCODE_MOVE_END:
                    setFraction(1f);
                    return true;
                default:
                    return false;
            }
        }

        @Override public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
            return handleKey(keyCode, event.isShiftPressed()) || super.onKeyDown(keyCode, event);
        }

        @Override public CharSequence getAccessibilityClassName() {
            return android.widget.SeekBar.class.getName();
        }

        @Override public void onInitializeAccessibilityNodeInfo(
                android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(android.widget.SeekBar.class.getName());
            info.setRangeInfo(android.view.accessibility.AccessibilityNodeInfo.RangeInfo.obtain(
                    android.view.accessibility.AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
                    0f, 100f, fraction * 100f));
            info.setContentDescription("扫描线，位置 " + Math.round(fraction * 100f)
                    + "%，左右方向键移动");
            if (getVisibility() == VISIBLE && preview != null) {
                info.addAction(android.view.accessibility.AccessibilityNodeInfo
                        .AccessibilityAction.ACTION_SCROLL_FORWARD);
                info.addAction(android.view.accessibility.AccessibilityNodeInfo
                        .AccessibilityAction.ACTION_SCROLL_BACKWARD);
                info.addAction(android.view.accessibility.AccessibilityNodeInfo
                        .AccessibilityAction.ACTION_SET_PROGRESS);
            }
        }

        @Override public boolean performAccessibilityAction(int action, android.os.Bundle args) {
            if (getVisibility() == VISIBLE && preview != null) {
                if (action == android.view.accessibility.AccessibilityNodeInfo
                        .ACTION_SCROLL_FORWARD) {
                    setFraction(fraction + 0.01f);
                    return true;
                }
                if (action == android.view.accessibility.AccessibilityNodeInfo
                        .ACTION_SCROLL_BACKWARD) {
                    setFraction(fraction - 0.01f);
                    return true;
                }
                if (action == android.view.accessibility.AccessibilityNodeInfo
                        .AccessibilityAction.ACTION_SET_PROGRESS.getId()) {
                    final float target = args == null
                            ? fraction * 100f
                            : args.getFloat(android.view.accessibility.AccessibilityNodeInfo
                                    .ACTION_ARGUMENT_PROGRESS_VALUE, fraction * 100f);
                    setFraction(target / 100f);
                    return true;
                }
            }
            return super.performAccessibilityAction(action, args);
        }

        @Override protected void onDetachedFromWindow() {
            handler.removeCallbacks(hold);
            super.onDetachedFromWindow();
        }
    }

    /** .frame-guides: a 9 % inset rule, a centre cross, four corner marks. */
    private static class GuideView extends View {
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int mark;
        private final Palette palette;

        GuideView(android.content.Context context, int markLength, Palette palette) {
            super(context);
            mark = markLength;
            this.palette = palette;
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            final int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            final float dx = w * 0.09f, dy = h * 0.09f;
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(palette.color(Palette.Role.GUIDE_LINE));
            canvas.drawRect(dx, dy, w - dx, h - dy, paint);
            paint.setColor(palette.color(Palette.Role.GUIDE_CENTER));
            canvas.drawLine(w / 2f, dy, w / 2f, h - dy, paint);
            canvas.drawLine(dx, h / 2f, w - dx, h / 2f, paint);
            paint.setColor(palette.color(Palette.Role.GUIDE_CORNER));
            final float L = mark;
            canvas.drawLine(dx, dy, dx + L, dy, paint);
            canvas.drawLine(dx, dy, dx, dy + L, paint);
            canvas.drawLine(w - dx, dy, w - dx - L, dy, paint);
            canvas.drawLine(w - dx, dy, w - dx, dy + L, paint);
            canvas.drawLine(dx, h - dy, dx + L, h - dy, paint);
            canvas.drawLine(dx, h - dy, dx, h - dy - L, paint);
            canvas.drawLine(w - dx, h - dy, w - dx - L, h - dy, paint);
            canvas.drawLine(w - dx, h - dy, w - dx, h - dy - L, paint);
        }
    }

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
        final double fit = importScale(w, h, capPixels);
        final int sw = Math.max(1, (int) Math.round(w * fit));
        final int sh = Math.max(1, (int) Math.round(h * fit));
        final String budgetMessage = "原图 " + w + "×" + h + "（" + megapixels(w, h) + " MP），这台机器一次能渲染约 "
                + String.format(java.util.Locale.US, "%.1f", capPixels / 1e6) + " MP"
                + (overscanOn ? "（已按片边预留画布，片边会把画布再撑大约 1.5 倍）" : "") + "。"
                + "\n\n按 " + sw + "×" + sh + " 导入一定出得来；按原尺寸可能在本机渲染失败（E-1001）。";
        showActionSheet("这张图超过本机预算", budgetMessage, new String[]{
                "按 " + sw + "×" + sh + " 导入", "按原尺寸", "取消"
        }, which -> {
            if (which == 0) {
                importBudgetAccepted = true;
                loadImage(uri);
            } else if (which == 1) {
                importAtNativeSize = true;
                loadImage(uri);
            }
        });
    }

    /**
     * The scale the import will actually apply, so the dialog can name the size
     * it is about to deliver -- the same three ceilings, in the same order, as
     * `loadImage` below.
     */
    private double importScale(int w, int h, int capPixels) {
        final long area = (long) w * (long) h;
        double k = 1.0;
        if (area > capPixels) k = Math.sqrt((double) capPixels / (double) area);
        final int longEdge = Math.max(w, h);
        if (longEdge > IMPORT_LONG_EDGE) k = Math.min(k, (double) IMPORT_LONG_EDGE / longEdge);
        final long availBytes = availableMemoryBytes();
        if (availBytes > 0) {
            final long budget = Math.max(1L << 20, availBytes - (128L << 20));
            final double wantBytes = (double) area * 12.0;
            if (wantBytes > budget) k = Math.min(k, Math.sqrt((double) budget / wantBytes));
        }
        return Math.min(k, 1.0);
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
                if (head != null) android.graphics.BitmapFactory.decodeStream(head, null, bounds);
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
        if (canaryStarted || engine == null) return;
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
                    if (result != null) probe.applyCanary(result);
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
        filmIds.clear();
        filmLabels.clear();
        filmTypes.clear();
        printIds.clear();
        printLabels.clear();
        filmStocks.clear();
        try (InputStream in = getAssets().open("resources/stock_catalog.json")) {
            final org.json.JSONObject root = new org.json.JSONObject(
                    new String(readFully(in), java.nio.charset.StandardCharsets.UTF_8));
            final org.json.JSONArray films = root.optJSONArray("films");
            readStocks(films, filmIds, filmLabels, filmTypes);
            readStocks(root.optJSONArray("prints"), printIds, printLabels, null);
            readFilmStocks(films);
        } catch (Throwable t) {
            android.util.Log.w("SpektraLab", "stock catalog: " + t);
        }
        if (filmIds.isEmpty()) { filmIds.add("kodak_portra_400"); filmLabels.add("Kodak Portra 400"); }
        if (printIds.isEmpty()) { printIds.add("kodak_portra_endura"); printLabels.add("Kodak Portra Endura"); }

        refreshFilmPanel();
        refreshPaperLabel();
    }

    /** Read an asset to its end. The catalogue now carries spectral arrays, so
     *  its size is no longer something `available()` can be trusted to predict. */
    private static byte[] readFully(InputStream in) throws java.io.IOException {
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        final byte[] chunk = new byte[16 * 1024];
        int n;
        while ((n = in.read(chunk)) > 0) out.write(chunk, 0, n);
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
        if (films == null) return;
        for (int i = 0; i < films.length(); ++i) {
            final org.json.JSONObject o = films.optJSONObject(i);
            if (o == null) continue;
            final String id = o.optString("id");
            if (id.isEmpty()) continue;
            org.json.JSONObject data = o.optJSONObject("spectral");
            if (data == null) data = o.optJSONObject("data");
            if (data == null) data = o;
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
            if ("id".equals(key) || "name".equals(key) || "spectral_source".equals(key)) continue;
            final Object value = o.opt(key);
            if (value instanceof String) {
                final String text = ((String) value).trim();
                if (!text.isEmpty()) info.put(key, text);
            } else if (value instanceof Number || value instanceof Boolean) {
                info.put(key, String.valueOf(value));
            }
        }
        return info;
    }

    /** One row per wavelength; absent or non-numeric values become NaN gaps. */
    private static double[] jsonDoubles(org.json.JSONArray array) {
        if (array == null) return null;
        final double[] values = new double[array.length()];
        for (int i = 0; i < values.length; ++i) {
            values[i] = array.optDouble(i, Double.NaN);
        }
        return values;
    }

    /** A matrix of channel values; a single flat array is one channel. */
    private static double[][] jsonRows(org.json.JSONArray array) {
        if (array == null || array.length() == 0) return null;
        if (array.optJSONArray(0) == null) {
            final double[] flat = jsonDoubles(array);
            final double[][] rows = new double[flat.length][];
            for (int i = 0; i < rows.length; ++i) rows[i] = new double[]{flat[i]};
            return rows;
        }
        final double[][] rows = new double[array.length()][];
        for (int i = 0; i < rows.length; ++i) {
            rows[i] = jsonDoubles(array.optJSONArray(i));
        }
        return rows;
    }

    private static void readStocks(org.json.JSONArray array, List<String> ids, List<String> labels,
                                   List<String> types) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            org.json.JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id");
            if (id.isEmpty()) continue;
            String name = o.optString("name", id);
            StringBuilder label = new StringBuilder(name);
            String type = o.optString("type", "");
            String use = o.optString("use", "");
            if ("positive".equals(type)) label.append(" · 反转");
            else if ("negative".equals(type)) label.append(" · 负片");
            if ("cine".equals(use)) label.append(" · 电影");
            ids.add(id);
            labels.add(label.toString());
            if (types != null) types.add(type);
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) result = getResources().getDimensionPixelSize(resourceId);
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
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"));
        chain.add(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"));
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
        chain.add(documents);
        Intent content = new Intent(Intent.ACTION_GET_CONTENT);
        content.addCategory(Intent.CATEGORY_OPENABLE);
        content.setType("*/*");
        content.putExtra(Intent.EXTRA_MIME_TYPES, types);
        chain.add(content);
        if (!launchFirstResolvable(chain, 1)) {
            showSnackbar("本机没有可用的文件选择器");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1) return;
        if (resultCode != RESULT_OK) return;          // the user cancelled
        // RESULT_OK with no URI happens on ROMs whose picker cannot hand back a
        // stream for the item (a cloud-only photo, a broken provider). Doing
        // nothing there is what "下载后没法导入" looks like from the outside: the
        // tap simply has no effect and nothing is said.
        Uri uri = data == null ? null : data.getData();
        if (uri == null) {
            try { DebugSink.text("picker", "RESULT_OK with no data"); } catch (Throwable ignored) { }
            showSnackbar("没有拿到图片（选择器返回空）。请点「导入」→「从文件管理器选择」再试一次");
            return;
        }
        loadImage(uri);
    }

    private void loadImage(Uri uri) {
        /** Re-opens a chip; captured before rememberFrame consumes the flag. */
    final boolean reopening = reopeningFrame;
    sourceUri = uri;
        // A new import works out its own imprint date; the previous frame's
        // cached text must not travel with the new Source.
        dateText = null;
        defaultDateText = null;
        // The frame's own look, stated before any session is built: a chip
        // returns to a photograph *and* to what was done to it; a pick from ＋
        // starts with no film at all. Scan inspect belongs to the same Frame,
        // so its position comes back with it.
        if (reopeningFrame) {
            final Frame target = (currentFrame >= 0 && currentFrame < frames.size())
                    ? frames.get(currentFrame) : null;
            restoreScanStateFromFrame(target);
            if (target != null) applyLook(target.look);
        } else {
            saveScanStateIntoFrame();
            restoreScanStateFromFrame(null);
            applyLook(defaultLook());
        }
        lookHistory.reset(captureLook());
        updateUndoAction();
        try {
            // Check if this is a RAW file by extension
            String fileName = getFileName(uri);
            final String lower = fileName == null ? "" : fileName.toLowerCase();
            isRawFile = lower.endsWith(".rw2") || lower.endsWith(".raw")
                    || lower.endsWith(".dng") || lower.endsWith(".arw")
                    || lower.endsWith(".nef") || lower.endsWith(".cr2")
                    || lower.endsWith(".cr3") || lower.endsWith(".orf")
                    || lower.endsWith(".raf") || lower.endsWith(".pef")
                    || lower.endsWith(".srw") || lower.endsWith(".x3f")
                    || lower.endsWith(".rwl") || lower.endsWith(".3fr")
                    || lower.endsWith(".mrw") || lower.endsWith(".erf");
            // A RAW container we know of but cannot open still beats decoding
            // its bytes as a JPEG and reporting a mystery.
            final String[] knownButUnsupported = {".heic.raw", ".iiq", ".mos"};
            for (String ext : knownButUnsupported) {
                if (lower.endsWith(ext)) {
                    showSnackbar("[E-1107] 暂不支持 " + ext + " 格式");
                    return;
                }
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
            if (!importAtNativeSize && !importBudgetAccepted && !suppressBudgetDialog) {
                final int[] full = sniffImageSize(uri);
                if (full != null) {
                    final long area = (long) full[0] * (long) full[1];
                    final int cap = maxImportPixels();
                    if (area > cap || Math.max(full[0], full[1]) > IMPORT_LONG_EDGE) {
                        showImportBudgetDialog(uri, full[0], full[1], cap);
                        return;
                    }
                }
            }
            importBudgetAccepted = false;

            // Free the previous frame before decoding: a 20 MP pick wants a
            // ~200 MB direct buffer and the old session still holds its planes
            // (this OOM was 241 MB with 201 MB left in the heap).
            //
            // **Under the render lock.** Closing a session out from under a
            // render is the SIGSEGV this device logs as `vkCmdCopyBuffer`
            // against a destroyed buffer.
            synchronized (renderMutex) {
                if (session != null) { session.close(); session = null; }
            }
            lastRenderedBitmap = null;
            setOpticsUnlocked(false);
            setStage(0, null);
            sourceBitmap = null;
            sourcePixels = null;
            // The canvas is not a control. `setOriginalVisual` only paints the
            // 原图 button; what CompareView holds is `after` (the last render)
            // and it survives a new session. That was the whole "I changed the
            // photo and the canvas still shows the old one" report: the frame
            // was never dropped, so the next render was the first thing to
            // replace it.
            if (compare != null) compare.clear();
            System.gc();

            // Cap by area, the way the RAW path does. inSampleSize only halves,
            // so one exact scale follows if the sample still overshoots.
            final boolean nativeSize = importAtNativeSize;
            importAtNativeSize = false;                       // one import only
            // A cap the last render failure handed us (see offerSmallerImport):
            // one import's worth, then back to the device's own answer.
            final int forcedCap = pendingCapPixels;
            pendingCapPixels = 0;
            e1001Offered = false;
            final int capPixels = nativeSize ? Integer.MAX_VALUE
                    : (forcedCap > 0 ? forcedCap : maxImportPixels());
            final int edgeCap = nativeSize ? Integer.MAX_VALUE : IMPORT_LONG_EDGE;
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
                            if (which == 0) pickRawFile();
                        });
                return;
            }
            sourceBitmap = applyExifRotation(uri, sourceBitmap);
            final long area = (long) sourceBitmap.getWidth() * sourceBitmap.getHeight();
            final int longEdge = Math.max(sourceBitmap.getWidth(), sourceBitmap.getHeight());
            // Three ceilings in three units, and the strictest wins: area is
            // quadratic in the scale, the long edge is linear, and the third is
            // what this phone can actually hold -- the import's float buffer is
            // 12 B/px, which is 240 MB at 20 MP and the largest single
            // allocation the app makes. Asking before taking it is what turns
            // an OutOfMemoryError (an Error, so the old `catch (Exception)`
            // never saw it and the process died) into a smaller import.
            double k = 1.0;
            if (area > capPixels) k = Math.min(k, Math.sqrt((double) capPixels / (double) area));
            if (longEdge > edgeCap) k = Math.min(k, (double) edgeCap / longEdge);
            final long availBytes = availableMemoryBytes();
            if (availBytes > 0) {
                final long budget = Math.max(1L << 20, availBytes - (128L << 20));
                final double wantBytes = (double) area * 12.0;
                if (wantBytes > budget) k = Math.min(k, Math.sqrt((double) budget / wantBytes));
            }
            if (k < 1.0) {
                final int nw = Math.max(1, (int) Math.round(sourceBitmap.getWidth() * k));
                final int nh = Math.max(1, (int) Math.round(sourceBitmap.getHeight() * k));
                Bitmap scaled = Bitmap.createScaledBitmap(sourceBitmap, nw, nh, true);
                if (scaled != sourceBitmap) sourceBitmap.recycle();
                sourceBitmap = scaled;
                try { DebugSink.text("import-scale", "k=" + k + " -> " + nw + "x" + nh
                        + " avail=" + availBytes); } catch (Throwable ignored) { }
            }
            sourceWidth = sourceBitmap.getWidth();
            sourceHeight = sourceBitmap.getHeight();
            refreshEdgeFormats();

            // Convert a row at a time: a full int[] of 20 MP would be another
            // 80 MB on the Java heap next to the direct buffer.
            sourcePixels = ByteBuffer.allocateDirect(sourceWidth * sourceHeight * 3 * 4)
                .order(ByteOrder.nativeOrder());
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
            
            final int pw = Math.max(1, Math.round(sourceWidth * Math.min(1f, 1024f / Math.max(sourceWidth, sourceHeight))));
            final int ph = Math.max(1, Math.round(sourceHeight * Math.min(1f, 1024f / Math.max(sourceWidth, sourceHeight))));
            beforeBitmap = Bitmap.createScaledBitmap(sourceBitmap, pw, ph, true);
            // A source already within the preview edge is returned unchanged
            // by createScaledBitmap. Recycling it here would leave CompareView
            // holding a recycled bitmap and crash the next draw.
            if (beforeBitmap != sourceBitmap) sourceBitmap.recycle();
            sourceBitmap = null;
            compare.setBefore(beforeBitmap);
            statusText.setText("Local preview");
            setStage(1, getFileName(uri));
            rememberFrame(uri);
            refreshFilmPanel();
            setStep(1);
            
            // Create engine and session
            ensureEngine();
            synchronized (renderMutex) {
                if (session != null) { session.close(); session = null; }
            }
            session = engine.openSession(sourcePixels, sourceWidth, sourceHeight, 3);
            // No parameter write here: a fresh session is stated by the render
            // that produces the picture (see `renderImage`), on the render
            // thread, so the UI never waits behind the engine's lock.
            lastImportError = null;
            // An import is not a look. The frame lands unrendered with no film
            // chosen; the first render happens when the user picks a stock.
            showingOriginal = true;
            lastRenderedBitmap = null;
            if (!reopening) brandFilter = FilmBrand.ALL;
            setOriginalVisual(true);
            refreshFilmPanel();
            if (filmStock != null) {
                // A chip came back with a look: develop it again rather than
                // making the user re-pick a film that was never unpicked.
                showingOriginal = false;
                refreshFilmPanel();
                renderImage();
            } else {
                statusText.setText("未渲染 · 选择胶片开始");
            }
        } catch (Throwable t) {
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
     * 8000 is where a frame stops being plausible rather than where it stops
     * working: measured on the SM8735 here, 7500 x 5000 (37.5 MP) renders in
     * 32 s with a 2.7 GB pool, 8000 x 5333 (42.7 MP) in 57 s, and 8192 x 5464
     * (44.8 MP) loses the device inside `spk_iir_df_acc`. A 45 MP camera raw
     * is 8256 x 5504, so this lands it at 8000 x 5333 rather than at 4700.
     */
    private static final int IMPORT_LONG_EDGE = 8000;
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
            android.app.ActivityManager am =
                    (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return mi.availMem;
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Pixels of **source** this device will render, from three ceilings.
     *
     * The binding ceiling comes from the engine itself (`spk_max_plane_bytes`,
     * a direct call) and only falls back to the parsed profile -- because the
     * profile is JSON and the JSON can fail to parse, and a failed parse used to
     * read as "no limit". A device whose driver reports the Vulkan minimum
     * (128 MiB = 11.2 MP) then accepted a 14.8 MP import and died at render.
     *
     * `withRebate` divides by the film canvas's growth: the ceiling applies to
     * the **canvas**, not to the source, and the canvas is wider than the frame
     * by the film's own width and by the margin along it. Measured from the
     * engine's gate table (`overscan.cpp`, kFormats) and confirmed against real
     * renders:
     *
     *     135        1.51   (4700x3133 -> 4914x4538)
     *     135 half   1.53
     *     120 645    1.18   (1952x2604 -> 2116x2833, measured)
     *     120 6x9    1.13
     *     135 XPan   1.50
     *
     * 1.6 covers the worst of them with margin. A frame that lands a little
     * small is a picture; one that lands over the limit is a dead end.
     */
    private int maxImportPixels(boolean withRebate) {
        try { ensureEngine(); } catch (Throwable ignored) { }
        probeNow();
        long byBinding = Long.MAX_VALUE;
        try {
            final int byNative = engine == null ? 0 : engine.maxFramePixels();
            if (byNative > 0) byBinding = byNative;
        } catch (Throwable ignored) { }
        if (byBinding == Long.MAX_VALUE) byBinding = probe.maxPixelsByBinding();
        final long byMemory = DeviceProbe.maxPixelsByMemory(availableMemoryBytes());
        // The hard cap is measured, not chosen: 42.7 MP renders here, 44.8 MP
        // takes the device down, and the pool's peak is 6.3 planes (76 B/px).
        long px = Math.min(Math.min(byBinding, byMemory), DeviceProbe.HARD_PIXEL_CAP);
        if (withRebate) px = (long) (px / 1.6);
        return (int) Math.min(Math.max(px, 1_000_000L), Integer.MAX_VALUE);
    }

    private int maxImportPixels() {
        return maxImportPixels(overscanOn);
    }

    private void loadRawImage(Uri uri) {
        final boolean reopening = reopeningFrame;
        // The Look the RAW session must carry, snapshotted here on the UI
        // thread: `solve` below meters with it, and the worker never reads a
        // control.
        final String importParams = filmStock == null
                ? null : snapshotRestatement(filmStock, PREVIEW_LONG_EDGE);
        try {
            String filePath = getFilePathFromUri(uri);
            rawFilePath = filePath;
            if (filePath == null) {
                showSnackbar("无法获取RAW文件路径");
                return;
            }
            
            statusText.setText("Decoding RAW…");
            // **The same two things the JPEG path does, for the same reasons**
            // — and this path had none of them, which is why a RAW import kept
            // the previous photograph everywhere except the little source
            // thumbnail:
            //   · compare.clear() drops `CompareView.after`, the previous
            //     render, which nothing else ever clears;
            //   · the last render and the optics lock are reset below.
            if (compare != null) compare.clear();
            lastRenderedBitmap = null;
            setOpticsUnlocked(false);
            setStage(2, getFileName(uri));

            new Thread(() -> {
                try {
                    String resourcesDir = getResourcesDir();
                    if (engine == null) {
                        vkLogFile().delete();
                        engine = SpektraLabEngine.create(resourcesDir, vkLogFile().getAbsolutePath());
                        uploadCapabilities();
                    }
                    // Under the render lock: closing a session out from under
                    // a render is a native crash, not an exception.
                    synchronized (renderMutex) {
                        if (session != null) { session.close(); session = null; }
                    }
                    
                    // `film_stock` names a profile file: "" and "null" both make
                    // `load_profile` fail ("bad stock name"), so an unchosen film
                    // omits the key and the engine keeps its own default. The UI
                    // still shows 原图 and renders nothing until a chip is picked.
                    final String filmField = filmStock == null ? ""
                            : String.format("\"film_stock\":\"%s\",", filmStock);
                    String params = String.format(
                        "{%s\"print_stock\":\"%s\",\"input_color_space\":\"ProPhoto RGB\",\"input_cctf_decoding\":false,\"output_color_space\":\"sRGB\",\"print_exposure\":%.2f,\"preview_long_edge\":2048}",
                        filmField, printStock,
                        optics.get(OpticsDelta.Param.EXPOSURE)
                    );
                    
                    try { DebugSink.text("open-params", "path=" + filePath + "\n" + params); } catch (Throwable ignored) {}
                    // The cap comes from the device, not from a constant: a
                    // plane is 12 B/px and a binding may not exceed the GPU's
                    // maxStorageBufferRange. Two limits apply, whichever is
                    // lower -- that range, and the 65 535-workgroup dispatch
                    // limit (16,776,960 px) this driver still loses blocks over.
                    final boolean nativeSize = importAtNativeSize;
                    importAtNativeSize = false;                   // one import only
                    if (nativeSize) {
                        try { DebugSink.text("import-mode", "native size, no cap"); } catch (Throwable ignored) { }
                    }
                    // Keep the caps so a RAW export can decode the same frame
                    // in its own session.
                    rawImportMaxPixels = nativeSize ? 0 : maxImportPixels();
                    rawImportLongEdge = nativeSize ? 0 : IMPORT_LONG_EDGE;
                    session = engine.openRawSession(filePath, params,
                            rawImportMaxPixels, rawImportLongEdge);
                    lastImportError = null;
                    // **The session's own parameters, which this path never sent.**
                    // `openRawSession` carries the film, the paper and the decode
                    // colour space; it does not carry grain, halation, glare, the
                    // format or the rebate. The Look was snapshotted on the UI
                    // thread, so `solve` below meters with the same values the
                    // first preview will render.
                    if (importParams != null) session.setParams(importParams);

                    // **The raw itself**, which is the "before" side of every
                    // comparison and the only honest thing to show on import:
                    // the decoded frame in working space, packed with no
                    // transfer function, so a display reads it the way it reads
                    // a raw -- flat, and dark. The camera's own JPEG is **not**
                    // a stand-in for it: a preview is the camera's rendering,
                    // not this frame, and showing it as "原图" is the same lie
                    // as importing it would be. No frame, no picture.
                    Bitmap rawView = null;
                    try {
                        final int[] d = session.exportSource("live");
                        if (d != null && d[0] > 0 && d[1] > 0) {
                            final int rw = d[0], rh = d[1];
                            final int[] px = new int[rw * rh];
                            if (session.copyArgbRange(px, 0, rh) == rw * rh) {
                                rawView = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888);
                                rawView.setPixels(px, 0, rw, 0, 0, rw, rh);
                            }
                        }
                        try {
                            int probePixel = 0;
                            if (rawView != null) {
                                int[] one = new int[1];
                                rawView.getPixels(one, 0, 1, Math.max(0, rawView.getWidth() / 2),
                                                  Math.max(0, rawView.getHeight() / 2), 1, 1);
                                probePixel = one[0];
                            }
                            DebugSink.text("raw-view", rawView == null ? "export: empty"
                                    : "export " + rawView.getWidth() + "x" + rawView.getHeight()
                                      + " mid=" + Integer.toHexString(probePixel));
                        }
                        catch (Throwable ignored) { }
                    } catch (Throwable t) {
                        try { DebugSink.text("raw-view", "export failed: " + t); } catch (Throwable ignored) { }
                    }
                    final Bitmap preview = rawView;
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
                        runOnUiThread(() -> {
                            beforeBitmap = preview;
                            compare.setBefore(preview);
                            setStage(1, getFileName(uri));
                            rememberFrame(uri);
                            refreshFilmPanel();
                            setStep(1);
                            statusText.setText("Raw preview");
                        });
                    }

                    // "Process" (配光): without a solved print exposure and
                    // filter pack the print runs on defaults and blows the
                    // highlights. The mac app makes the user press Process;
                    // this build does it once on open.
                    try {
                        String solved = session.solve("both");
                        DebugSink.text("solve", solved == null ? "(null)" : solved);
                    } catch (Throwable se) {
                        DebugSink.text("solve-failed", String.valueOf(se));
                    }
                    runOnUiThread(() -> {
                        if (session == null) return;
                        showingOriginal = true;
                        lastRenderedBitmap = null;
                        if (!reopening) brandFilter = FilmBrand.ALL;
                        setOriginalVisual(true);
                        refreshFilmPanel();
                        if (filmStock != null) {
                            showingOriginal = false;
                            refreshFilmPanel();
                            renderImage();
                        } else {
                            statusText.setText("未渲染 · 选择胶片开始");
                        }
                    });

                    // Get dimensions from session
                    int[] dims = session.getDimensions();
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
                } catch (Throwable t) {
                    final String said = describeImportFailure(t);
                    lastImportError = said;
                    // A High-Efficiency NEF is not a preview job. Say what it
                    // is (E-1108, a compression this build has no decoder for)
                    // and what to change, and leave the file alone.
                    runOnUiThread(() -> {
                        statusText.setText("RAW加载失败 " + SpkError.shortForm(said));
                        showErrorSnackbar("RAW 加载失败", said);
                    });
                }
            }).start();
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
    private void showErrorDialog(String title, String body) {
        final String shownTitle = (title == null || title.isEmpty()) ? "错误" : title;
        final String shownBody = (body == null || body.isEmpty()) ? "[E-0000] 未知错误" : body;
        runOnUiThread(() -> {
            final android.app.Dialog dialog = new android.app.Dialog(this);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(m3Role(
                    com.google.android.material.R.attr.colorSurfaceContainerHigh,
                    Palette.Role.SURFACE_ELEVATED));
            View cap = new View(this);
            cap.setBackgroundColor(m3Role(com.google.android.material.R.attr.colorError,
                    Palette.Role.ACCENT));
            card.addView(cap, new LinearLayout.LayoutParams(-1, dp(3)));

            LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(android.view.Gravity.CENTER_VERTICAL);
            head.setPadding(dp(16), 0, dp(16), 0);
            TextView kicker = mixedM3Label("ERROR / 错误详情",
                    M3Tokens.TypeRole.LABEL_LARGE, m3Primary());
            head.addView(kicker, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView close = m3Label("关闭", M3Tokens.TypeRole.LABEL_LARGE,
                    m3OnSurfaceVariant(), tfHan);
            close.setPaintFlags(close.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            close.setPadding(dp(12), dp(14), 0, dp(14));
            close.setOnClickListener(v -> dialog.dismiss());
            head.addView(close);
            card.addView(head, new LinearLayout.LayoutParams(-1, dp(42)));

            ScrollView scroll = new ScrollView(this);
            LinearLayout bodyBox = new LinearLayout(this);
            bodyBox.setOrientation(LinearLayout.VERTICAL);
            bodyBox.setPadding(dp(16), 0, dp(16), 4);
            TextView titleView = m3Label(shownTitle, M3Tokens.TypeRole.TITLE_LARGE,
                    m3OnSurface(), tfHan);
            bodyBox.addView(titleView);
            TextView advice = m3Label("请复制下面的完整信息，方便定位问题。",
                    M3Tokens.TypeRole.BODY_MEDIUM, m3OnSurfaceVariant(), tfHan);
            LinearLayout.LayoutParams adviceLp = new LinearLayout.LayoutParams(-1, -2);
            adviceLp.topMargin = dp(8);
            bodyBox.addView(advice, adviceLp);
            TextView detail = m3Label(shownBody, M3Tokens.TypeRole.BODY_SMALL,
                    m3OnSurface(), tfHan);
            detail.setTextIsSelectable(true);
            detail.setPadding(dp(12), dp(11), dp(12), dp(11));
            android.graphics.drawable.GradientDrawable detailBg = new android.graphics.drawable.GradientDrawable();
            detailBg.setColor(m3Role(
                    com.google.android.material.R.attr.colorSurfaceContainerLowest,
                    Palette.Role.SURFACE_SUNKEN));
            detail.setBackground(detailBg);
            LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
            detailLp.topMargin = dp(14);
            bodyBox.addView(detail, detailLp);
            scroll.addView(bodyBox, new android.widget.FrameLayout.LayoutParams(-1, -2));
            int bodyHeight = (int) Math.min(dp(340), getResources().getDisplayMetrics().heightPixels * 0.44f);
            card.addView(scroll, new LinearLayout.LayoutParams(-1, bodyHeight));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(dp(16), dp(14), dp(16), dp(16));
            TextView closeAction = m3Label("关闭", M3Tokens.TypeRole.LABEL_LARGE,
                    m3Primary(), tfHan);
            closeAction.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
            closeBg.setColor(m3Role(
                    com.google.android.material.R.attr.colorSurfaceContainerHigh,
                    Palette.Role.SURFACE_ELEVATED));
            closeBg.setStroke(dp(1), m3Primary());
            closeAction.setBackground(closeBg);
            closeAction.setOnClickListener(v -> dialog.dismiss());
            LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            closeLp.rightMargin = dp(8);
            actions.addView(closeAction, closeLp);
            TextView copy = m3Label("复制错误信息", M3Tokens.TypeRole.LABEL_LARGE,
                    m3Role(com.google.android.material.R.attr.colorOnPrimary,
                            Palette.Role.ON_ACCENT), tfHan);
            copy.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable copyBg = new android.graphics.drawable.GradientDrawable();
            copyBg.setColor(m3Primary());
            copy.setBackground(copyBg);
            copy.setOnClickListener(v -> {
                try {
                    final String text = shownTitle + "\n\n" + shownBody
                            + "\n\nbuild=SpektraLab Android " + UpdateCheck.versionName(this)
                            + "\n" + probe.describe();
                    android.content.ClipboardManager cm =
                            (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("SpektraLab", text));
                    copy.setText("已复制");
                } catch (Throwable t) {
                    copy.setText("复制失败 · 请长按详情");
                }
            });
            actions.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1.45f));
            card.addView(actions, new LinearLayout.LayoutParams(-1, -2));

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
        });
    }

    private int extractInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        idx += key.length();
        int end = json.indexOf(",", idx);
        if (end < 0) end = json.indexOf("}", idx);
        if (end < 0) return 0;
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
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
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
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
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
            fromPlatform = new android.media.ExifInterface(path).getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL);
        } catch (Throwable t) {
            try { DebugSink.text("exif", "path: " + t); } catch (Throwable ignored) { }
        }
        if (fromPlatform > 0) return fromPlatform;
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
            if (file.length() < 16) return -1;
            final byte[] head = new byte[8];
            file.readFully(head);
            final boolean little = head[0] == 0x49 && head[1] == 0x49;
            final int magic = shortAt(head, 2, little);
            if (magic != 42 && magic != 0x55) return -1;
            final long ifd = longAt(head, 4, little);
            if (ifd <= 0 || ifd > file.length() - 2) return -1;
            file.seek(ifd);
            final byte[] count = new byte[2];
            file.readFully(count);
            final int entries = shortAt(count, 0, little);
            if (entries <= 0 || entries > 512) return -1;
            final byte[] table = new byte[entries * 12];
            file.readFully(table);
            for (int i = 0; i < entries; ++i) {
                final int base = i * 12;
                if (shortAt(table, base, little) != 0x0112) continue;      // Orientation
                if (shortAt(table, base + 2, little) != 3) continue;       // SHORT
                return shortAt(table, base + 8, little);
            }
        } catch (Throwable t) {
            return -1;
        } finally {
            if (file != null) try { file.close(); } catch (Throwable ignored) { }
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
                return new android.media.ExifInterface(in).getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Throwable ignored) { }
        try (android.os.ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r")) {
            if (fd != null) {
                return new android.media.ExifInterface(fd.getFileDescriptor()).getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL);
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
                if (head != null) BitmapFactory.decodeStream(head, null, bounds);
            } catch (Throwable t) {
                try { DebugSink.text("decode", "sniff " + t); } catch (Throwable ignored) { }
            }
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                int sample = 1;
                while ((long) (bounds.outWidth / (sample * 2)) * (bounds.outHeight / (sample * 2)) >= capPixels
                        || Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= edgeCap) sample *= 2;
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = sample;
                try (InputStream stream = getContentResolver().openInputStream(uri)) {
                    Bitmap decoded = BitmapFactory.decodeStream(stream, null, opts);
                    if (decoded != null) return decoded;
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
        if (android.os.Build.VERSION.SDK_INT < 28) return null;
        try {
            final android.graphics.ImageDecoder.Source src =
                    android.graphics.ImageDecoder.createSource(getContentResolver(), uri);
            Bitmap decoded = android.graphics.ImageDecoder.decodeBitmap(src,
                    (decoder, info, source) -> {
                        final android.util.Size size = info.getSize();
                        final int w = size.getWidth(), h = size.getHeight();
                        double k = 1.0;
                        final long area = (long) w * (long) h;
                        if (area > capPixels) k = Math.min(k, Math.sqrt((double) capPixels / area));
                        final int longEdge = Math.max(w, h);
                        if (longEdge > edgeCap) k = Math.min(k, (double) edgeCap / longEdge);
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
        if (orientation <= 0) return bitmap;      // unreadable: leave the pixels alone
        try {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            switch (orientation) {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90); break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180); break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270); break;
                case android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1); break;
                case android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1, -1); break;
                // The two mirrored rotations. Rare, but a phone that writes 5
                // or 7 and lands in the default branch shows its photo sideways.
                case android.media.ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.postRotate(90); matrix.postScale(-1, 1); break;
                case android.media.ExifInterface.ORIENTATION_TRANSVERSE:
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
        if (session == null || !uiReady) return;
        if (filmStock == null) {
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
    private String snapshotRestatement(String stock, int longEdge) {
        final Look look = captureLook();
        look.filmStock = stock;
        // The typed text is on the Look; this is only the Frame's default.
        return LookRestatement.json(look, new LookRestatement.Context(
                isRawFile,
                longEdge,
                isPositiveFilm(stock),
                edgeTextFor(stock),
                look.dateOn ? defaultImprintDate() : ""));
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

    private void renderImage() {
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
        if (filmStock == null) {
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
        final String renderedStock = filmStock;
        // A settled edit is whatever Look the next render will use. Recording
        // here catches every control path (slider release, stock, switch, reset)
        // without each one having to remember undo.
        final Look renderLook = captureLook();
        if (!restoringUndo) lookHistory.record(renderLook);
        updateUndoAction();
        // The view layer is a process-global native pass, so snapshot its
        // values with the Look and send them under the render lock.
        final double[] viewAdjust = renderLook.view.toNative();
        // Snapshot the whole Look here, on the UI thread: the render thread gets
        // one finished string and never reads a control or a Frame.
        final String previewParams;
        try {
            previewParams = snapshotRestatement(renderedStock, PREVIEW_LONG_EDGE);
            // The line a "every film looks the same" report needs: what this
            // render is about to tell the engine, next to the UI selection.
            DebugSink.text("params", "draw film=" + renderedStock + " print=" + printStock
                    + " render=" + rendering + " long=" + PREVIEW_LONG_EDGE);
        } catch (Throwable t) {
            showErrorSnackbar("渲染参数有误", describe(t));
            return;
        }
        rendering = true;
        progressBar.setVisibility(View.VISIBLE);
        busyLabel.setVisibility(View.VISIBLE);
        statusText.setText("Rendering…");
        try { DebugSink.text("render", "begin " + sourceWidth + "x" + sourceHeight); }
        catch (Throwable ignored) { }

        new Thread(() -> {
            try {
                synchronized (renderMutex) {
                if (session == null) throw new RuntimeException("session closed before render");
                // The finished Look snapshot, taken on the UI thread before
                // this thread started. Under the render lock it is one writer.
                SpektraLabEngine.setViewAdjust(viewAdjust);
                session.setParams(previewParams);
                int[] dims = session.renderBegin("live");
                int w = dims[0], h = dims[1];
                if (w <= 0 || h <= 0) throw new RuntimeException("engine returned empty frame");

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
                runOnUiThread(() -> {
                    showingOriginal = false;
                    compare.setAfter(bmp);
                    setStage(1, null);
                    setStep(2);
                    lastRenderedBitmap = bmp;
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
                    } catch (Throwable ignored) { }
                    progressBar.setVisibility(View.GONE);
                    busyLabel.setVisibility(View.GONE);
                    rendering = false;
                    if (pendingRender) { pendingRender = false; renderImage(); }
                });
            }
            } catch (Throwable e) {
                try { session.renderEnd(); } catch (Throwable ignored) {}
                final String msg = describe(e);
                runOnUiThread(() -> {
                    statusText.setText("渲染失败 " + SpkError.shortForm(msg));
                    progressBar.setVisibility(View.GONE);
                    busyLabel.setVisibility(View.GONE);
                    rendering = false;
                    // The foot is one 7 sp line: a failure has to be readable,
                    // and it has to arrive with enough context to be reported.
                    showErrorSnackbar("渲染失败", SpkError.format(msg)
                            + "\n\n" + sourceWidth + "×" + sourceHeight
                            + "\n" + probe.describe());
                    if (pendingRender) { pendingRender = false; renderImage(); }
                });
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
                if (code == 1001 || code == 1009) offerSmallerImport();
            }
        }).start();
    }

    /**
     * Re-import at a size this device can actually bind, then render again.
     * One offer per import: a loop of dialogs is worse than the error.
     */
    private void offerSmallerImport() {
        if (sourceUri == null || e1001Offered) return;
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
    private void uploadStaleExportProgress() {
        try {
            java.io.File[] files = getCacheDir().listFiles();
            if (files == null) return;
            for (java.io.File f : files) {
                if (!f.getName().endsWith(".prog")) continue;
                byte[] buf = new byte[(int) f.length()];
                try (java.io.InputStream in = new java.io.FileInputStream(f)) {
                    int off = 0, n;
                    while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
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
            if (caps != null) DebugSink.text("capabilities", caps);
            String mem = engine != null ? engine.memoryReport() : null;
            if (mem != null) DebugSink.text("memory-report", mem);
            uploadStaleExportProgress();
        } catch (Throwable ignored) {}
    }

    /** Upload the native export black box, if the JNI left one. */
    private void uploadExportProgress(java.io.File out) {
        try {
            java.io.File prog = new java.io.File(out.getAbsolutePath() + ".prog");
            if (!prog.exists()) return;
            byte[] buf = new byte[(int) prog.length()];
            try (java.io.InputStream in = new java.io.FileInputStream(prog)) {
                int off = 0, n;
                while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
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
            if (g == null) return "";
            final int wAt = g.indexOf("\"canvas_w\":");
            final int hAt = g.indexOf("\"canvas_h\":");
            if (wAt < 0 || hAt < 0) return "";
            final int w0 = wAt + 11, w1 = g.indexOf(',', w0);
            final int h0 = hAt + 11, h1 = g.indexOf(',', h0);
            if (w1 < 0 || h1 < 0) return "";
            return g.substring(w0, w1) + "x" + g.substring(h0, h1);
        } catch (Throwable ignored) { return ""; }
    }

    private void exportImage(String format, String tier) {
        final boolean tiff = "tiff".equals(format);
        final boolean dng = "dng".equals(format);
        final String ext = dng ? ".dng" : tiff ? ".tif" : ".jpg";
        final String mime = dng ? "image/x-adobe-dng" : tiff ? "image/tiff" : "image/jpeg";
        if (sharePill != null) sharePill.setVisibility(View.GONE);
        if (engine == null || session == null) {
            showSnackbar("请先导入照片");
            return;
        }
        if (filmStock == null) {
            showSnackbar("先选一张胶片，再导出");
            return;
        }
        // The export states the Look it was started with, not whatever the
        // controls say by the time the render thread picks it up: a Frame
        // switch or a slider drag during a long export must not move the file.
        final String exportStock = filmStock;
        final Look exportLook = captureLook();
        final double[] exportViewAdjust = exportLook.view.toNative();
        final String exportParams = snapshotRestatement(exportStock, PREVIEW_LONG_EDGE);
        final String rawExportPath = isRawFile ? (rawFilePath == null ? "" : rawFilePath) : null;
        // A bitmap export renders from the preview session; a RAW export opens
        // its own below. The render lock keeps a stock change out of either.
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
                    DebugSink.text("export-params", "film=" + exportStock + " print=" + printStock
                            + " raw=" + (rawExportPath != null) + " " + exportParams);
                } catch (Throwable ignored) {}

                final long t0 = System.currentTimeMillis();
                final java.util.concurrent.atomic.AtomicBoolean spinning =
                        new java.util.concurrent.atomic.AtomicBoolean(true);
                final android.os.Handler ticker = new android.os.Handler(android.os.Looper.getMainLooper());
                Runnable tick = new Runnable() {
                    @Override public void run() {
                        if (!spinning.get()) return;
                        long el = (System.currentTimeMillis() - t0) / 1000;
                        statusText.setText("导出中（" + tier + "）：渲染中 " + el + " 秒...");
                        ticker.postDelayed(this, 1000);
                    }
                };
                runOnUiThread(tick);
                int[] info;
                final String canvas;
                try {
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
                            SpektraLabEngine.setViewAdjust(exportViewAdjust);
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
                            if (rawExport != null) rawExport.close();
                        }
                    }
                } finally {
                    spinning.set(false);
                }
                uploadExportProgress(tmp);
                uploadVkLog();
                int w = info[0], h = info[1];
                long renderMs = info[2];
                if (w <= 0 || h <= 0 || !tmp.exists() || tmp.length() == 0) {
                    throw new RuntimeException("引擎没有输出图像");
                }

                final int fw = w, fh = h;
                runOnUiThread(() -> statusText.setText("导出中：写入相册 " + fw + "x" + fh + "..."));
                String fileName = "SpektraLab_" + System.currentTimeMillis() + ext;
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, mime);
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_PICTURES + "/SpektraLab");
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
                android.content.ContentResolver resolver = getContentResolver();
                Uri outUri = resolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (outUri == null) throw new RuntimeException("无法创建媒体文件");
                try (java.io.InputStream in = new java.io.FileInputStream(tmp);
                     java.io.OutputStream out = resolver.openOutputStream(outUri)) {
                    if (out == null) throw new RuntimeException("无法打开输出流");
                    byte[] buf = new byte[262144];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }
                values.clear();
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(outUri, values, null, null);

                try {
                    long len = tmp.length();
                    DebugSink.text("export", "format=" + format + " tier=" + tier + " w=" + w + " h=" + h + " renderMs=" + renderMs
                            + " bytes=" + len);
                    if (len > 0 && len < 30L * 1024 * 1024) {
                        byte[] buf = new byte[(int) len];
                        try (java.io.InputStream in = new java.io.FileInputStream(tmp)) {
                            int off = 0, n;
                            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
                        }
                        DebugSink.bytes("export", "out-" + tier + ext, buf, mime);
                    }
                } catch (Throwable ignored) {}
                final long secs = Math.max(1, (System.currentTimeMillis() - t0) / 1000);
                final long fRender = renderMs;
                final long bytes = tmp.exists() ? tmp.length() : 0;
                final String sizeLabel = bytes > 0
                        ? String.format("%.1f MB", bytes / 1048576.0) : "?";
                runOnUiThread(() -> {
                    lastExportUri = outUri;
                    lastExportMime = mime;
                    // A separate control, and only when it has something to do:
                    // every other format opens where it lands, and a permanent
                    // share button on a photograph app reads as "post to
                    // social", which is not what this is.
                    if (sharePill != null) sharePill.setVisibility((tiff || dng) ? View.VISIBLE : View.GONE);
                    // Dimensions and file size together: "3.3 MB" alone reads as
                    // a bug when the frame is 16 MP, and the reason is worth
                    // saying out loud rather than guessing at later.
                    final String canvasNote = canvas.isEmpty() ? "" : " · 画布 " + canvas;
                    final String tiffHint = tiff
                            ? "（TIFF：点导出旁边的「分享」发给手机版 Lightroom 即可使用）"
                            : dng ? "（DNG：线性 16 位，给电脑上的 Lightroom / ACR；手机版打不开，已知问题，不必反馈）" : "";
                    showSnackbar("已保存 " + fw + "x" + fh + canvasNote + " · " + sizeLabel + tiffHint);
                    statusText.setText("已导出 " + fw + "x" + fh + canvasNote + " · " + sizeLabel
                        + "（" + secs + " 秒，其中渲染 " + (fRender / 1000.0) + " 秒）" + tiffHint);
                    exportProgressBar.setVisibility(View.GONE);
                    setStep(2);
                });
            } catch (Throwable e) {
                final String msg = describe(e);
                try { DebugSink.text("export-error", "tier=" + tier + " " + msg); } catch (Throwable ignored) {}
                uploadExportProgress(tmp);
                uploadVkLog();
                runOnUiThread(() -> {
                    statusText.setText("导出失败 " + SpkError.shortForm(msg));
                    showErrorSnackbar("导出失败", SpkError.format(msg)
                            + "\n\n" + sourceWidth + "×" + sourceHeight
                            + "\n" + probe.describe());
                    exportProgressBar.setVisibility(View.GONE);
                    setStep(2);
                });
                final int code = SpkError.codeOf(msg);
                if (code == 1001 || code == 1009) offerSmallerImport();
            } finally {
                if (tmp.exists()) tmp.delete();
            }
        }).start();
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
            if (b.length() > 0) b.append(" <- ");
            b.append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
            if (c.getCause() == c) break;
        }
        return b.toString();
    }

    private static void deleteTree(java.io.File f) {
        if (f == null) return;
        java.io.File[] kids = f.listFiles();
        if (kids != null) for (java.io.File k : kids) deleteTree(k);
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
        if (marker.isFile() && want.equals(readSmallFile(marker))) return dir.getAbsolutePath();
        synchronized (RESOURCE_LOCK) {
            if (marker.isFile() && want.equals(readSmallFile(marker))) return dir.getAbsolutePath();
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
            while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n;
            return new String(b, 0, off, java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Throwable t) {
            return "";
        }
    }

    private void copyAssetTree(String assetPath, java.io.File outDir) throws java.io.IOException {
        String[] files = getAssets().list(assetPath);
        if (files == null || files.length == 0) {
            java.io.File parent = outDir.getParentFile();
            if (parent != null) parent.mkdirs();
            try (InputStream in = getAssets().open(assetPath);
                 java.io.OutputStream out = new java.io.FileOutputStream(outDir)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
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
    protected void onDestroy() {
        // A sheet or snackbar outliving its Activity would keep this instance
        // reachable through a window the system still owns.
        dismissOpenSheet();
        if (currentSnackbar != null) currentSnackbar.dismiss();
        currentSnackbar = null;
        snackbarHost = null;
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

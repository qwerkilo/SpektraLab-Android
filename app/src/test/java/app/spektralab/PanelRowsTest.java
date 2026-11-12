package app.spektralab;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Guards the panel rows that the governance refactor dropped and the theme
 * rebuild's page restore: the Date back corner/style choices, the Optics
 * readout, and the open Edge page surviving a theme switch. The source-level
 * {@code ChoiceRowCoverageCheck} cannot see a missing readout group, and the
 * device matrix catches it only after a build and a phone.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public final class PanelRowsTest {

    @Test
    public void dateBackKeepsCornerAndStyleRows() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "边框");
        click(activity, "日期水印");
        assertShown(activity, "日期角位");
        assertShown(activity, "日期样式");
    }

    @Test
    public void opticsKeepsItsReadout() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "光学");
        assertShown(activity, "放大机曝光");
    }

    @Test
    public void opticsHeaderKeepsMeterAndView() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "光学");
        assertShown(activity, "测光");
        assertShown(activity, "视图");
    }

    @Test
    public void viewSheetKeepsHueAndGamut() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "光学");
        click(activity, "视图");
        final android.app.Dialog dialog =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no view sheet", dialog);
        final View root = dialog.getWindow().getDecorView();
        assertNotNull("no 胶片原色 in the view sheet", find(root, "胶片原色"));
        assertNotNull("no 预览色域 in the view sheet", find(root, "预览色域"));
    }

    @Test
    public void scanSheetKeepsPresetAndCorrections() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "光学");
        click(activity, "扫描仪");
        final android.app.Dialog dialog =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no scan sheet", dialog);
        final View root = dialog.getWindow().getDecorView();
        assertNotNull("no SP-3000 preset in the scan sheet", find(root, "SP-3000"));
        assertNotNull("no white correction in the scan sheet", find(root, "白点校正"));
        assertNotNull("no black correction in the scan sheet", find(root, "黑点校正"));
        assertNotNull("the scan sheet must scroll at large text",
                findByType(root, androidx.core.widget.NestedScrollView.class));
    }

    @Test
    public void scanPresetTurnsBothSwitchesOn() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "光学");
        click(activity, "扫描仪");
        final android.app.Dialog dialog =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no scan sheet", dialog);
        final View root = dialog.getWindow().getDecorView();
        clickTarget(find(root, "SP-3000（模型）"));
        final java.util.List<View> switches = new java.util.ArrayList<>();
        collectByType(root, com.google.android.material.materialswitch.MaterialSwitch.class, switches);
        org.junit.Assert.assertEquals("expected two correction switches", 2, switches.size());
        for (View view : switches) {
            assertTrue("a correction switch stayed off after choosing SP-3000",
                    ((com.google.android.material.materialswitch.MaterialSwitch) view).isChecked());
        }
    }

    @Test
    public void scanPresetRevealsUndoWithAFrameButNoFilm() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        final java.lang.reflect.Field session =
                MainActivity.class.getDeclaredField("currentSession");
        session.setAccessible(true);
        session.set(activity, new FrameLookSession(new Look()));
        click(activity, "光学");
        click(activity, "扫描仪");
        final android.app.Dialog dialog =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no scan sheet", dialog);
        clickTarget(find(dialog.getWindow().getDecorView(), "SP-3000（模型）"));

        final View undo = find(activity, "撤销");
        assertNotNull("no undo action", undo);
        org.junit.Assert.assertEquals(
                "a scan edit with no film must still reveal undo",
                View.VISIBLE, undo.getVisibility());
    }

    @Test
    public void scaleCardsGrowWithTheFont() throws Exception {
        RuntimeEnvironment.setFontScale(2f);
        try {
            final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
            click(activity, "尺度");
            final ViewGroup strip = (ViewGroup) privateView(activity, "formatCardStrip");
            assertTrue("no Scale format cards", strip.getChildCount() > 0);
            final float density = activity.getResources().getDisplayMetrics().density;
            final View card = strip.getChildAt(0);
            org.junit.Assert.assertEquals("2.0x card width",
                    Math.round(116 * density), card.getLayoutParams().width);
            org.junit.Assert.assertEquals("2.0x card height",
                    Math.round(125 * density), card.getLayoutParams().height);
        } finally {
            RuntimeEnvironment.setFontScale(1f);
        }
    }

    @Test
    public void composeSheetScrollsAtLargeText() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "尺度");
        click(activity, "构图 / COMPOSE");
        final android.app.Dialog dialog =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no compose sheet", dialog);
        assertNotNull("the compose sheet must scroll at large text",
                findByType(dialog.getWindow().getDecorView(),
                        androidx.core.widget.NestedScrollView.class));
    }

    @Test
    public void textDialogsUseMaterialInput() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        final java.lang.reflect.Method askText = MainActivity.class.getDeclaredMethod(
                "askText", String.class, String.class, java.util.function.Consumer.class);
        askText.setAccessible(true);
        askText.invoke(activity, "标题", "", (java.util.function.Consumer<String>) value -> { });
        assertMaterialInput("askText");

        final java.lang.reflect.Method askSeed = MainActivity.class.getDeclaredMethod(
                "askSeed", String.class, int.class, java.util.function.IntConsumer.class);
        askSeed.setAccessible(true);
        askSeed.invoke(activity, "种子", 1, (java.util.function.IntConsumer) value -> { });
        assertMaterialInput("askSeed");
    }

    private static void assertMaterialInput(String caller) {
        final android.app.Dialog dialog =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no dialog from " + caller, dialog);
        final View root = dialog.getWindow().getDecorView();
        assertNotNull(caller + " did not use TextInputEditText",
                findByType(root, com.google.android.material.textfield.TextInputEditText.class));
        dialog.dismiss();
    }

    private static View findByType(View root, Class<?> type) {
        if (type.isInstance(root)) {
            return root;
        }
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); ++i) {
                final View found = findByType(group.getChildAt(i), type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collectByType(View root, Class<?> type, java.util.List<View> out) {
        if (type.isInstance(root)) {
            out.add(root);
        }
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); ++i) {
                collectByType(group.getChildAt(i), type, out);
            }
        }
    }

    @Test
    public void filmPanelKeepsThePresetAction() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "胶片");
        assertShown(activity, "预设");
    }

    @Test
    public void presetsListBuiltInLooksFirst() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "预设");
        final android.app.Dialog dialog =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no preset sheet", dialog);
        final View root = dialog.getWindow().getDecorView();
        assertNotNull("no Portra 400 built-in look", find(root, "外观 · Portra 400"));
        assertNotNull("no CineStill 800T built-in look",
                find(root, "外观 · CineStill 800T"));
    }

    @Test
    public void builtInLookOpensAStrengthDraft() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        final java.lang.reflect.Field session =
                MainActivity.class.getDeclaredField("currentSession");
        session.setAccessible(true);
        session.set(activity, new FrameLookSession(new Look()));
        click(activity, "预设");
        final android.app.Dialog menu =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no preset sheet", menu);
        clickTarget(find(menu.getWindow().getDecorView(), "外观 · Portra 400"));
        final android.app.Dialog dialog =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no strength sheet", dialog);
        final View root = dialog.getWindow().getDecorView();
        assertNotNull("no strength percentage", find(root, "100%"));
        assertNotNull("no apply action", find(root, "应用"));
        dialog.dismiss();
    }

    @Test
    public void cancellingStrengthLeavesTheLookAlone() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        final Look base = new Look();
        base.filmStock = "fujifilm_c200";
        final FrameLookSession live = new FrameLookSession(base);
        final java.lang.reflect.Field session =
                MainActivity.class.getDeclaredField("currentSession");
        session.setAccessible(true);
        session.set(activity, live);
        click(activity, "预设");
        final android.app.Dialog menu =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        clickTarget(find(menu.getWindow().getDecorView(), "外观 · Portra 400"));
        final android.app.Dialog sheet =
                org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("no strength sheet", sheet);
        sheet.dismiss();
        org.junit.Assert.assertEquals("a dismissed sheet must not edit the Look",
                "fujifilm_c200", live.look().filmStock);
    }

    @Test
    public void themeSwitchKeepsTheOpenEdgePage() {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        click(activity, "边框");
        click(activity, "日期水印");
        assertShown(activity, "日期角位");
        clickTheme(activity);
        assertShown(activity, "日期角位");
    }

    @Test
    public void histogramSharesTheExposureBlock() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        final View histogram = privateView(activity, "histogram");
        final View exposureValue = privateView(activity, "stageExposureValue");
        final ViewGroup exposure = (ViewGroup) exposureValue.getParent().getParent();
        org.junit.Assert.assertSame(
                "histogram must be laid out under the EV value, not over it",
                exposure, histogram.getParent());
    }

    @Test
    public void prismShellFloatsTheBarAndDocksTheTabs() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        final ViewGroup root = (ViewGroup) privateView(activity, "uiRoot");
        assertTrue("the floating bar must be the last root child",
                root.getChildAt(root.getChildCount() - 1)
                        instanceof com.google.android.material.appbar.AppBarLayout);
        final ViewGroup controls = (ViewGroup) privateView(activity, "controls");
        org.junit.Assert.assertSame("the sheet must sit above the tabs",
                privateView(activity, "panelHolder"), controls.getChildAt(0));
        org.junit.Assert.assertSame("the tab dock must be the last control child",
                privateView(activity, "tabLayout"), controls.getChildAt(1));
    }

    @Test
    public void stagePanelKeepsItsReadoutAndScanLayers() throws Exception {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertNotNull("no file readout", find(activity, "未选择照片"));
        assertNotNull("no print exposure readout", find(activity, "PRINT EXPOSURE"));
        assertNotNull("no scan chip", find(activity, "SCAN · 扫描"));
        assertNotNull("no scan readout card", find(activity, "SCAN INSPECT · CENTRE ROW"));
        assertTrue("the stage must live inside mainCol",
                privateView(activity, "stageView").getParent() != null);
    }

    private static View privateView(MainActivity activity, String field)
            throws Exception {
        final java.lang.reflect.Field declared = MainActivity.class.getDeclaredField(field);
        declared.setAccessible(true);
        final View view = (View) declared.get(activity);
        assertNotNull("no " + field, view);
        return view;
    }

    private static void clickTheme(MainActivity activity) {
        final View dark = find(activity, "DARK");
        final View light = find(activity, "LIGHT");
        final View target = dark != null ? dark : light;
        assertNotNull("no theme button labelled DARK/LIGHT", target);
        clickTarget(target);
    }

    private static void assertShown(MainActivity activity, String label) {
        final View target = find(activity, label);
        assertNotNull("no view labelled " + label, target);
        assertTrue(label + " is not visible", target.isShown());
    }

    private static void click(MainActivity activity, String label) {
        final View target = find(activity, label);
        assertNotNull("no view labelled " + label, target);
        clickTarget(target);
    }

    private static void clickTarget(View target) {
        View clickable = target;
        while (clickable != null && !clickable.isClickable()) {
            clickable = clickable.getParent() instanceof View
                    ? (View) clickable.getParent() : null;
        }
        assertNotNull("no clickable ancestor", clickable);
        clickable.performClick();
    }

    private static View find(MainActivity activity, String label) {
        return find(activity.getWindow().getDecorView(), label);
    }

    private static View find(View root, String label) {
        if (root instanceof TextView) {
            final CharSequence text = ((TextView) root).getText();
            if (text != null && text.toString().contains(label)) {
                return root;
            }
        }
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); ++i) {
                final View found = find(group.getChildAt(i), label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

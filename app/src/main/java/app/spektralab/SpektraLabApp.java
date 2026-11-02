package app.spektralab;

import android.app.Application;

/**
 * Dynamic colour is applied once for every activity. Material 3 falls back to
 * the brand theme on API 24–30; the Console's stage and spectrum colours stay
 * outside the dynamic scheme.
 *
 * <p>Called by reflection so the plain-JVM check compile (which has only
 * android.jar) still builds the app sources. The Gradle build supplies the
 * Material library at runtime.
 */
public final class SpektraLabApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        try {
            final Class<?> colors = Class.forName(
                    "com.google.android.material.color.DynamicColors");
            colors.getMethod("applyToActivitiesIfAvailable", Application.class)
                    .invoke(null, this);
        } catch (Throwable ignored) {
            // Older/plain builds keep the brand palette.
        }
    }
}

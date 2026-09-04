# Facade

A Nova-inspired home screen launcher and rootless device personalization suite for Android.

**Package:** `com.slygames.facade` · **Min SDK:** 26 (Android 8.0) · **Target SDK:** 35

## Modules

- **Nova-Style Launcher Engine** — a custom coordinate-based `ViewGroup` workspace
  (`features/launcher/workspace`) for the paginated desktop grid and drag-and-drop, a
  native `AppWidgetHost` integration (`features/launcher/widget`), and a Compose-driven
  app drawer (`features/appdrawer`), reached via a swipe up from the workspace.
- **Live Wallpaper Engine** — `services/wallpaper/FacadeWallpaperService`, a
  `WallpaperService.Engine` binding a looping AndroidX Media3 `ExoPlayer` directly to the
  wallpaper surface, paused whenever the surface is hidden.
- **Surface Overlay Engine** — `services/overlay/FacadeAccessibilityService`, an
  `AccessibilityService` used only to host `TYPE_ACCESSIBILITY_OVERLAY` windows (floating
  HUDs, a custom status bar, an interceptable volume control).
- **Elevated System Tweak Bridge** — `services/shizuku/ShizukuManager`, a Shizuku client
  wrapper for rootless `WRITE_SECURE_SETTINGS`-gated tweaks (animation scales, UI Tuner
  toggles) with a documented stub for binding a Shizuku user service.

## Architecture

Clean Architecture + MVI/MVVM, Kotlin Coroutines & `StateFlow`, Hilt for DI, Room for the
workspace layout database, and Jetpack DataStore for launcher preferences. UI is a
hybrid split: a plain `ViewGroup` hierarchy for the desktop grid (60fps drag-and-drop,
zero-jank nested `AppWidgetHostView`s) and Jetpack Compose + Material 3 everywhere else
(app drawer, settings, wallpaper picker, overlay/system-tweak screens).

See `app/src/main/java/com/slygames/facade/` for the full package layout.

## Building

```
./gradlew assembleDebug
```

Requires a configured Android SDK (`local.properties` with `sdk.dir=...`, or the
`ANDROID_HOME` environment variable) and network access to Google's Maven repository for
the Android Gradle Plugin and AndroidX artifacts.

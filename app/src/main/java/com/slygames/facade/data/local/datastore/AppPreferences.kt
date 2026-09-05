package com.slygames.facade.data.local.datastore

/** Which built-in look the status bar's battery glyph is drawn in - see `BatteryIconStyle` in
 * `services.overlay.OverlayContent` (kept as a String key here so DataStore doesn't need a
 * custom serializer; the overlay code owns the actual enum). */
enum class BatteryIconStyle { CLASSIC, MINIMAL, BOLD }

/**
 * Snapshot of every user-configurable, non-wallpaper-specific setting
 * (wallpaper's own knobs live in [WallpaperPreferences]), persisted via
 * [AppPreferencesRepository] on top of Jetpack DataStore.
 */
data class AppPreferences(
    val dynamicColorEnabled: Boolean = true,
    val overlayStatusBarEnabled: Boolean = false,
    val overlayVolumeHudEnabled: Boolean = false,

    // Status bar content - each element is independently toggleable so the bar can be as
    // minimal or as busy as the user wants.
    val statusBarShowClock: Boolean = true,
    val statusBarShowBattery: Boolean = true,
    val statusBarShowWifi: Boolean = true,
    val statusBarUse24HourClock: Boolean = false,
    val statusBarBatteryStyle: BatteryIconStyle = BatteryIconStyle.CLASSIC,

    // A custom accent used for overlay icons/text; null means "follow the app's own theme"
    // (Material You or the static fallback palette) instead of a fixed color the user picked.
    val overlayAccentColorArgb: Int? = null
)

package com.slygames.facade.data.local.datastore

/**
 * Snapshot of every user-configurable, non-wallpaper-specific setting
 * (wallpaper's own knobs live in [WallpaperPreferences]), persisted via
 * [AppPreferencesRepository] on top of Jetpack DataStore.
 */
data class AppPreferences(
    val dynamicColorEnabled: Boolean = true,
    val overlayStatusBarEnabled: Boolean = false,
    val overlayVolumeHudEnabled: Boolean = false,
    val overlayFloatingHudEnabled: Boolean = false
)

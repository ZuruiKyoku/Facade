package com.slygames.facade.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.slygames.facade.di.AppDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository over Jetpack DataStore Preferences holding the app's
 * non-wallpaper settings: Material You theming, which surface overlays
 * are enabled, and how they're customized. Backed by a single
 * `.preferences_pb` file injected as `DataStore<Preferences>` (see
 * `di.DataStoreModule`).
 */
@Singleton
class AppPreferencesRepository @Inject constructor(
    @AppDataStore private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val OVERLAY_STATUS_BAR_ENABLED = booleanPreferencesKey("overlay_status_bar_enabled")
        val OVERLAY_VOLUME_HUD_ENABLED = booleanPreferencesKey("overlay_volume_hud_enabled")
        val OVERLAY_FLOATING_HUD_ENABLED = booleanPreferencesKey("overlay_floating_hud_enabled")
        val STATUS_BAR_SHOW_CLOCK = booleanPreferencesKey("status_bar_show_clock")
        val STATUS_BAR_SHOW_BATTERY = booleanPreferencesKey("status_bar_show_battery")
        val STATUS_BAR_SHOW_WIFI = booleanPreferencesKey("status_bar_show_wifi")
        val STATUS_BAR_USE_24H = booleanPreferencesKey("status_bar_use_24h")
        val STATUS_BAR_BATTERY_STYLE = stringPreferencesKey("status_bar_battery_style")
        val FLOATING_HUD_CORNER = stringPreferencesKey("floating_hud_corner")
        val FLOATING_HUD_LABEL = stringPreferencesKey("floating_hud_label")
        val OVERLAY_ACCENT_COLOR = intPreferencesKey("overlay_accent_color_argb")
    }

    val preferencesFlow: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs -> prefs.toAppPreferences() }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setOverlayStatusBarEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.OVERLAY_STATUS_BAR_ENABLED] = enabled }
    }

    suspend fun setOverlayVolumeHudEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.OVERLAY_VOLUME_HUD_ENABLED] = enabled }
    }

    suspend fun setOverlayFloatingHudEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.OVERLAY_FLOATING_HUD_ENABLED] = enabled }
    }

    suspend fun setStatusBarShowClock(show: Boolean) {
        dataStore.edit { it[Keys.STATUS_BAR_SHOW_CLOCK] = show }
    }

    suspend fun setStatusBarShowBattery(show: Boolean) {
        dataStore.edit { it[Keys.STATUS_BAR_SHOW_BATTERY] = show }
    }

    suspend fun setStatusBarShowWifi(show: Boolean) {
        dataStore.edit { it[Keys.STATUS_BAR_SHOW_WIFI] = show }
    }

    suspend fun setStatusBarUse24HourClock(use24h: Boolean) {
        dataStore.edit { it[Keys.STATUS_BAR_USE_24H] = use24h }
    }

    suspend fun setStatusBarBatteryStyle(style: BatteryIconStyle) {
        dataStore.edit { it[Keys.STATUS_BAR_BATTERY_STYLE] = style.name }
    }

    suspend fun setFloatingHudCorner(corner: HudCorner) {
        dataStore.edit { it[Keys.FLOATING_HUD_CORNER] = corner.name }
    }

    suspend fun setFloatingHudLabel(label: String) {
        dataStore.edit { it[Keys.FLOATING_HUD_LABEL] = label.take(MAX_HUD_LABEL_LENGTH) }
    }

    /** Pass null to clear the override and fall back to the app's own theme. */
    suspend fun setOverlayAccentColor(argb: Int?) {
        dataStore.edit { prefs ->
            if (argb == null) prefs.remove(Keys.OVERLAY_ACCENT_COLOR) else prefs[Keys.OVERLAY_ACCENT_COLOR] = argb
        }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toAppPreferences(): AppPreferences {
        val defaults = AppPreferences()
        return AppPreferences(
            dynamicColorEnabled = this[Keys.DYNAMIC_COLOR_ENABLED] ?: defaults.dynamicColorEnabled,
            overlayStatusBarEnabled = this[Keys.OVERLAY_STATUS_BAR_ENABLED] ?: defaults.overlayStatusBarEnabled,
            overlayVolumeHudEnabled = this[Keys.OVERLAY_VOLUME_HUD_ENABLED] ?: defaults.overlayVolumeHudEnabled,
            overlayFloatingHudEnabled = this[Keys.OVERLAY_FLOATING_HUD_ENABLED] ?: defaults.overlayFloatingHudEnabled,
            statusBarShowClock = this[Keys.STATUS_BAR_SHOW_CLOCK] ?: defaults.statusBarShowClock,
            statusBarShowBattery = this[Keys.STATUS_BAR_SHOW_BATTERY] ?: defaults.statusBarShowBattery,
            statusBarShowWifi = this[Keys.STATUS_BAR_SHOW_WIFI] ?: defaults.statusBarShowWifi,
            statusBarUse24HourClock = this[Keys.STATUS_BAR_USE_24H] ?: defaults.statusBarUse24HourClock,
            statusBarBatteryStyle = this[Keys.STATUS_BAR_BATTERY_STYLE]?.let { raw ->
                runCatching { BatteryIconStyle.valueOf(raw) }.getOrNull()
            } ?: defaults.statusBarBatteryStyle,
            floatingHudCorner = this[Keys.FLOATING_HUD_CORNER]?.let { raw ->
                runCatching { HudCorner.valueOf(raw) }.getOrNull()
            } ?: defaults.floatingHudCorner,
            floatingHudLabel = this[Keys.FLOATING_HUD_LABEL] ?: defaults.floatingHudLabel,
            overlayAccentColorArgb = this[Keys.OVERLAY_ACCENT_COLOR]
        )
    }

    private companion object {
        const val MAX_HUD_LABEL_LENGTH = 16
    }
}

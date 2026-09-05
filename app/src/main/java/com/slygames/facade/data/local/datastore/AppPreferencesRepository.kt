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
        val STATUS_BAR_SHOW_CLOCK = booleanPreferencesKey("status_bar_show_clock")
        val STATUS_BAR_SHOW_BATTERY = booleanPreferencesKey("status_bar_show_battery")
        val STATUS_BAR_SHOW_WIFI = booleanPreferencesKey("status_bar_show_wifi")
        val STATUS_BAR_USE_24H = booleanPreferencesKey("status_bar_use_24h")
        val STATUS_BAR_BATTERY_STYLE = stringPreferencesKey("status_bar_battery_style")
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
            statusBarShowClock = this[Keys.STATUS_BAR_SHOW_CLOCK] ?: defaults.statusBarShowClock,
            statusBarShowBattery = this[Keys.STATUS_BAR_SHOW_BATTERY] ?: defaults.statusBarShowBattery,
            statusBarShowWifi = this[Keys.STATUS_BAR_SHOW_WIFI] ?: defaults.statusBarShowWifi,
            statusBarUse24HourClock = this[Keys.STATUS_BAR_USE_24H] ?: defaults.statusBarUse24HourClock,
            statusBarBatteryStyle = this[Keys.STATUS_BAR_BATTERY_STYLE]?.let { raw ->
                runCatching { BatteryIconStyle.valueOf(raw) }.getOrNull()
            } ?: defaults.statusBarBatteryStyle,
            overlayAccentColorArgb = this[Keys.OVERLAY_ACCENT_COLOR]
        )
    }
}

package com.slygames.facade.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.slygames.facade.di.AppDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository over Jetpack DataStore Preferences holding the app's
 * non-wallpaper settings: Material You theming and which surface overlays
 * are enabled. Backed by a single `.preferences_pb` file injected as
 * `DataStore<Preferences>` (see `di.DataStoreModule`).
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

    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toAppPreferences(): AppPreferences {
        val defaults = AppPreferences()
        return AppPreferences(
            dynamicColorEnabled = this[Keys.DYNAMIC_COLOR_ENABLED] ?: defaults.dynamicColorEnabled,
            overlayStatusBarEnabled = this[Keys.OVERLAY_STATUS_BAR_ENABLED] ?: defaults.overlayStatusBarEnabled,
            overlayVolumeHudEnabled = this[Keys.OVERLAY_VOLUME_HUD_ENABLED] ?: defaults.overlayVolumeHudEnabled,
            overlayFloatingHudEnabled = this[Keys.OVERLAY_FLOATING_HUD_ENABLED] ?: defaults.overlayFloatingHudEnabled
        )
    }
}

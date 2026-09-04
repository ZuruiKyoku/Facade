package com.slygames.facade.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.slygames.facade.di.LauncherDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository over Jetpack DataStore Preferences holding every launcher
 * configuration knob: grid size, icon scale, active icon pack, and gesture
 * mappings. Backed by a single `.preferences_pb` file injected as
 * `DataStore<Preferences>` (see `di.DataStoreModule`).
 */
@Singleton
class LauncherPreferencesRepository @Inject constructor(
    @LauncherDataStore private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val GRID_ROWS = intPreferencesKey("grid_rows")
        val DOCK_SLOT_COUNT = intPreferencesKey("dock_slot_count")
        val ICON_SCALE = floatPreferencesKey("icon_scale")
        val SHOW_ICON_LABELS = booleanPreferencesKey("show_icon_labels")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val INFINITE_SCROLL_ENABLED = booleanPreferencesKey("infinite_scroll_enabled")
        val ACTIVE_ICON_PACK = stringPreferencesKey("active_icon_pack_package")
        val GESTURE_MAPPINGS_JSON = stringPreferencesKey("gesture_mappings_json")
        val OVERLAY_STATUS_BAR_ENABLED = booleanPreferencesKey("overlay_status_bar_enabled")
        val OVERLAY_VOLUME_HUD_ENABLED = booleanPreferencesKey("overlay_volume_hud_enabled")
        val OVERLAY_FLOATING_HUD_ENABLED = booleanPreferencesKey("overlay_floating_hud_enabled")

        // Retained for forward-compat / migration reference; unused directly.
        @Suppress("unused")
        val LEGACY_HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
    }

    val preferencesFlow: Flow<LauncherPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw exception
        }
        .map { prefs -> prefs.toLauncherPreferences() }

    suspend fun setGridSize(columns: Int, rows: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GRID_COLUMNS] = columns.coerceIn(2, 10)
            prefs[Keys.GRID_ROWS] = rows.coerceIn(2, 12)
        }
    }

    suspend fun setDockSlotCount(count: Int) {
        dataStore.edit { it[Keys.DOCK_SLOT_COUNT] = count.coerceIn(3, 7) }
    }

    suspend fun setIconScale(scale: Float) {
        dataStore.edit { it[Keys.ICON_SCALE] = scale.coerceIn(0.5f, 1.5f) }
    }

    suspend fun setShowIconLabels(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_ICON_LABELS] = show }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setInfiniteScrollEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.INFINITE_SCROLL_ENABLED] = enabled }
    }

    suspend fun setActiveIconPack(packageName: String?) {
        dataStore.edit { prefs ->
            if (packageName == null) prefs.remove(Keys.ACTIVE_ICON_PACK)
            else prefs[Keys.ACTIVE_ICON_PACK] = packageName
        }
    }

    suspend fun setGestureMapping(trigger: GestureTrigger, action: GestureAction, targetComponentKey: String? = null) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.GESTURE_MAPPINGS_JSON]?.let { decodeGestureMappings(it) }
                ?: LauncherPreferences.defaultGestureMappings()
            val updated = current
                .filterNot { it.trigger == trigger }
                .plus(GestureMapping(trigger, action, targetComponentKey))
            prefs[Keys.GESTURE_MAPPINGS_JSON] = encodeGestureMappings(updated)
        }
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

    private fun Preferences.toLauncherPreferences(): LauncherPreferences {
        val defaults = LauncherPreferences()
        return LauncherPreferences(
            gridColumns = this[Keys.GRID_COLUMNS] ?: defaults.gridColumns,
            gridRows = this[Keys.GRID_ROWS] ?: defaults.gridRows,
            dockSlotCount = this[Keys.DOCK_SLOT_COUNT] ?: defaults.dockSlotCount,
            iconScale = this[Keys.ICON_SCALE] ?: defaults.iconScale,
            showIconLabels = this[Keys.SHOW_ICON_LABELS] ?: defaults.showIconLabels,
            dynamicColorEnabled = this[Keys.DYNAMIC_COLOR_ENABLED] ?: defaults.dynamicColorEnabled,
            infiniteScrollEnabled = this[Keys.INFINITE_SCROLL_ENABLED] ?: defaults.infiniteScrollEnabled,
            activeIconPackPackage = this[Keys.ACTIVE_ICON_PACK],
            gestureMappings = this[Keys.GESTURE_MAPPINGS_JSON]?.let { decodeGestureMappings(it) }
                ?: defaults.gestureMappings,
            overlayStatusBarEnabled = this[Keys.OVERLAY_STATUS_BAR_ENABLED] ?: defaults.overlayStatusBarEnabled,
            overlayVolumeHudEnabled = this[Keys.OVERLAY_VOLUME_HUD_ENABLED] ?: defaults.overlayVolumeHudEnabled,
            overlayFloatingHudEnabled = this[Keys.OVERLAY_FLOATING_HUD_ENABLED] ?: defaults.overlayFloatingHudEnabled
        )
    }

    private fun encodeGestureMappings(mappings: List<GestureMapping>): String {
        val array = JSONArray()
        mappings.forEach { mapping ->
            array.put(
                JSONObject().apply {
                    put("trigger", mapping.trigger.name)
                    put("action", mapping.action.name)
                    put("target", mapping.targetComponentKey ?: JSONObject.NULL)
                }
            )
        }
        return array.toString()
    }

    private fun decodeGestureMappings(json: String): List<GestureMapping> = try {
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    GestureMapping(
                        trigger = GestureTrigger.valueOf(obj.getString("trigger")),
                        action = GestureAction.valueOf(obj.getString("action")),
                        targetComponentKey = obj.optString("target").takeIf { it.isNotEmpty() && it != "null" }
                    )
                )
            }
        }
    } catch (_: Exception) {
        LauncherPreferences.defaultGestureMappings()
    }
}

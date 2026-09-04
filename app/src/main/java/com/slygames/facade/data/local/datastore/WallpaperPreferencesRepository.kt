package com.slygames.facade.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.slygames.facade.di.WallpaperDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class WallpaperPreferences(
    /** content:// URI (persisted via [android.content.ContentResolver.takePersistableUriPermission]) of the chosen looping video/canvas source. */
    val selectedMediaUri: String? = null,
    val muted: Boolean = true,
    val loop: Boolean = true
)

/**
 * Persists the user's chosen live-wallpaper media source for
 * [com.slygames.facade.services.wallpaper.FacadeWallpaperService] to read on
 * every `Engine` (re)creation - the Engine itself never needs write access.
 */
@Singleton
class WallpaperPreferencesRepository @Inject constructor(
    @WallpaperDataStore private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val SELECTED_MEDIA_URI = stringPreferencesKey("selected_media_uri")
        val MUTED = booleanPreferencesKey("muted")
        val LOOP = booleanPreferencesKey("loop")
    }

    val preferencesFlow: Flow<WallpaperPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            WallpaperPreferences(
                selectedMediaUri = prefs[Keys.SELECTED_MEDIA_URI],
                muted = prefs[Keys.MUTED] ?: true,
                loop = prefs[Keys.LOOP] ?: true
            )
        }

    suspend fun setSelectedMedia(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.SELECTED_MEDIA_URI) else prefs[Keys.SELECTED_MEDIA_URI] = uri
        }
    }

    suspend fun setMuted(muted: Boolean) {
        dataStore.edit { it[Keys.MUTED] = muted }
    }

    suspend fun setLoop(loop: Boolean) {
        dataStore.edit { it[Keys.LOOP] = loop }
    }
}

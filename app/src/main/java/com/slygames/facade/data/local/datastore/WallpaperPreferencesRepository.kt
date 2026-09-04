package com.slygames.facade.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.slygames.facade.di.WallpaperDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class WallpaperPreferences(
    /** content:// URIs (persisted via [android.content.ContentResolver.takePersistableUriPermission] where the source supports it) of the chosen looping video pool. One entry plays on repeat; two or more shuffle per [shuffleIntervalMinutes]. */
    val selectedMediaUris: Set<String> = emptySet(),
    val muted: Boolean = true,
    val loop: Boolean = true,
    /** How often to switch to a different (random) entry in [selectedMediaUris]. 0 = never - the same video just loops. Meaningless with fewer than two selected. */
    val shuffleIntervalMinutes: Int = 0
)

/**
 * Persists the user's chosen live-wallpaper media pool for
 * [com.slygames.facade.services.wallpaper.FacadeWallpaperService] to read on
 * every `Engine` (re)creation - the Engine itself never needs write access.
 */
@Singleton
class WallpaperPreferencesRepository @Inject constructor(
    @WallpaperDataStore private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val SELECTED_MEDIA_URIS = stringSetPreferencesKey("selected_media_uris")
        val MUTED = booleanPreferencesKey("muted")
        val LOOP = booleanPreferencesKey("loop")
        val SHUFFLE_INTERVAL_MINUTES = intPreferencesKey("shuffle_interval_minutes")
    }

    val preferencesFlow: Flow<WallpaperPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            WallpaperPreferences(
                selectedMediaUris = prefs[Keys.SELECTED_MEDIA_URIS] ?: emptySet(),
                muted = prefs[Keys.MUTED] ?: true,
                loop = prefs[Keys.LOOP] ?: true,
                shuffleIntervalMinutes = prefs[Keys.SHUFFLE_INTERVAL_MINUTES] ?: 0
            )
        }

    /** Adds [uris] to the pool (read-modify-write under DataStore's own atomic edit, so concurrent add/remove calls can't clobber each other). */
    suspend fun addSelectedMediaUris(uris: Set<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_MEDIA_URIS] = (prefs[Keys.SELECTED_MEDIA_URIS] ?: emptySet()) + uris
        }
    }

    suspend fun removeSelectedMediaUri(uri: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_MEDIA_URIS] = (prefs[Keys.SELECTED_MEDIA_URIS] ?: emptySet()) - uri
        }
    }

    suspend fun setMuted(muted: Boolean) {
        dataStore.edit { it[Keys.MUTED] = muted }
    }

    suspend fun setLoop(loop: Boolean) {
        dataStore.edit { it[Keys.LOOP] = loop }
    }

    suspend fun setShuffleIntervalMinutes(minutes: Int) {
        dataStore.edit { it[Keys.SHUFFLE_INTERVAL_MINUTES] = minutes }
    }
}

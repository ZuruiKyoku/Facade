package com.slygames.facade.features.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.data.local.datastore.WallpaperPreferences
import com.slygames.facade.data.local.datastore.WallpaperPreferencesRepository
import com.slygames.facade.di.IoDispatcher
import com.slygames.facade.services.wallpaper.FacadeWallpaperService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WallpaperPickerUiState(
    val preferences: WallpaperPreferences = WallpaperPreferences(),
    /** Keyed by URI string; a present-but-null value means the thumbnail failed to decode (shown as a placeholder), absent means still loading. */
    val thumbnails: Map<String, Bitmap?> = emptyMap()
)

@HiltViewModel
class WallpaperPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: WallpaperPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())

    val uiState: StateFlow<WallpaperPickerUiState> = combine(
        preferencesRepository.preferencesFlow,
        _thumbnails
    ) { prefs, thumbnails ->
        WallpaperPickerUiState(preferences = prefs, thumbnails = thumbnails)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WallpaperPickerUiState())

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                loadMissingThumbnails(prefs.selectedMediaUris)
            }
        }
    }

    /** Adds newly-picked videos to the pool, taking a persistable read grant on each where the source supports it (some picker-returned URIs don't - the wallpaper service falls back gracefully if a grant didn't survive to the next read). */
    fun addSelectedVideos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // Not all providers support persistable grants; playback still works for the
                    // current process, it just may need to be re-picked after a reboot.
                }
            }
            preferencesRepository.addSelectedMediaUris(uris.map { it.toString() }.toSet())
        }
    }

    fun removeSelectedVideo(uriString: String) {
        viewModelScope.launch {
            preferencesRepository.removeSelectedMediaUri(uriString)
            _thumbnails.value = _thumbnails.value - uriString
        }
    }

    fun setMuted(muted: Boolean) = viewModelScope.launch { preferencesRepository.setMuted(muted) }
    fun setLoop(loop: Boolean) = viewModelScope.launch { preferencesRepository.setLoop(loop) }
    fun setShuffleIntervalMinutes(minutes: Int) = viewModelScope.launch { preferencesRepository.setShuffleIntervalMinutes(minutes) }

    /** Launches the platform's live-wallpaper preview/activation flow targeting [FacadeWallpaperService]. */
    fun buildActivateWallpaperIntent(): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, FacadeWallpaperService::class.java)
            )
        }

    private suspend fun loadMissingThumbnails(uris: Set<String>) {
        val missing = uris.filterNot { _thumbnails.value.containsKey(it) }
        if (missing.isEmpty()) return
        val loaded = withContext(ioDispatcher) {
            missing.associateWith { uriString -> loadVideoThumbnail(uriString) }
        }
        _thumbnails.value = _thumbnails.value + loaded
    }

    /** A frame from the start of the video, decoded directly from its content URI - works for any source (Photo Picker, document tree, etc.) without needing a local file path. */
    private fun loadVideoThumbnail(uriString: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uriString))
            retriever.getFrameAtTime(0)
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}

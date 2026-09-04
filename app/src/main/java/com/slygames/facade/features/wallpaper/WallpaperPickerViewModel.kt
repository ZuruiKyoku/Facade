package com.slygames.facade.features.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WallpaperMediaEntry(val uri: Uri, val displayName: String, val durationMs: Long)

data class WallpaperPickerUiState(
    val availableVideos: List<WallpaperMediaEntry> = emptyList(),
    val preferences: WallpaperPreferences = WallpaperPreferences(),
    val isLoading: Boolean = false
)

@HiltViewModel
class WallpaperPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: WallpaperPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _videos = MutableStateFlow<List<WallpaperMediaEntry>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<WallpaperPickerUiState> = combine(
        _videos,
        preferencesRepository.preferencesFlow,
        _isLoading
    ) { videos, prefs, loading ->
        WallpaperPickerUiState(availableVideos = videos, preferences = prefs, isLoading = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WallpaperPickerUiState())

    init {
        loadLocalVideos()
    }

    fun loadLocalVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _videos.value = withContext(ioDispatcher) { queryLocalVideos() }
            _isLoading.value = false
        }
    }

    fun selectMedia(uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers (e.g. non-tree DocumentsProvider results) don't support persistable grants;
                // FacadeWallpaperService falls back to a fresh pick if this URI can't be opened later.
            }
            preferencesRepository.setSelectedMedia(uri.toString())
        }
    }

    fun setMuted(muted: Boolean) = viewModelScope.launch { preferencesRepository.setMuted(muted) }
    fun setLoop(loop: Boolean) = viewModelScope.launch { preferencesRepository.setLoop(loop) }

    /** Launches the platform's live-wallpaper preview/activation flow targeting [FacadeWallpaperService]. */
    fun buildActivateWallpaperIntent(): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, FacadeWallpaperService::class.java)
            )
        }

    private fun queryLocalVideos(): List<WallpaperMediaEntry> {
        val results = mutableListOf<WallpaperMediaEntry>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                results += WallpaperMediaEntry(
                    uri = uri,
                    displayName = cursor.getString(nameCol) ?: uri.lastPathSegment.orEmpty(),
                    durationMs = cursor.getLong(durationCol)
                )
            }
        }
        return results
    }
}

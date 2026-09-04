package com.slygames.facade.services.wallpaper

import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.slygames.facade.data.local.datastore.WallpaperPreferences
import com.slygames.facade.data.local.datastore.WallpaperPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Live Wallpaper Engine: a [WallpaperService] whose [Engine] binds a single
 * looping AndroxX Media3 [ExoPlayer] instance directly to the wallpaper's
 * [SurfaceHolder]. Playback is paused (not just muted) whenever the surface
 * isn't visible - [Engine.onVisibilityChanged] fires when the user opens an
 * app over the home screen, locks the device, or swipes to the app drawer -
 * so a hidden wallpaper never burns battery decoding frames nobody sees.
 */
@AndroidEntryPoint
class FacadeWallpaperService : WallpaperService() {

    @Inject lateinit var preferencesRepository: WallpaperPreferencesRepository

    override fun onCreateEngine(): Engine = FacadeEngine()

    private inner class FacadeEngine : Engine() {

        private var exoPlayer: ExoPlayer? = null
        private var engineScope: CoroutineScope? = null
        private var preferencesJob: Job? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            ensurePlayerInitialized()
            exoPlayer?.setVideoSurfaceHolder(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            exoPlayer?.setVideoSurfaceHolder(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                ensurePlayerInitialized()
                exoPlayer?.play()
            } else {
                exoPlayer?.pause()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            releasePlayer()
        }

        private fun ensurePlayerInitialized() {
            if (exoPlayer != null) return
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            engineScope = scope
            val player = ExoPlayer.Builder(this@FacadeWallpaperService).build()
            exoPlayer = player
            preferencesJob = scope.launch {
                preferencesRepository.preferencesFlow.collect { prefs ->
                    applyPreferences(player, prefs)
                }
            }
        }

        private fun applyPreferences(player: ExoPlayer, prefs: WallpaperPreferences) {
            val uriString = prefs.selectedMediaUri
            if (uriString == null) {
                player.stop()
                return
            }
            val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
            if (player.currentMediaItem?.mediaId != mediaItem.mediaId) {
                try {
                    player.setMediaItem(mediaItem)
                    player.prepare()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load wallpaper media $uriString", e)
                    return
                }
            }
            player.volume = if (prefs.muted) 0f else 1f
            player.repeatMode = if (prefs.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            if (isVisible) player.play()
        }

        private fun releasePlayer() {
            preferencesJob?.cancel()
            engineScope?.cancel()
            engineScope = null
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    private companion object {
        const val TAG = "FacadeWallpaperService"
    }
}

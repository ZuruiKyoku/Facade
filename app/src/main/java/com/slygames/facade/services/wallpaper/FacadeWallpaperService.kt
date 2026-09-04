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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Live Wallpaper Engine: a [WallpaperService] whose [Engine] binds a single
 * looping AndroxX Media3 [ExoPlayer] instance directly to the wallpaper's
 * [SurfaceHolder]. Playback is paused (not just muted) whenever the surface
 * isn't visible - [Engine.onVisibilityChanged] fires when the user opens an
 * app over the home screen, locks the device, or swipes to the app drawer -
 * so a hidden wallpaper never burns battery decoding frames nobody sees.
 *
 * When the user has selected more than one video, a second timer loop
 * ([restartShuffleLoop]) switches to a different random entry from the pool
 * every [WallpaperPreferences.shuffleIntervalMinutes]; with zero or one
 * selected it's a no-op and the engine behaves exactly as a single-video
 * looping wallpaper always has.
 */
@AndroidEntryPoint
class FacadeWallpaperService : WallpaperService() {

    @Inject lateinit var preferencesRepository: WallpaperPreferencesRepository

    override fun onCreateEngine(): Engine = FacadeEngine()

    private inner class FacadeEngine : Engine() {

        private var exoPlayer: ExoPlayer? = null
        private var engineScope: CoroutineScope? = null
        private var preferencesJob: Job? = null
        private var shuffleJob: Job? = null

        /** The pool/interval combination [shuffleJob] is currently looping over, so unrelated preference changes (mute, loop) don't restart - and so lose the countdown on - the timer. */
        private var runningShuffleKey: Pair<Set<String>, Int>? = null
        private var currentUriString: String? = null

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
            val uris = prefs.selectedMediaUris
            if (uris.isEmpty()) {
                player.stop()
                currentUriString = null
            } else if (currentUriString == null || currentUriString !in uris) {
                // First load, or the playing video was removed from the pool - (re)start from a
                // random member rather than always the same one so a shuffling pool doesn't
                // predictably reopen on the same video every time the wallpaper reattaches.
                playUri(player, uris.random())
            }
            player.volume = if (prefs.muted) 0f else 1f
            player.repeatMode = if (prefs.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            if (isVisible) player.play()
            restartShuffleLoop(player, prefs)
        }

        private fun playUri(player: ExoPlayer, uriString: String) {
            currentUriString = uriString
            try {
                player.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
                player.prepare()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load wallpaper media $uriString", e)
            }
        }

        private fun restartShuffleLoop(player: ExoPlayer, prefs: WallpaperPreferences) {
            val key = prefs.selectedMediaUris to prefs.shuffleIntervalMinutes
            if (key == runningShuffleKey) return
            runningShuffleKey = key
            shuffleJob?.cancel()

            val uris = prefs.selectedMediaUris
            if (uris.size < 2 || prefs.shuffleIntervalMinutes <= 0) return
            val scope = engineScope ?: return
            shuffleJob = scope.launch {
                while (isActive) {
                    delay(prefs.shuffleIntervalMinutes * MINUTE_MS)
                    // Pick uniformly among the OTHER entries so it never re-picks what's already
                    // playing; falls back to the single remaining one if the pool shrank mid-wait.
                    val next = uris.filterNot { it == currentUriString }.randomOrNull() ?: uris.first()
                    playUri(player, next)
                    player.volume = if (prefs.muted) 0f else 1f
                    player.repeatMode = if (prefs.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    if (isVisible) player.play()
                }
            }
        }

        private fun releasePlayer() {
            shuffleJob?.cancel()
            preferencesJob?.cancel()
            engineScope?.cancel()
            engineScope = null
            runningShuffleKey = null
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    private companion object {
        const val TAG = "FacadeWallpaperService"
        const val MINUTE_MS = 60_000L
    }
}

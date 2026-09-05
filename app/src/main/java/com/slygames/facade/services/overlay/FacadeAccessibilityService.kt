package com.slygames.facade.services.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.graphics.Color
import com.slygames.facade.data.local.datastore.AppPreferences
import com.slygames.facade.data.local.datastore.AppPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Surface Overlay Engine: an [AccessibilityService] used purely as a host
 * for [android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY]
 * windows - floating HUDs, a custom status bar, and an interceptable volume
 * control - not for reading other apps' window content (`canRetrieveWindowContent="false"`
 * in `res/xml/accessibility_service_config.xml`).
 */
@AndroidEntryPoint
class FacadeAccessibilityService : AccessibilityService() {

    @Inject lateinit var preferencesRepository: AppPreferencesRepository

    private lateinit var overlayController: OverlayWindowController
    private lateinit var audioManager: AudioManager
    private lateinit var statusBarInfoProvider: StatusBarInfoProvider
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var preferencesJob: Job? = null
    private var statusBarJob: Job? = null
    private var volumeHudHideJob: Job? = null

    @Volatile private var volumeHudEnabled = false
    @Volatile private var volumeHudAccent: Color? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        // TYPE_ACCESSIBILITY_OVERLAY windows must be added through the AccessibilityService's
        // own Context, not the plain applicationContext - only the service instance carries
        // the special window token that type needs; applicationContext's WindowManager throws
        // WindowManager.BadTokenException ("token null is not valid") on addView.
        overlayController = OverlayWindowController(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        statusBarInfoProvider = StatusBarInfoProvider(applicationContext)
        // Belt-and-suspenders: android:accessibilityFlags="flagRequestFilterKeyEvents" in
        // accessibility_service_config.xml should be enough on its own, but some OEM builds
        // only honor the flag reliably when it's also set on the live AccessibilityServiceInfo
        // after connecting.
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        observePreferences()
    }

    private fun observePreferences() {
        preferencesJob?.cancel()
        preferencesJob = serviceScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                val accent = prefs.overlayAccentColorArgb?.let { Color(it) }
                volumeHudAccent = accent
                volumeHudEnabled = prefs.overlayVolumeHudEnabled

                // Unlike the other two surfaces, the volume HUD isn't shown just because its
                // toggle is on - it only appears transiently while adjusting volume (see
                // showVolumeHud). Turning the toggle off should still hide it immediately if it
                // happens to be up, and cancel its pending auto-hide.
                if (!prefs.overlayVolumeHudEnabled) {
                    volumeHudHideJob?.cancel()
                    overlayController.hide(OverlaySurface.VOLUME_HUD)
                }

                if (prefs.overlayFloatingHudEnabled) {
                    overlayController.show(OverlaySurface.FLOATING_HUD, corner = prefs.floatingHudCorner) {
                        FloatingHudOverlayContent(label = prefs.floatingHudLabel, accentColor = accent)
                    }
                } else {
                    overlayController.hide(OverlaySurface.FLOATING_HUD)
                }

                updateStatusBar(prefs, accent)
            }
        }
    }

    /** The status bar needs to keep redrawing on its own (a ticking clock, live battery/Wi-Fi
     * push updates) independent of preference changes, so it gets its own child coroutine that's
     * restarted - cheaply, given how rarely preferences actually change - on every preferences
     * emission rather than trying to thread every relevant field through as separate
     * distinctUntilChanged slices. */
    private fun updateStatusBar(prefs: AppPreferences, accent: Color?) {
        statusBarJob?.cancel()
        if (!prefs.overlayStatusBarEnabled) {
            overlayController.hide(OverlaySurface.STATUS_BAR)
            return
        }
        statusBarJob = serviceScope.launch {
            combine(clockTicker(), statusBarInfoProvider.observe()) { _, info -> info }
                .collect { info ->
                    overlayController.show(OverlaySurface.STATUS_BAR) {
                        StatusBarOverlayContent(
                            clockText = if (prefs.statusBarShowClock) formatClock(prefs.statusBarUse24HourClock) else null,
                            batteryPercent = if (prefs.statusBarShowBattery) info.batteryPercent else null,
                            isCharging = info.isCharging,
                            batteryStyle = prefs.statusBarBatteryStyle,
                            wifiConnected = if (prefs.statusBarShowWifi) info.wifiConnected else null,
                            accentColor = accent
                        )
                    }
                }
        }
    }

    private fun clockTicker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(CLOCK_TICK_MS)
        }
    }

    private fun formatClock(use24h: Boolean): String {
        val pattern = if (use24h) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Facade doesn't act on window-state changes today; the config only listens for them
        // in case a future release wants per-app HUD visibility (e.g. hide the floating HUD
        // while a video/game app is foregrounded).
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!volumeHudEnabled) return super.onKeyEvent(event)
        if (event.action != KeyEvent.ACTION_DOWN) {
            // Still consume ACTION_UP for the keys we handle on ACTION_DOWN below, so the
            // system's own volume panel doesn't flash in on key-up.
            return if (event.keyCode in VOLUME_KEY_CODES) true else super.onKeyEvent(event)
        }

        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                adjustAndShowVolume(AudioManager.ADJUST_RAISE)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                adjustAndShowVolume(AudioManager.ADJUST_LOWER)
                true
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                adjustAndShowVolume(AudioManager.ADJUST_TOGGLE_MUTE)
                true
            }
            else -> super.onKeyEvent(event)
        }
    }

    /** Applies [direction] to the media stream directly (flags=0 suppresses the system's own
     * volume UI, which Facade's overlay replaces) and refreshes the HUD to match. */
    private fun adjustAndShowVolume(direction: Int) {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        showVolumeHud()
    }

    private fun showVolumeHud() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        overlayController.show(OverlaySurface.VOLUME_HUD) {
            VolumeHudOverlayContent(level = current.toFloat() / max.toFloat(), accentColor = volumeHudAccent)
        }

        // Mirrors the system volume panel's own auto-dismiss: restart the hide timer on every
        // press rather than hiding on a fixed schedule from the first one.
        volumeHudHideJob?.cancel()
        volumeHudHideJob = serviceScope.launch {
            delay(VOLUME_HUD_AUTO_HIDE_MS)
            overlayController.hide(OverlaySurface.VOLUME_HUD)
        }
    }

    override fun onInterrupt() {
        // Required override; Facade has no ongoing gesture/feedback loop to interrupt.
    }

    override fun onDestroy() {
        super.onDestroy()
        preferencesJob?.cancel()
        statusBarJob?.cancel()
        volumeHudHideJob?.cancel()
        serviceScope.cancel()
        if (::overlayController.isInitialized) overlayController.hideAll()
    }

    private companion object {
        const val VOLUME_HUD_AUTO_HIDE_MS = 1_500L
        const val CLOCK_TICK_MS = 15_000L
        val VOLUME_KEY_CODES = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE
        )
    }
}

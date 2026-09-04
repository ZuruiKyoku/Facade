package com.slygames.facade.services.overlay

import android.accessibilityservice.AccessibilityService
import android.text.format.DateFormat
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.slygames.facade.data.local.datastore.LauncherPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
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

    @Inject lateinit var preferencesRepository: LauncherPreferencesRepository

    private lateinit var overlayController: OverlayWindowController
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var preferencesJob: Job? = null

    @Volatile
    private var volumeHudEnabled = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayController = OverlayWindowController(applicationContext)
        observeOverlayToggles()
    }

    private fun observeOverlayToggles() {
        preferencesJob?.cancel()
        preferencesJob = serviceScope.launch {
            preferencesRepository.preferencesFlow
                .map { Triple(it.overlayStatusBarEnabled, it.overlayVolumeHudEnabled, it.overlayFloatingHudEnabled) }
                .distinctUntilChanged()
                .collect { (statusBar, volumeHud, floatingHud) ->
                    applyToggle(OverlaySurface.STATUS_BAR, statusBar) {
                        StatusBarOverlayContent(
                            clockText = DateFormat.getTimeFormat(this@FacadeAccessibilityService).format(Date())
                        )
                    }
                    applyToggle(OverlaySurface.VOLUME_HUD, volumeHud) { VolumeHudOverlayContent(level = 0.5f) }
                    applyToggle(OverlaySurface.FLOATING_HUD, floatingHud) { FloatingHudOverlayContent() }
                    volumeHudEnabled = volumeHud
                }
        }
    }

    private inline fun applyToggle(
        surface: OverlaySurface,
        enabled: Boolean,
        crossinline content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        if (enabled) overlayController.show(surface) { content() } else overlayController.hide(surface)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Facade doesn't act on window-state changes today; the config only listens for them
        // in case a future release wants per-app HUD visibility (e.g. hide the floating HUD
        // while a video/game app is foregrounded).
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!volumeHudEnabled) return super.onKeyEvent(event)

        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // TODO: adjust AudioManager stream volume directly and update the volume HUD's
                // displayed level, then consume the event so the system's own volume panel
                // never appears alongside Facade's.
                true
            }
            else -> super.onKeyEvent(event)
        }
    }

    override fun onInterrupt() {
        // Required override; Facade has no ongoing gesture/feedback loop to interrupt.
    }

    override fun onDestroy() {
        super.onDestroy()
        preferencesJob?.cancel()
        serviceScope.cancel()
        if (::overlayController.isInitialized) overlayController.hideAll()
    }
}

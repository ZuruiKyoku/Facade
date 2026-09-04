package com.slygames.facade.services.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.slygames.facade.core.designsystem.FacadeTheme

/**
 * Identifies one floating overlay surface Facade can mount independently
 * (each gated by its own Settings > Overlays toggle), so
 * [FacadeAccessibilityService] can add/remove/update them without them
 * fighting over a single shared window.
 */
enum class OverlaySurface { STATUS_BAR, VOLUME_HUD, FLOATING_HUD }

/**
 * Adds/removes [TYPE_ACCESSIBILITY_OVERLAY][WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY]
 * windows hosting Compose content on behalf of [FacadeAccessibilityService].
 * This layout type is only usable from an active [android.accessibilityservice.AccessibilityService]
 * (unlike `TYPE_APPLICATION_OVERLAY`, it needs no separate `SYSTEM_ALERT_WINDOW`
 * grant beyond the accessibility service itself being enabled) - [serviceContext] must be the
 * service instance itself (`this`), not `applicationContext`, since only the service's own
 * Context carries the window token this layout type requires; `applicationContext`'s
 * WindowManager throws `BadTokenException` on `addView`.
 */
class OverlayWindowController(private val serviceContext: Context) {

    private val windowManager = serviceContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val activeWindows = mutableMapOf<OverlaySurface, ComposeView>()
    private val lifecycleOwners = mutableMapOf<OverlaySurface, ComposeOverlayLifecycleOwner>()

    fun show(surface: OverlaySurface, content: @Composable () -> Unit) {
        if (activeWindows.containsKey(surface)) {
            // Already shown; caller should update content by calling show() again with new state,
            // which recomposes in place since setContent is idempotent per ComposeView instance.
            activeWindows[surface]?.setContent { FacadeTheme { content() } }
            return
        }

        val lifecycleOwner = ComposeOverlayLifecycleOwner().also { it.onCreate() }
        val composeView = ComposeView(serviceContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { FacadeTheme { content() } }
        }

        windowManager.addView(composeView, layoutParamsFor(surface))
        activeWindows[surface] = composeView
        lifecycleOwners[surface] = lifecycleOwner
    }

    fun hide(surface: OverlaySurface) {
        val view = activeWindows.remove(surface) ?: return
        try {
            windowManager.removeViewImmediate(view)
        } catch (_: IllegalArgumentException) {
            // View already detached (e.g. the accessibility service was disabled mid-teardown).
        }
        lifecycleOwners.remove(surface)?.onDestroy()
    }

    fun hideAll() {
        activeWindows.keys.toList().forEach(::hide)
    }

    private fun layoutParamsFor(surface: OverlaySurface): WindowManager.LayoutParams {
        val (gravity, height) = when (surface) {
            OverlaySurface.STATUS_BAR -> Gravity.TOP to WindowManager.LayoutParams.WRAP_CONTENT
            OverlaySurface.VOLUME_HUD -> Gravity.CENTER to WindowManager.LayoutParams.WRAP_CONTENT
            OverlaySurface.FLOATING_HUD -> (Gravity.TOP or Gravity.END) to WindowManager.LayoutParams.WRAP_CONTENT
        }
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            height,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}

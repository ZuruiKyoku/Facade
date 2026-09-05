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
enum class OverlaySurface { STATUS_BAR, VOLUME_HUD }

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

    /** Windows of the same [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] type stack by
     * insertion order (last added wins), not by anything resembling a CSS z-index. This list is
     * the actual stacking policy - back to front - and [restack] enforces it deterministically
     * after every new window is added, so whichever surface was toggled on most recently can't
     * arbitrarily end up covering the others. */
    private val zOrderBackToFront = listOf(OverlaySurface.STATUS_BAR, OverlaySurface.VOLUME_HUD)

    fun show(surface: OverlaySurface, content: @Composable () -> Unit) {
        val existing = activeWindows[surface]
        if (existing != null) {
            // Already shown; caller should update content by calling show() again with new
            // state, which recomposes in place since setContent is idempotent per ComposeView.
            existing.setContent { FacadeTheme { content() } }
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
        restack()
    }

    /** Re-adds every currently active window in [zOrderBackToFront] order, so the one meant to be
     * frontmost always ends up frontmost regardless of which order surfaces were toggled on in. */
    private fun restack() {
        zOrderBackToFront.forEach { surface ->
            val view = activeWindows[surface] ?: return@forEach
            try {
                windowManager.removeViewImmediate(view)
            } catch (_: IllegalArgumentException) {
                return@forEach
            }
            windowManager.addView(view, layoutParamsFor(surface))
        }
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
        val (gravity, width, height) = when (surface) {
            // This window draws above the real status bar (that's the point - see
            // StatusBarOverlayContent), so it spans the full width and is sized to exactly the
            // real status bar's height, rather than floating a small pill that just doubles up
            // with the system clock/icons underneath it. The height is resolved here (not left
            // to Compose's WindowInsets.statusBars, which measured 0 on a real Galaxy Z Fold -
            // that inset isn't reliably dispatched to a window added directly via WindowManager
            // outside of an Activity's own decor view, apparently varying by OEM/device).
            OverlaySurface.STATUS_BAR -> Triple(
                Gravity.TOP or Gravity.START,
                WindowManager.LayoutParams.MATCH_PARENT,
                statusBarHeightPx()
            )
            OverlaySurface.VOLUME_HUD -> Triple(
                Gravity.CENTER,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        return WindowManager.LayoutParams(
            width,
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

    /** The platform's own status bar height, read the same way the framework itself has always
     * exposed it (there's no public API for it) - far more reliable across OEMs than asking
     * Compose's WindowInsets for it from a non-Activity window. Falls back to a reasonable
     * density-scaled default if the internal resource is ever missing. */
    private fun statusBarHeightPx(): Int {
        val resources = serviceContext.resources
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) {
            resources.getDimensionPixelSize(resId)
        } else {
            (24 * resources.displayMetrics.density).toInt()
        }
    }
}

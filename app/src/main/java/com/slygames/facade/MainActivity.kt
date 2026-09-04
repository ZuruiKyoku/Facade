package com.slygames.facade

import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.slygames.facade.core.designsystem.FacadeTheme
import com.slygames.facade.features.appdrawer.AppDrawerScreen
import com.slygames.facade.features.launcher.widget.FacadeAppWidgetHostViewManager
import com.slygames.facade.features.launcher.workspace.WorkspaceScreen
import com.slygames.facade.features.overlays.OverlaySettingsScreen
import com.slygames.facade.features.settings.SettingsScreen
import com.slygames.facade.features.systemtweaks.SystemTweaksScreen
import com.slygames.facade.features.wallpaper.WallpaperPickerScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Facade's single HOME activity (`MAIN`/`HOME`/`DEFAULT`/`LAUNCHER` in the
 * manifest). Everything else - the app drawer, settings, wallpaper picker,
 * overlay/system-tweak screens - is a Compose destination reached via
 * [NavHost] from here rather than a separate Activity, so the process never
 * pays a second cold-start cost after the user is already home.
 *
 * Once Facade actually holds the HOME role, a plain/default-priority back
 * callback (which is all [NavHost] registers on its own) never sees the
 * system back button/gesture at all: Android treats it as a task-level
 * "return to home" action and reacts to it before dispatch ever reaches an
 * app-registered callback of normal priority - there is nothing "behind"
 * the home task to return to at the OS's level, only [NavController]'s own
 * in-app back stack knows about the app drawer/settings/etc. Registering
 * our own callback at [OnBackInvokedDispatcher.PRIORITY_OVERLAY] (API 33+;
 * the AndroidX-compat dispatcher on older devices, where this interception
 * doesn't happen) intercepts ahead of that system handling instead of
 * fighting it, and pops [navController] manually.
 *
 * (The other half of "back didn't close the app drawer": `WorkspaceGridView`
 * fired its swipe-up callback once per qualifying touch-move sample instead
 * of once per gesture, pushing several duplicate `app_drawer` back-stack
 * entries per swipe - so a single back press just landed on the next
 * duplicate instead of the workspace. Fixed at the source, with
 * `launchSingleTop` here too as a second line of defense.)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var widgetHostViewManager: FacadeAppWidgetHostViewManager

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerBackCallback()

        setContent {
            FacadeTheme {
                navController = rememberNavController()
                FacadeNavHost(
                    widgetHostViewManager = widgetHostViewManager,
                    navController = navController!!
                )
            }
        }
    }

    private fun registerBackCallback() {
        val popCurrentDestination = { navController?.popBackStack() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_OVERLAY
            ) { popCurrentDestination() }
        } else {
            onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        popCurrentDestination()
                    }
                }
            )
        }
    }
}

private object Routes {
    const val WORKSPACE = "workspace"
    const val APP_DRAWER = "app_drawer"
    const val SETTINGS = "settings"
    const val WALLPAPER_PICKER = "settings/wallpaper"
    const val OVERLAYS = "settings/overlays"
    const val SYSTEM_TWEAKS = "settings/system_tweaks"
}

@Composable
private fun FacadeNavHost(
    widgetHostViewManager: FacadeAppWidgetHostViewManager,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.WORKSPACE) {
        composable(Routes.WORKSPACE) {
            WorkspaceScreen(
                modifier = Modifier,
                widgetHostViewManager = widgetHostViewManager,
                onOpenAppDrawer = {
                    // Defense in depth against WorkspaceGridView's swipe-up firing more than
                    // once for a single gesture: launchSingleTop skips pushing a second
                    // app_drawer entry (and the back-stack duplication that caused) if it's
                    // already on top, rather than relying solely on that call site being fixed.
                    navController.navigate(Routes.APP_DRAWER) { launchSingleTop = true }
                }
            )
        }
        composable(
            Routes.APP_DRAWER,
            // Mirrors the swipe-up gesture that opens it: slides up from the bottom on the way
            // in, and back down on the way out (navigating forward from here, e.g. into
            // Settings, keeps the default transition).
            enterTransition = { slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn() },
            popExitTransition = { slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut() }
        ) {
            AppDrawerScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToWallpaperPicker = { navController.navigate(Routes.WALLPAPER_PICKER) },
                onNavigateToOverlays = { navController.navigate(Routes.OVERLAYS) },
                onNavigateToSystemTweaks = { navController.navigate(Routes.SYSTEM_TWEAKS) }
            )
        }
        composable(Routes.WALLPAPER_PICKER) {
            WallpaperPickerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.OVERLAYS) {
            OverlaySettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SYSTEM_TWEAKS) {
            SystemTweaksScreen(onBack = { navController.popBackStack() })
        }
    }
}

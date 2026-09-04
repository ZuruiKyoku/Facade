package com.slygames.facade

import android.os.Bundle
import androidx.activity.ComponentActivity
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
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var widgetHostViewManager: FacadeAppWidgetHostViewManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FacadeTheme {
                FacadeNavHost(widgetHostViewManager = widgetHostViewManager)
            }
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
                onOpenAppDrawer = { navController.navigate(Routes.APP_DRAWER) }
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

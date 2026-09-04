package com.slygames.facade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.slygames.facade.core.designsystem.FacadeTheme
import com.slygames.facade.features.overlays.OverlaySettingsScreen
import com.slygames.facade.features.settings.SettingsScreen
import com.slygames.facade.features.systemtweaks.SystemTweaksScreen
import com.slygames.facade.features.wallpaper.WallpaperPickerScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Facade's single activity - a normal (non-HOME) app entry point. Settings,
 * the wallpaper picker, and the overlay/system-tweak screens are Compose
 * destinations reached via [NavHost] from here rather than separate
 * Activities, so the process never pays a second cold-start cost.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FacadeTheme {
                FacadeNavHost()
            }
        }
    }
}

private object Routes {
    const val SETTINGS = "settings"
    const val WALLPAPER_PICKER = "settings/wallpaper"
    const val OVERLAYS = "settings/overlays"
    const val SYSTEM_TWEAKS = "settings/system_tweaks"
}

@Composable
private fun FacadeNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.SETTINGS) {
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

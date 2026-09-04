package com.slygames.facade.core.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FacadeDarkColorScheme = darkColorScheme(
    primary = FacadeDarkPrimary,
    onPrimary = FacadeDarkOnPrimary,
    primaryContainer = FacadeDarkPrimaryContainer,
    onPrimaryContainer = FacadeDarkOnPrimaryContainer,
    secondary = FacadeDarkSecondary,
    onSecondary = FacadeDarkOnSecondary,
    secondaryContainer = FacadeDarkSecondaryContainer,
    onSecondaryContainer = FacadeDarkOnSecondaryContainer,
    tertiary = FacadeDarkTertiary,
    onTertiary = FacadeDarkOnTertiary,
    background = FacadeDarkBackground,
    onBackground = FacadeDarkOnBackground,
    surface = FacadeDarkSurface,
    onSurface = FacadeDarkOnSurface,
    surfaceVariant = FacadeDarkSurfaceVariant,
    onSurfaceVariant = FacadeDarkOnSurfaceVariant,
    outline = FacadeDarkOutline,
    error = FacadeDarkError,
    onError = FacadeDarkOnError
)

private val FacadeLightColorScheme = lightColorScheme(
    primary = FacadeLightPrimary,
    onPrimary = FacadeLightOnPrimary,
    primaryContainer = FacadeLightPrimaryContainer,
    onPrimaryContainer = FacadeLightOnPrimaryContainer,
    secondary = FacadeLightSecondary,
    onSecondary = FacadeLightOnSecondary,
    secondaryContainer = FacadeLightSecondaryContainer,
    onSecondaryContainer = FacadeLightOnSecondaryContainer,
    tertiary = FacadeLightTertiary,
    onTertiary = FacadeLightOnTertiary,
    background = FacadeLightBackground,
    onBackground = FacadeLightOnBackground,
    surface = FacadeLightSurface,
    onSurface = FacadeLightOnSurface,
    surfaceVariant = FacadeLightSurfaceVariant,
    onSurfaceVariant = FacadeLightOnSurfaceVariant,
    outline = FacadeLightOutline,
    error = FacadeLightError,
    onError = FacadeLightOnError
)

/**
 * Root Material3 theme for every Compose surface in Facade (app drawer, dock,
 * settings, wallpaper picker, overlay toggles). The coordinate-based
 * workspace grid is a plain View hierarchy and reads its palette separately
 * via [WorkspaceIconLabelStyle] / XML theme attributes, so it stays outside
 * this composition.
 *
 * @param dynamicColor When true (default) and running on Android 12+, derive
 * the scheme from the user's wallpaper via Material You dynamic color.
 */
@Composable
fun FacadeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FacadeDarkColorScheme
        else -> FacadeLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FacadeTypography,
        shapes = FacadeShapes,
        content = content
    )
}

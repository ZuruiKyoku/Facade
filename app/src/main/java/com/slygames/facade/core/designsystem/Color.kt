package com.slygames.facade.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Facade's brand palette. These are the static fallback tokens used on API
 * levels without dynamic color (Android 12-), or when the user disables
 * wallpaper-derived theming in Settings > Desktop.
 */

// Light scheme
val FacadeLightPrimary = Color(0xFF6750A4)
val FacadeLightOnPrimary = Color(0xFFFFFFFF)
val FacadeLightPrimaryContainer = Color(0xFFEADDFF)
val FacadeLightOnPrimaryContainer = Color(0xFF21005D)
val FacadeLightSecondary = Color(0xFF625B71)
val FacadeLightOnSecondary = Color(0xFFFFFFFF)
val FacadeLightSecondaryContainer = Color(0xFFE8DEF8)
val FacadeLightOnSecondaryContainer = Color(0xFF1D192B)
val FacadeLightTertiary = Color(0xFF7D5260)
val FacadeLightOnTertiary = Color(0xFFFFFFFF)
val FacadeLightBackground = Color(0xFFFFFBFE)
val FacadeLightOnBackground = Color(0xFF1C1B1F)
val FacadeLightSurface = Color(0xFFFFFBFE)
val FacadeLightOnSurface = Color(0xFF1C1B1F)
val FacadeLightSurfaceVariant = Color(0xFFE7E0EC)
val FacadeLightOnSurfaceVariant = Color(0xFF49454F)
val FacadeLightOutline = Color(0xFF79747E)
val FacadeLightError = Color(0xFFB3261E)
val FacadeLightOnError = Color(0xFFFFFFFF)

// Dark scheme
val FacadeDarkPrimary = Color(0xFFD0BCFF)
val FacadeDarkOnPrimary = Color(0xFF381E72)
val FacadeDarkPrimaryContainer = Color(0xFF4F378B)
val FacadeDarkOnPrimaryContainer = Color(0xFFEADDFF)
val FacadeDarkSecondary = Color(0xFFCCC2DC)
val FacadeDarkOnSecondary = Color(0xFF332D41)
val FacadeDarkSecondaryContainer = Color(0xFF4A4458)
val FacadeDarkOnSecondaryContainer = Color(0xFFE8DEF8)
val FacadeDarkTertiary = Color(0xFFEFB8C8)
val FacadeDarkOnTertiary = Color(0xFF492532)
val FacadeDarkBackground = Color(0xFF1C1B1F)
val FacadeDarkOnBackground = Color(0xFFE6E1E5)
val FacadeDarkSurface = Color(0xFF1C1B1F)
val FacadeDarkOnSurface = Color(0xFFE6E1E5)
val FacadeDarkSurfaceVariant = Color(0xFF49454F)
val FacadeDarkOnSurfaceVariant = Color(0xFFCAC4D0)
val FacadeDarkOutline = Color(0xFF938F99)
val FacadeDarkError = Color(0xFFF2B8B5)
val FacadeDarkOnError = Color(0xFF601410)

/** Scrim used behind floating overlays (folders, widget resize handles, HUDs). */
val FacadeOverlayScrim = Color(0x66000000)

/** Default translucent workspace label backdrop, independent of light/dark scheme. */
val FacadeIconLabelScrim = Color(0x99000000)

package com.slygames.facade.services.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.slygames.facade.data.local.datastore.BatteryIconStyle
import kotlin.math.roundToInt

/**
 * Compose content mounted into each [OverlaySurface] window by [OverlayWindowController]. Kept
 * deliberately independent of any ViewModel/DataStore access - callers (mainly
 * [FacadeAccessibilityService]) resolve preferences/live state and pass plain parameters down, so
 * these stay easy to preview and reason about in isolation.
 */

@Composable
fun StatusBarOverlayContent(
    clockText: String?,
    batteryPercent: Int?,
    isCharging: Boolean,
    batteryStyle: BatteryIconStyle,
    wifiConnected: Boolean?,
    accentColor: Color?
) {
    // A full-width, fully opaque bar: this window draws above the system status bar (that's the
    // whole point - see OverlayWindowController), so anything less than fully opaque and
    // full-width just doubles up with the real clock/icons underneath instead of replacing them.
    // The window itself is already sized to exactly the real status bar's height by
    // OverlayWindowController, so this just fills it. The background stays a neutral theme
    // surface (matching how the real status bar always looks like part of the OS chrome, not a
    // branded widget) - accentColor only tints the glyphs drawn on top of it.
    val tint = accentColor ?: MaterialTheme.colorScheme.onSurface
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (clockText != null) {
                Text(
                    text = clockText,
                    color = tint,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 16.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = 16.dp)
            ) {
                if (wifiConnected != null) {
                    Icon(
                        if (wifiConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(15.dp)
                    )
                }
                if (batteryPercent != null) {
                    BatteryGlyph(percent = batteryPercent, charging = isCharging, style = batteryStyle, tint = tint)
                    Text(
                        text = "$batteryPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = tint
                    )
                }
            }
        }
    }
}

/** Hand-drawn rather than pulled from an icon set, so the three styles can genuinely differ
 * (stroke weight, corner rounding, whether the terminal "nub" shows) instead of all being the
 * same glyph with a different tint. */
@Composable
private fun BatteryGlyph(percent: Int, charging: Boolean, style: BatteryIconStyle, tint: Color) {
    val fraction = (percent.coerceIn(0, 100) / 100f)
    val fillColor = when {
        charging -> CHARGING_GREEN
        percent <= 15 -> LOW_BATTERY_RED
        else -> tint
    }
    val (strokeWidth, cornerRadius, showNub) = when (style) {
        BatteryIconStyle.CLASSIC -> Triple(1.4.dp, 2.dp, true)
        BatteryIconStyle.MINIMAL -> Triple(1.dp, 3.dp, false)
        BatteryIconStyle.BOLD -> Triple(2.2.dp, 4.dp, true)
    }
    val bodyWidth = if (style == BatteryIconStyle.MINIMAL) 20.dp else 18.dp
    Canvas(modifier = Modifier.size(width = bodyWidth + if (showNub) 2.dp else 0.dp, height = 10.dp)) {
        val nubWidth = if (showNub) 2.dp.toPx() else 0f
        val bodyWidthPx = bodyWidth.toPx()
        val strokePx = strokeWidth.toPx()
        val corner = CornerRadius(cornerRadius.toPx())

        drawRoundRect(
            color = tint,
            topLeft = Offset.Zero,
            size = Size(bodyWidthPx, size.height),
            cornerRadius = corner,
            style = Stroke(width = strokePx)
        )
        if (showNub) {
            val nubHeight = size.height * 0.5f
            drawRoundRect(
                color = tint,
                topLeft = Offset(bodyWidthPx + 0.5.dp.toPx(), (size.height - nubHeight) / 2f),
                size = Size(nubWidth, nubHeight),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
        val inset = strokePx + 1.dp.toPx()
        val fillWidth = ((bodyWidthPx - inset * 2) * fraction).coerceAtLeast(0f)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(inset, inset),
                size = Size(fillWidth, size.height - inset * 2),
                cornerRadius = CornerRadius((cornerRadius.toPx() - strokePx).coerceAtLeast(0f))
            )
        }
    }
}

@Composable
fun VolumeHudOverlayContent(level: Float, accentColor: Color? = null) {
    val clamped = level.coerceIn(0f, 1f)
    val tint = accentColor ?: MaterialTheme.colorScheme.primary
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                if (clamped <= 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = tint
            )
            LinearProgressIndicator(
                progress = { clamped },
                color = tint,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(width = 120.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Text(
                text = "${(clamped * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun FloatingHudOverlayContent(label: String, accentColor: Color? = null) {
    val background = accentColor ?: MaterialTheme.colorScheme.primaryContainer
    // contentColorFor only knows the fixed set of Material3 role colors; an arbitrary
    // user-picked accent needs its own readable-text decision instead of falling through to
    // whatever LocalContentColor happens to be.
    val onBackground = if (accentColor != null) readableTextColor(accentColor) else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(color = background, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = label,
            color = onBackground,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

/** Relative luminance (sRGB) thresholded at the standard 0.5 midpoint - simple, but reliable
 * across the full range of colors a user could pick from the accent swatch, unlike
 * [androidx.compose.material3.contentColorFor] which only special-cases known theme roles. */
private fun readableTextColor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.5f) Color.Black else Color.White
}

private val CHARGING_GREEN = Color(0xFF2E7D32)
private val LOW_BATTERY_RED = Color(0xFFC62828)

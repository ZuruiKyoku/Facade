package com.slygames.facade.services.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Placeholder Compose content for each [OverlaySurface] window. These are
 * intentionally simple - the point of this scaffold is the plumbing
 * ([OverlayWindowController] mounting real Compose content on a
 * `TYPE_ACCESSIBILITY_OVERLAY` window with no Activity backing it), not a
 * finished HUD design.
 */

@Composable
fun StatusBarOverlayContent(clockText: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Text(text = clockText, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    }
}

@Composable
fun VolumeHudOverlayContent(level: Float) {
    val clamped = level.coerceIn(0f, 1f)
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
                contentDescription = null
            )
            LinearProgressIndicator(
                progress = { clamped },
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
fun FloatingHudOverlayContent() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "Facade",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp)
        )
    }
}

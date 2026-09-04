package com.slygames.facade.services.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.VolumeUp, contentDescription = null)
            LinearProgressIndicator(
                progress = { level.coerceIn(0f, 1f) },
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(width = 120.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
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

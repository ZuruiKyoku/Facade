package com.slygames.facade.services.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
    // A full-width, fully opaque bar sized to exactly the real status bar's height: this window
    // draws above the system status bar (that's the whole point - see OverlayWindowController),
    // so anything less than fully opaque and full-width just doubles up with the real clock/
    // icons underneath instead of replacing them. Trade-off: this also visually covers the real
    // battery/signal/notification icons, which Facade doesn't reproduce (yet) - only the clock
    // renders on top for now.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxSize()
                // A punch-hole/notch camera isn't always centered, so this nudges the clock
                // clear of wherever this specific device's cutout actually is.
                .windowInsetsPadding(WindowInsets.displayCutout)
        ) {
            Text(text = clockText, modifier = Modifier.padding(horizontal = 16.dp))
        }
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

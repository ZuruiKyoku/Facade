package com.slygames.facade.features.overlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slygames.facade.core.permission.PermissionState
import com.slygames.facade.data.local.datastore.BatteryIconStyle

@Composable
fun OverlaySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverlaySettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Both prerequisite permissions can only change while Facade is backgrounded (the user
    // granting them in system Settings), so re-check on every resume rather than polling.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Surface overlays") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.overlayPermissionState != PermissionState.GRANTED) {
                ListItem(
                    headlineContent = { Text("Display over other apps") },
                    supportingContent = { Text("Required to draw overlay surfaces") },
                    trailingContent = {
                        Button(onClick = { context.startActivity(viewModel.buildOverlayPermissionIntent()) }) {
                            Text("Grant")
                        }
                    }
                )
            }
            if (state.accessibilityServiceState != PermissionState.GRANTED) {
                ListItem(
                    headlineContent = { Text("Facade Surface Overlays service") },
                    supportingContent = { Text("Required to intercept volume keys and mount overlays") },
                    trailingContent = {
                        Button(onClick = { context.startActivity(viewModel.buildAccessibilitySettingsIntent()) }) {
                            Text("Enable")
                        }
                    }
                )
            }

            val prefs = state.preferences

            ListItem(
                headlineContent = { Text("Custom status bar") },
                trailingContent = {
                    Switch(
                        checked = prefs.overlayStatusBarEnabled,
                        enabled = state.prerequisitesMet,
                        onCheckedChange = viewModel::setStatusBarOverlayEnabled
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Interceptable volume HUD") },
                trailingContent = {
                    Switch(
                        checked = prefs.overlayVolumeHudEnabled,
                        enabled = state.prerequisitesMet,
                        onCheckedChange = viewModel::setVolumeHudEnabled
                    )
                }
            )

            HorizontalDivider()
            SectionHeader("Status bar content")
            ListItem(
                headlineContent = { Text("Clock") },
                trailingContent = {
                    Switch(checked = prefs.statusBarShowClock, onCheckedChange = viewModel::setStatusBarShowClock)
                }
            )
            ListItem(
                headlineContent = { Text("24-hour time") },
                trailingContent = {
                    Switch(
                        checked = prefs.statusBarUse24HourClock,
                        enabled = prefs.statusBarShowClock,
                        onCheckedChange = viewModel::setStatusBarUse24HourClock
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Battery") },
                trailingContent = {
                    Switch(checked = prefs.statusBarShowBattery, onCheckedChange = viewModel::setStatusBarShowBattery)
                }
            )
            ListItem(
                headlineContent = { Text("Wi-Fi indicator") },
                trailingContent = {
                    Switch(checked = prefs.statusBarShowWifi, onCheckedChange = viewModel::setStatusBarShowWifi)
                }
            )
            ListItem(headlineContent = { Text("Battery icon style") })
            BatteryStyleRow(
                selected = prefs.statusBarBatteryStyle,
                onSelect = viewModel::setStatusBarBatteryStyle,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )

            HorizontalDivider()
            SectionHeader("Accent color")
            Text(
                text = "Tints the status bar's icons and text and the volume HUD.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            AccentColorRow(
                selectedArgb = prefs.overlayAccentColorArgb,
                onSelect = viewModel::setOverlayAccentColor,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun BatteryStyleRow(
    selected: BatteryIconStyle,
    onSelect: (BatteryIconStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        BatteryIconStyle.entries.forEach { style ->
            FilterChip(
                selected = style == selected,
                onClick = { onSelect(style) },
                label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun AccentColorRow(
    selectedArgb: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        // null = "follow the app's theme" - its own swatch rather than just omitting the choice,
        // so clearing a custom color is as easy as picking one.
        ColorSwatch(color = null, selected = selectedArgb == null, onClick = { onSelect(null) })
        ACCENT_PRESETS.forEach { preset ->
            ColorSwatch(color = preset, selected = selectedArgb == preset.toArgb(), onClick = { onSelect(preset.toArgb()) })
        }
    }
}

@Composable
private fun ColorSwatch(color: Color?, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
            .then(
                if (color != null) Modifier.background(color, CircleShape)
                else Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            )
            .border(border, CircleShape)
    ) {
        if (color == null) {
            Text("A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)

private val ACCENT_PRESETS = listOf(
    Color(0xFFFF6B4A), // coral
    Color(0xFFFFB300), // amber
    Color(0xFF43A047), // green
    Color(0xFF00897B), // teal
    Color(0xFF1E88E5), // blue
    Color(0xFF8E24AA), // purple
    Color(0xFFD81B60), // pink
    Color(0xFF546E7A)  // graphite
)

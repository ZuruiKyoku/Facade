package com.slygames.facade.features.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slygames.facade.core.permission.PermissionState

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
        Column(modifier = Modifier.padding(padding)) {
            if (state.overlayPermissionState != PermissionState.GRANTED) {
                ListItem(
                    headlineContent = { Text("Display over other apps") },
                    supportingContent = { Text("Required to draw floating HUDs") },
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

            ListItem(
                headlineContent = { Text("Custom status bar") },
                trailingContent = {
                    Switch(
                        checked = state.preferences.overlayStatusBarEnabled,
                        enabled = state.prerequisitesMet,
                        onCheckedChange = viewModel::setStatusBarOverlayEnabled
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Interceptable volume HUD") },
                trailingContent = {
                    Switch(
                        checked = state.preferences.overlayVolumeHudEnabled,
                        enabled = state.prerequisitesMet,
                        onCheckedChange = viewModel::setVolumeHudEnabled
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Floating HUD widgets") },
                trailingContent = {
                    Switch(
                        checked = state.preferences.overlayFloatingHudEnabled,
                        enabled = state.prerequisitesMet,
                        onCheckedChange = viewModel::setFloatingHudEnabled
                    )
                }
            )
        }
    }
}

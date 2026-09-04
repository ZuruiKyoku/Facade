package com.slygames.facade.features.systemtweaks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slygames.facade.services.shizuku.ShizukuConnectionState

@Composable
fun SystemTweaksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SystemTweaksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshConnection()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("System tweaks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text("Shizuku connection") },
                supportingContent = { Text(connectionLabel(state.connectionState)) },
                trailingContent = {
                    if (state.connectionState == ShizukuConnectionState.PERMISSION_REQUIRED) {
                        Button(onClick = viewModel::requestPermission) { Text("Connect") }
                    }
                }
            )
            if (state.connectionState == ShizukuConnectionState.UNAVAILABLE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(onClick = { context.startActivity(viewModel.buildInstallShizukuIntent()) }) {
                        Text("Get Shizuku")
                    }
                    OutlinedButton(onClick = { context.startActivity(viewModel.buildShizukuSetupGuideIntent()) }) {
                        Text("Setup guide")
                    }
                }
                Text(
                    text = "Shizuku itself has to be started via ADB or root - no app, Facade " +
                        "included, can grant itself shell privileges. The setup guide walks " +
                        "through both.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            val tweaksEnabled = state.connectionState == ShizukuConnectionState.READY

            ListItem(
                headlineContent = { Text("Animation speed") },
                supportingContent = { Text(if (tweaksEnabled) "Requires Shizuku" else "Connect Shizuku to unlock") }
            )
            AnimationScale.entries.forEach { scale ->
                ListItem(
                    headlineContent = { Text(scale.label) },
                    trailingContent = {
                        Button(enabled = tweaksEnabled, onClick = { viewModel.setAnimationScale(scale) }) {
                            Text("Apply")
                        }
                    }
                )
            }

            var systemUiTunerEnabled by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("System UI Tuner demo mode") },
                trailingContent = {
                    Switch(
                        checked = systemUiTunerEnabled,
                        enabled = tweaksEnabled,
                        onCheckedChange = {
                            systemUiTunerEnabled = it
                            viewModel.toggleSystemUiTuner(it)
                        }
                    )
                }
            )

            state.lastCommandResult?.let { result ->
                Text(
                    text = if (result.isSuccess) "Applied" else "Failed: ${result.stderr}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

private fun connectionLabel(state: ShizukuConnectionState): String = when (state) {
    ShizukuConnectionState.UNAVAILABLE -> "Shizuku not installed or not running"
    ShizukuConnectionState.PERMISSION_REQUIRED -> "Permission required"
    ShizukuConnectionState.READY -> "Connected"
}

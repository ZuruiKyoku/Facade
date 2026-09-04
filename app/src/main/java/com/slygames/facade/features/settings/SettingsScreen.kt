package com.slygames.facade.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slygames.facade.R
import com.slygames.facade.core.permission.FacadePermission
import com.slygames.facade.core.permission.PermissionState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onNavigateToWallpaperPicker: () -> Unit,
    onNavigateToOverlays: () -> Unit,
    onNavigateToSystemTweaks: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    permissionsViewModel: PermissionsViewModel = hiltViewModel()
) {
    val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()
    val permissionStates by permissionsViewModel.states.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { permissionsViewModel.refresh() }

    // Every permission here can only change while Facade is backgrounded (the user granting it
    // in system Settings, or via the RoleManager request sheet below), so re-check on resume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionsViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // RoleManager.createRequestRoleIntent's contract requires launching via
    // startActivityForResult (a plain startActivity leaves the system unable to identify the
    // calling package, so the request sheet finishes immediately without showing UI - see
    // RequestRoleActivity's "Package name cannot be null or empty" log).
    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { permissionsViewModel.refresh() }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionHeader(stringResource(R.string.settings_section_desktop)) }
            item {
                ListItem(
                    headlineContent = { Text("${stringResource(R.string.settings_grid_columns)} (${prefs.gridColumns})") },
                    supportingContent = {
                        Slider(
                            value = prefs.gridColumns.toFloat(),
                            valueRange = 3f..8f,
                            steps = 4,
                            onValueChange = { settingsViewModel.setGridSize(it.toInt(), prefs.gridRows) }
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("${stringResource(R.string.settings_grid_rows)} (${prefs.gridRows})") },
                    supportingContent = {
                        Slider(
                            value = prefs.gridRows.toFloat(),
                            valueRange = 3f..10f,
                            steps = 6,
                            onValueChange = { settingsViewModel.setGridSize(prefs.gridColumns, it.toInt()) }
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text("${stringResource(R.string.settings_icon_scale)} (${(prefs.iconScale * 100).roundToInt()}%)")
                    },
                    supportingContent = {
                        Slider(
                            value = prefs.iconScale,
                            valueRange = 0.5f..1.5f,
                            onValueChange = { settingsViewModel.setIconScale(it) }
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Show icon labels") },
                    trailingContent = {
                        Switch(checked = prefs.showIconLabels, onCheckedChange = settingsViewModel::setShowIconLabels)
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Wallpaper-based color (Material You)") },
                    trailingContent = {
                        Switch(checked = prefs.dynamicColorEnabled, onCheckedChange = settingsViewModel::setDynamicColorEnabled)
                    }
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_wallpaper)) }
            item {
                ListItem(
                    headlineContent = { Text("Wallpaper") },
                    supportingContent = { Text("Set a photo, or a looping video background") },
                    modifier = Modifier.clickable(onClick = onNavigateToWallpaperPicker)
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_overlays)) }
            item {
                ListItem(
                    headlineContent = { Text("Surface overlays") },
                    supportingContent = { Text("Floating HUDs, status bar, volume controls") },
                    modifier = Modifier.clickable(onClick = onNavigateToOverlays)
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_system_tweaks)) }
            item {
                ListItem(
                    headlineContent = { Text("Shizuku system tweaks") },
                    supportingContent = { Text("Animation scales, UI tuner toggles") },
                    modifier = Modifier.clickable(onClick = onNavigateToSystemTweaks)
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("Permissions") }
            items(FacadePermission.entries) { permission ->
                PermissionRow(
                    permission = permission,
                    state = permissionStates[permission] ?: PermissionState.DENIED,
                    onGrantClick = {
                        val intent = permissionsViewModel.buildRequestIntent(permission)
                        when {
                            intent != null && permission == FacadePermission.DEFAULT_LAUNCHER ->
                                roleRequestLauncher.launch(intent)
                            intent != null -> context.startActivity(intent)
                            permission == FacadePermission.SHIZUKU ->
                                permissionsViewModel.requestShizukuPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                        }
                    }
                )
            }
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
private fun PermissionRow(
    permission: FacadePermission,
    state: PermissionState,
    onGrantClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(permission.name.replace('_', ' ')) },
        supportingContent = { Text(state.name) },
        trailingContent = {
            if (state != PermissionState.GRANTED) {
                Button(onClick = onGrantClick) { Text(stringResource(R.string.permission_grant)) }
            }
        }
    )
}


private const val SHIZUKU_PERMISSION_REQUEST_CODE = 5721

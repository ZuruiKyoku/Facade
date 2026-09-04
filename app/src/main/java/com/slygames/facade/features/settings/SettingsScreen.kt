package com.slygames.facade.features.settings

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slygames.facade.R
import com.slygames.facade.core.permission.FacadePermission
import com.slygames.facade.core.permission.PermissionState

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

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionHeader(stringResource(R.string.settings_section_desktop)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_grid_columns)) },
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
                    headlineContent = { Text(stringResource(R.string.settings_grid_rows)) },
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
                    headlineContent = { Text(stringResource(R.string.settings_icon_scale)) },
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

            item { SectionHeader(stringResource(R.string.settings_section_dock)) }
            item {
                ListItem(
                    headlineContent = { Text("Dock icon slots") },
                    supportingContent = {
                        Slider(
                            value = prefs.dockSlotCount.toFloat(),
                            valueRange = 3f..7f,
                            steps = 3,
                            onValueChange = { settingsViewModel.setDockSlotCount(it.toInt()) }
                        )
                    }
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_wallpaper)) }
            item {
                ListItem(
                    headlineContent = { Text("Live wallpaper") },
                    supportingContent = { Text("Pick a looping video or animated background") },
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
                        if (intent != null) context.startActivity(intent)
                        else if (permission == FacadePermission.SHIZUKU) {
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

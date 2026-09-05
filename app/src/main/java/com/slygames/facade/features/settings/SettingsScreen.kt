package com.slygames.facade.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
    // in system Settings), so re-check on resume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionsViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionHeader("Appearance") }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
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
                    leadingContent = { Icon(Icons.Filled.Wallpaper, contentDescription = null) },
                    headlineContent = { Text("Wallpaper") },
                    supportingContent = { Text("Set a photo, or a looping video background") },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onNavigateToWallpaperPicker)
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_overlays)) }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Widgets, contentDescription = null) },
                    headlineContent = { Text("Surface overlays") },
                    supportingContent = { Text("Custom status bar, volume controls") },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onNavigateToOverlays)
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_system_tweaks)) }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    headlineContent = { Text("Shizuku system tweaks") },
                    supportingContent = { Text("Animation scales, UI tuner toggles") },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
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

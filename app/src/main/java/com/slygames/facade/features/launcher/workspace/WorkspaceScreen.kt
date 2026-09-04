package com.slygames.facade.features.launcher.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slygames.facade.core.util.IntentDispatcher
import com.slygames.facade.data.model.WorkspaceItem
import com.slygames.facade.features.launcher.dock.DockBar
import com.slygames.facade.features.launcher.widget.FacadeAppWidgetHostViewManager

/**
 * Composition root for the home screen: hosts the coordinate-based
 * [WorkspaceGridView] via [AndroidView] for the desktop grid, layers the
 * Compose [DockBar] on top, and owns the widget host's start/stopListening
 * lifecycle pairing.
 */
@Composable
fun WorkspaceScreen(
    onOpenAppDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    widgetHostViewManager: FacadeAppWidgetHostViewManager,
    modifier: Modifier = Modifier,
    viewModel: WorkspaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderState by viewModel.openFolder.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, widgetHostViewManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> widgetHostViewManager.startListening()
                Lifecycle.Event.ON_STOP -> widgetHostViewManager.stopListening()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onOpenAppDrawerState = rememberUpdatedState(onOpenAppDrawer)

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WorkspaceGridView(viewContext).apply {
                    this.widgetViewProvider = widgetHostViewManager
                    onSwipeUp = { onOpenAppDrawerState.value.invoke() }
                    listener = object : WorkspaceGridListener {
                        override fun onItemTapped(item: WorkspaceItem) {
                            when (item) {
                                is WorkspaceItem.App -> IntentDispatcher.launchApp(
                                    viewContext,
                                    item.appItem.packageName,
                                    item.appItem.activityName
                                )
                                is WorkspaceItem.Shortcut -> Unit // TODO: wire ShortcutManager.startShortcut
                                is WorkspaceItem.Folder -> viewModel.openFolder(item)
                                is WorkspaceItem.Widget -> Unit
                            }
                        }

                        override fun onItemLongPressed(item: WorkspaceItem) = Unit

                        override fun onItemMoved(itemId: Long, page: Int, cellX: Int, cellY: Int) {
                            viewModel.onItemMoved(itemId, page, cellX, cellY)
                        }

                        override fun onItemDroppedOnItem(draggedItemId: Long, targetItem: WorkspaceItem) {
                            viewModel.onItemDroppedOnItem(draggedItemId, targetItem)
                        }

                        override fun onItemDroppedOnFolder(draggedItemId: Long, folder: WorkspaceItem.Folder) {
                            viewModel.onItemDroppedOnFolder(draggedItemId, folder)
                        }

                        override fun onPageChanged(pageIndex: Int) = Unit

                        override fun onWorkspaceLongPressed(page: Int, cellX: Int, cellY: Int) = onOpenSettings()
                    }
                }
            },
            update = { view ->
                view.configureGrid(uiState.gridColumns, uiState.gridRows)
                view.submitPages(uiState.pagesByIndex)
            }
        )

        DockBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onAppClick = { appItem ->
                IntentDispatcher.launchApp(context, appItem.packageName, appItem.activityName)
            },
            onAppLongClick = { /* TODO: dock item context menu (remove / app info) */ }
        )
    }

    folderState.folder?.let { folder ->
        FolderSheet(
            folder = folder,
            onDismiss = viewModel::closeFolder,
            onRename = viewModel::renameOpenFolder
        )
    }
}

@Composable
private fun FolderSheet(
    folder: WorkspaceItem.Folder,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    val context = LocalContext.current
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    // Captured once per folder.id (not re-evaluated as `name` changes while typing) so a
    // brand-new, still-nameless folder gets the keyboard focused automatically - the only way
    // to name one is right here, there's no separate "rename" entry point for a fresh folder.
    val isNewFolder = remember(folder.id) { folder.name.isEmpty() }
    val focusRequester = remember(folder.id) { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(folder.id) {
        if (isNewFolder) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onRename(it)
                },
                modifier = Modifier.focusRequester(focusRequester),
                label = { Text("Folder name") },
                singleLine = true
            )
        },
        text = {
            Column {
                folder.items.forEach { item ->
                    if (item is WorkspaceItem.App) {
                        Text(
                            text = item.appItem.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    IntentDispatcher.launchApp(
                                        context,
                                        item.appItem.packageName,
                                        item.appItem.activityName
                                    )
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

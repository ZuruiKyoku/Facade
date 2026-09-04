package com.slygames.facade.features.launcher.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.data.local.datastore.LauncherPreferencesRepository
import com.slygames.facade.data.model.AppItem
import com.slygames.facade.data.model.WorkspaceItem
import com.slygames.facade.data.repository.AppRepository
import com.slygames.facade.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceUiState(
    val gridColumns: Int = 5,
    val gridRows: Int = 6,
    val pagesByIndex: Map<Int, List<WorkspaceItem>> = emptyMap(),
    val iconScale: Float = 1f,
    val showIconLabels: Boolean = true
)

/** Which item, if any, is currently open as a full-screen folder sheet. */
data class FolderSheetState(val folder: WorkspaceItem.Folder?)

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val appRepository: AppRepository,
    private val preferencesRepository: LauncherPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<WorkspaceUiState> = combine(
        workspaceRepository.observeDesktopPages(),
        preferencesRepository.preferencesFlow
    ) { pages, prefs ->
        WorkspaceUiState(
            gridColumns = prefs.gridColumns,
            gridRows = prefs.gridRows,
            pagesByIndex = pages.associate { it.pageIndex to it.items },
            iconScale = prefs.iconScale,
            showIconLabels = prefs.showIconLabels
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkspaceUiState())

    private val _openFolder = MutableStateFlow(FolderSheetState(null))
    val openFolder: StateFlow<FolderSheetState> = _openFolder.asStateFlow()
    private var openFolderJob: Job? = null

    /** Guards [seedDefaultLayoutOnFirstRun] against re-triggering while waiting for [LauncherPreferencesRepository] to persist the flag it just set. */
    private var seedAttempted = false

    init {
        refreshInstalledApps()
        seedDefaultLayoutOnFirstRun()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch { appRepository.refresh() }
    }

    /**
     * Fills the dock and page 0 from the installed-app list the very first
     * time the workspace has never had anything placed, so a fresh install
     * doesn't land on a totally empty grid. See
     * [WorkspaceRepository.seedDefaultLayoutIfEmpty].
     */
    private fun seedDefaultLayoutOnFirstRun() {
        viewModelScope.launch {
            combine(appRepository.apps, preferencesRepository.preferencesFlow) { apps, prefs -> apps to prefs }
                .collect { (apps, prefs) ->
                    if (seedAttempted || prefs.hasSeededDefaultLayout || apps.isEmpty()) return@collect
                    seedAttempted = true
                    workspaceRepository.seedDefaultLayoutIfEmpty(apps, prefs.gridColumns, prefs.gridRows)
                    preferencesRepository.setHasSeededDefaultLayout(true)
                }
        }
    }

    fun onItemMoved(itemId: Long, page: Int, cellX: Int, cellY: Int) {
        viewModelScope.launch { workspaceRepository.moveItem(itemId, page, cellX, cellY) }
    }

    fun onItemDroppedOnItem(draggedItemId: Long, targetItem: WorkspaceItem) {
        if (targetItem !is WorkspaceItem.App) return
        viewModelScope.launch {
            val folderId = workspaceRepository.createFolder(
                name = "",
                page = targetItem.screenPage,
                cellX = targetItem.cellX,
                cellY = targetItem.cellY
            )
            workspaceRepository.addItemToFolder(targetItem.id, folderId, rank = 0)
            workspaceRepository.addItemToFolder(draggedItemId, folderId, rank = 1)
            // Prompt for a name right away instead of leaving a nameless folder the user has to
            // remember to open and rename later - this is the only way to name a new folder.
            openFolder(
                WorkspaceItem.Folder(
                    id = folderId,
                    screenPage = targetItem.screenPage,
                    cellX = targetItem.cellX,
                    cellY = targetItem.cellY,
                    name = "",
                    items = emptyList()
                )
            )
        }
    }

    fun onItemDroppedOnFolder(draggedItemId: Long, folder: WorkspaceItem.Folder) {
        viewModelScope.launch {
            workspaceRepository.addItemToFolder(draggedItemId, folder.id, rank = folder.items.size)
        }
    }

    fun openFolder(folder: WorkspaceItem.Folder) {
        openFolderJob?.cancel()
        openFolderJob = viewModelScope.launch {
            workspaceRepository.observeFolder(folder.id).collect { hydrated ->
                _openFolder.value = FolderSheetState(hydrated ?: folder)
            }
        }
    }

    fun closeFolder() {
        openFolderJob?.cancel()
        _openFolder.value = FolderSheetState(null)
    }

    fun renameOpenFolder(name: String) {
        val folderId = _openFolder.value.folder?.id ?: return
        viewModelScope.launch { workspaceRepository.renameFolder(folderId, name) }
    }

    fun removeItem(itemId: Long) {
        viewModelScope.launch { workspaceRepository.removeItem(itemId) }
    }

    fun placeAppOnFirstFreeCell(appItem: AppItem, page: Int) {
        viewModelScope.launch {
            for (y in 0 until uiState.value.gridRows) {
                for (x in 0 until uiState.value.gridColumns) {
                    if (!workspaceRepository.isCellOccupied(page, x, y)) {
                        workspaceRepository.placeApp(appItem, page, x, y)
                        return@launch
                    }
                }
            }
        }
    }
}

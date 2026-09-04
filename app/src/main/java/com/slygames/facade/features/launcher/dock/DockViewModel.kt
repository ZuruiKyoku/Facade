package com.slygames.facade.features.launcher.dock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slygames.facade.data.local.datastore.LauncherPreferencesRepository
import com.slygames.facade.data.model.WorkspaceItem
import com.slygames.facade.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DockViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val preferencesRepository: LauncherPreferencesRepository
) : ViewModel() {

    val uiState = combine(
        workspaceRepository.observeDockItems(),
        preferencesRepository.preferencesFlow
    ) { items, prefs ->
        DockUiState(slotCount = prefs.dockSlotCount, items = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DockUiState())

    fun moveItem(itemId: Long, slotIndex: Int) {
        viewModelScope.launch {
            workspaceRepository.moveItem(itemId, page = DOCK_PAGE, cellX = slotIndex, cellY = 0)
        }
    }

    fun removeItem(item: WorkspaceItem) {
        viewModelScope.launch { workspaceRepository.removeItem(item.id) }
    }

    private companion object {
        /** Dock rows are stored with `isDockItem = true`, so `screenPage` is unused for placement; 0 is a stable default. */
        const val DOCK_PAGE = 0
    }
}

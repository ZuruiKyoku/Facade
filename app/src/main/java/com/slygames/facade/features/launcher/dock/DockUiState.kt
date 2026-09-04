package com.slygames.facade.features.launcher.dock

import com.slygames.facade.data.model.WorkspaceItem

data class DockUiState(
    val slotCount: Int = 5,
    val items: List<WorkspaceItem> = emptyList()
) {
    /** [items] padded/truncated to exactly [slotCount] entries (null = empty slot), in cellX order. */
    fun slots(): List<WorkspaceItem?> {
        val byCell = items.associateBy { it.cellX }
        return (0 until slotCount).map { byCell[it] }
    }
}

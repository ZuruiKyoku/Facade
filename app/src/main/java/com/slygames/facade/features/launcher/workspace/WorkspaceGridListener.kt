package com.slygames.facade.features.launcher.workspace

import com.slygames.facade.data.model.WorkspaceItem

/** Callbacks [WorkspaceGridView] fires up to whatever hosts it (a ViewModel-backed Compose screen). */
interface WorkspaceGridListener {
    fun onItemTapped(item: WorkspaceItem)
    fun onItemLongPressed(item: WorkspaceItem)

    /** A drag ended over an empty cell. */
    fun onItemMoved(itemId: Long, page: Int, cellX: Int, cellY: Int)

    /** A drag ended on top of another single app icon - offer/perform a folder merge. */
    fun onItemDroppedOnItem(draggedItemId: Long, targetItem: WorkspaceItem)

    /** A drag ended on top of an existing folder - add the dragged item into it. */
    fun onItemDroppedOnFolder(draggedItemId: Long, folder: WorkspaceItem.Folder)

    fun onPageChanged(pageIndex: Int)

    /** Empty-space long-press, used to surface "add widget" / "change wallpaper" / "settings". */
    fun onWorkspaceLongPressed(page: Int, cellX: Int, cellY: Int)
}

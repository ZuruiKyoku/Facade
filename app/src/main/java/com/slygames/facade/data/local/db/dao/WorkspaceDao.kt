package com.slygames.facade.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.slygames.facade.data.local.db.entity.WorkspaceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WorkspaceItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WorkspaceItemEntity>): List<Long>

    @Update
    suspend fun update(item: WorkspaceItemEntity)

    @Update
    suspend fun updateAll(items: List<WorkspaceItemEntity>)

    @Delete
    suspend fun delete(item: WorkspaceItemEntity)

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM workspace_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkspaceItemEntity?

    /** All desktop-grid items (not on the dock, not nested in a folder), grouped implicitly by page. */
    @Query(
        "SELECT * FROM workspace_items " +
            "WHERE isDockItem = 0 AND containerId IS NULL " +
            "ORDER BY screenPage ASC, cellY ASC, cellX ASC"
    )
    fun observeDesktopItems(): Flow<List<WorkspaceItemEntity>>

    @Query(
        "SELECT * FROM workspace_items " +
            "WHERE isDockItem = 0 AND containerId IS NULL AND screenPage = :page " +
            "ORDER BY cellY ASC, cellX ASC"
    )
    fun observePage(page: Int): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE isDockItem = 1 ORDER BY cellX ASC")
    fun observeDockItems(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE containerId = :folderId ORDER BY cellX ASC")
    fun observeFolderContents(folderId: Long): Flow<List<WorkspaceItemEntity>>

    /** Every item living inside ANY folder, across the whole workspace - used to build each closed folder's mini-icon preview without a per-folder query. */
    @Query("SELECT * FROM workspace_items WHERE containerId IS NOT NULL ORDER BY containerId ASC, cellX ASC")
    fun observeAllContainedItems(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT DISTINCT screenPage FROM workspace_items WHERE isDockItem = 0 AND containerId IS NULL ORDER BY screenPage ASC")
    suspend fun getUsedPageIndices(): List<Int>

    /** Every placed item regardless of dock/desktop/folder - used to decide whether the workspace is empty and needs a default layout seeded. */
    @Query("SELECT COUNT(*) FROM workspace_items")
    suspend fun countAll(): Int

    @Query(
        "SELECT COUNT(*) FROM workspace_items " +
            "WHERE isDockItem = 0 AND containerId IS NULL AND screenPage = :page " +
            "AND cellX = :cellX AND cellY = :cellY"
    )
    suspend fun countAtCell(page: Int, cellX: Int, cellY: Int): Int

    @Query("SELECT * FROM workspace_items WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getByAppWidgetId(appWidgetId: Int): WorkspaceItemEntity?

    @Query("DELETE FROM workspace_items WHERE appWidgetId = :appWidgetId")
    suspend fun deleteByAppWidgetId(appWidgetId: Int)

    @Query("DELETE FROM workspace_items WHERE folderMetadataId = :folderId")
    suspend fun deleteFolderPlaceholder(folderId: Long)

    @Query("DELETE FROM workspace_items WHERE packageName = :packageName")
    suspend fun deleteAllForPackage(packageName: String)

    @Transaction
    suspend fun moveItem(id: Long, newPage: Int, newCellX: Int, newCellY: Int) {
        val item = getById(id) ?: return
        update(item.copy(screenPage = newPage, cellX = newCellX, cellY = newCellY))
    }
}

package com.slygames.facade.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.slygames.facade.data.local.db.entity.FolderItemEntity
import com.slygames.facade.data.local.db.entity.WorkspaceItemEntity
import kotlinx.coroutines.flow.Flow

/** A folder's own placement metadata joined with the [WorkspaceItemEntity] rows it contains. */
data class FolderWithItems(
    @Embedded val folder: FolderItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "containerId"
    )
    val items: List<WorkspaceItemEntity>
)

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderItemEntity): Long

    @Update
    suspend fun update(folder: FolderItemEntity)

    @Delete
    suspend fun delete(folder: FolderItemEntity)

    @Query("DELETE FROM folder_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM folder_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FolderItemEntity?

    @Query("SELECT * FROM folder_items ORDER BY screenPage ASC")
    fun observeAll(): Flow<List<FolderItemEntity>>

    @Transaction
    @Query("SELECT * FROM folder_items WHERE id = :id LIMIT 1")
    fun observeFolderWithItems(id: Long): Flow<FolderWithItems?>

    @Transaction
    @Query("SELECT * FROM folder_items ORDER BY screenPage ASC")
    fun observeAllWithItems(): Flow<List<FolderWithItems>>

    @Query("UPDATE folder_items SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: Long, name: String)
}

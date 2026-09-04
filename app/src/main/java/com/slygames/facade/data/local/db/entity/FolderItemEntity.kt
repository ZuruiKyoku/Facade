package com.slygames.facade.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A folder placed directly on the desktop grid. The apps/shortcuts/widgets
 * it contains are ordinary [WorkspaceItemEntity] rows whose [WorkspaceItemEntity.containerId]
 * points back at [id] - mirroring how Android's stock launcher databases
 * model folders as a container id rather than a separate item graph.
 */
@Entity(
    tableName = "folder_items",
    indices = [
        Index(value = ["screenPage"]),
        Index(value = ["screenPage", "cellX", "cellY"], unique = true)
    ]
)
data class FolderItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "screenPage")
    val screenPage: Int,

    @ColumnInfo(name = "cellX")
    val cellX: Int,

    @ColumnInfo(name = "cellY")
    val cellY: Int
)

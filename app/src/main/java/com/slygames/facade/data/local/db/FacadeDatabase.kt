package com.slygames.facade.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.slygames.facade.data.local.db.dao.FolderDao
import com.slygames.facade.data.local.db.dao.WorkspaceDao
import com.slygames.facade.data.local.db.entity.FolderItemEntity
import com.slygames.facade.data.local.db.entity.WorkspaceItemEntity

/** Room database backing the desktop grid, dock, and folder layout. */
@Database(
    entities = [
        WorkspaceItemEntity::class,
        FolderItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FacadeDatabase : RoomDatabase() {

    abstract fun workspaceDao(): WorkspaceDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val DATABASE_NAME = "facade_workspace.db"
    }
}

package com.slygames.facade.data.local.db

import androidx.room.TypeConverter
import com.slygames.facade.data.model.WorkspaceItemType

class Converters {

    @TypeConverter
    fun fromWorkspaceItemType(type: WorkspaceItemType): String = type.name

    @TypeConverter
    fun toWorkspaceItemType(value: String): WorkspaceItemType =
        WorkspaceItemType.entries.firstOrNull { it.name == value } ?: WorkspaceItemType.APP
}

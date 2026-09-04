package com.slygames.facade.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.slygames.facade.data.model.WorkspaceItemType

/**
 * A single placed item on the desktop grid, dock, or inside a folder: an
 * app shortcut, a pinned deep-link shortcut, a bound widget, or a folder
 * placeholder itself (see [WorkspaceItemType.FOLDER], whose row points at
 * the matching [FolderItemEntity.id] via [folderMetadataId]).
 *
 * When [containerId] is non-null this row lives inside that folder instead
 * of directly on a desktop page; [screenPage]/[cellX]/[cellY] are then
 * unused for placement and [cellX] instead holds the item's sort rank
 * within the folder.
 */
@Entity(
    tableName = "workspace_items",
    foreignKeys = [
        ForeignKey(
            entity = FolderItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["containerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["containerId"]),
        Index(value = ["screenPage"]),
        Index(value = ["screenPage", "cellX", "cellY"]),
        Index(value = ["packageName"]),
        Index(value = ["appWidgetId"])
    ]
)
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "itemType")
    val itemType: WorkspaceItemType,

    @ColumnInfo(name = "packageName")
    val packageName: String? = null,

    @ColumnInfo(name = "className")
    val className: String? = null,

    /** Custom or pinned-shortcut label override; null means "use the app/shortcut's own label". */
    @ColumnInfo(name = "label")
    val label: String? = null,

    /** Shortcut id from [android.content.pm.ShortcutInfo], only set for SHORTCUT rows. */
    @ColumnInfo(name = "shortcutId")
    val shortcutId: String? = null,

    @ColumnInfo(name = "screenPage")
    val screenPage: Int = 0,

    @ColumnInfo(name = "cellX")
    val cellX: Int = 0,

    @ColumnInfo(name = "cellY")
    val cellY: Int = 0,

    @ColumnInfo(name = "spanX")
    val spanX: Int = 1,

    @ColumnInfo(name = "spanY")
    val spanY: Int = 1,

    /** Bound [android.appwidget.AppWidgetHost] id; -1 (or null) unless [itemType] is WIDGET. */
    @ColumnInfo(name = "appWidgetId")
    val appWidgetId: Int? = null,

    /** True for rows placed on the dock instead of a desktop page. */
    @ColumnInfo(name = "isDockItem", defaultValue = "0")
    val isDockItem: Boolean = false,

    /** FK to [FolderItemEntity.id] when this item lives inside a folder. */
    @ColumnInfo(name = "containerId")
    val containerId: Long? = null,

    /** When [itemType] is FOLDER, the matching [FolderItemEntity.id] carrying the folder's own metadata. */
    @ColumnInfo(name = "folderMetadataId")
    val folderMetadataId: Long? = null
)

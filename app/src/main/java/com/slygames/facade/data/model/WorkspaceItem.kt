package com.slygames.facade.data.model

/** Discriminator persisted on [com.slygames.facade.data.local.db.entity.WorkspaceItemEntity]. */
enum class WorkspaceItemType {
    APP,
    SHORTCUT,
    WIDGET,
    FOLDER
}

/**
 * Domain-level, placement-aware representation of anything the custom
 * workspace [android.view.ViewGroup] can render at a grid cell: an app
 * shortcut, a pinned deep-link shortcut, a bound widget, or a folder of
 * other items. This is the hydrated counterpart of
 * [com.slygames.facade.data.local.db.entity.WorkspaceItemEntity] - the
 * entity is what Room persists; this is what the UI layer consumes, joined
 * against [AppItem] / [WidgetItem] lookups from the repositories.
 */
sealed class WorkspaceItem {
    abstract val id: Long
    abstract val screenPage: Int
    abstract val cellX: Int
    abstract val cellY: Int
    abstract val spanX: Int
    abstract val spanY: Int

    data class App(
        override val id: Long,
        override val screenPage: Int,
        override val cellX: Int,
        override val cellY: Int,
        val appItem: AppItem
    ) : WorkspaceItem() {
        override val spanX: Int = 1
        override val spanY: Int = 1
    }

    data class Shortcut(
        override val id: Long,
        override val screenPage: Int,
        override val cellX: Int,
        override val cellY: Int,
        val packageName: String,
        val shortcutId: String,
        val label: String,
        val iconResourceName: String?
    ) : WorkspaceItem() {
        override val spanX: Int = 1
        override val spanY: Int = 1
    }

    data class Widget(
        override val id: Long,
        override val screenPage: Int,
        override val cellX: Int,
        override val cellY: Int,
        override val spanX: Int,
        override val spanY: Int,
        val widgetItem: WidgetItem
    ) : WorkspaceItem()

    data class Folder(
        override val id: Long,
        override val screenPage: Int,
        override val cellX: Int,
        override val cellY: Int,
        val name: String,
        val items: List<WorkspaceItem> = emptyList()
    ) : WorkspaceItem() {
        override val spanX: Int = 1
        override val spanY: Int = 1
    }
}

/** One page of the paginated desktop grid, in placement order. */
data class WorkspacePage(
    val pageIndex: Int,
    val items: List<WorkspaceItem>
)

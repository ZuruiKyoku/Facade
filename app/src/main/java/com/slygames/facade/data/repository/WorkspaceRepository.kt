package com.slygames.facade.data.repository

import android.appwidget.AppWidgetManager
import android.content.Context
import com.slygames.facade.data.local.db.dao.FolderDao
import com.slygames.facade.data.local.db.dao.WorkspaceDao
import com.slygames.facade.data.local.db.entity.FolderItemEntity
import com.slygames.facade.data.local.db.entity.WorkspaceItemEntity
import com.slygames.facade.data.model.AppItem
import com.slygames.facade.data.model.WidgetItem
import com.slygames.facade.data.model.WorkspaceItem
import com.slygames.facade.data.model.WorkspaceItemType
import com.slygames.facade.data.model.WorkspacePage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the persisted [WorkspaceItemEntity]/[FolderItemEntity] rows to the
 * hydrated [WorkspaceItem] domain model the workspace grid and dock render,
 * resolving each row's [AppItem] via [AppRepository] and each bound widget's
 * live [android.appwidget.AppWidgetProviderInfo] via [AppWidgetManager].
 */
@Singleton
class WorkspaceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workspaceDao: WorkspaceDao,
    private val folderDao: FolderDao,
    private val appRepository: AppRepository
) {
    private val appWidgetManager: AppWidgetManager by lazy { AppWidgetManager.getInstance(context) }

    fun observeDesktopPages(): Flow<List<WorkspacePage>> =
        combine(workspaceDao.observeDesktopItems(), appRepository.apps, folderDao.observeAll()) { entities, _, folders ->
            val folderNames = folders.associate { it.id to it.name }
            entities
                .mapNotNull { it.toDomainOrNull(folderNames) }
                .groupBy { it.screenPage }
                .toSortedMap()
                .map { (page, items) -> WorkspacePage(page, items) }
        }

    fun observeDockItems(): Flow<List<WorkspaceItem>> =
        combine(workspaceDao.observeDockItems(), appRepository.apps, folderDao.observeAll()) { entities, _, folders ->
            val folderNames = folders.associate { it.id to it.name }
            entities.mapNotNull { it.toDomainOrNull(folderNames) }
        }

    fun observeFolder(folderId: Long): Flow<WorkspaceItem.Folder?> =
        folderDao.observeFolderWithItems(folderId).map { withItems ->
            withItems?.let { (folder, items) ->
                WorkspaceItem.Folder(
                    id = folder.id,
                    screenPage = folder.screenPage,
                    cellX = folder.cellX,
                    cellY = folder.cellY,
                    name = folder.name,
                    items = items.mapNotNull { it.toDomainOrNull() }
                )
            }
        }

    suspend fun placeApp(appItem: AppItem, page: Int, cellX: Int, cellY: Int, isDock: Boolean = false): Long =
        workspaceDao.insert(
            WorkspaceItemEntity(
                itemType = WorkspaceItemType.APP,
                packageName = appItem.packageName,
                className = appItem.activityName,
                label = appItem.label,
                screenPage = page,
                cellX = cellX,
                cellY = cellY,
                isDockItem = isDock
            )
        )

    suspend fun placeWidget(widgetItem: WidgetItem, page: Int, cellX: Int, cellY: Int): Long =
        workspaceDao.insert(
            WorkspaceItemEntity(
                itemType = WorkspaceItemType.WIDGET,
                packageName = widgetItem.provider.packageName,
                className = widgetItem.provider.className,
                screenPage = page,
                cellX = cellX,
                cellY = cellY,
                spanX = widgetItem.defaultSpanX,
                spanY = widgetItem.defaultSpanY,
                appWidgetId = widgetItem.appWidgetId
            )
        )

    suspend fun moveItem(id: Long, page: Int, cellX: Int, cellY: Int) =
        workspaceDao.moveItem(id, page, cellX, cellY)

    suspend fun resizeWidget(id: Long, spanX: Int, spanY: Int) {
        val entity = workspaceDao.getById(id) ?: return
        workspaceDao.update(entity.copy(spanX = spanX, spanY = spanY))
    }

    /**
     * Removes a placed item. For [WorkspaceItemType.WIDGET] rows this only
     * drops the placement record - releasing the host-side `appWidgetId`
     * (and its live [android.appwidget.AppWidgetHostView]) is the caller's
     * job via `FacadeAppWidgetHostViewManager.releaseWidget()`, since that
     * lifecycle belongs to the widget host, not this repository.
     */
    suspend fun removeItem(id: Long) {
        workspaceDao.deleteById(id)
    }

    suspend fun removeByAppWidgetId(appWidgetId: Int) = workspaceDao.deleteByAppWidgetId(appWidgetId)

    suspend fun removeAllForPackage(packageName: String) = workspaceDao.deleteAllForPackage(packageName)

    suspend fun isCellOccupied(page: Int, cellX: Int, cellY: Int): Boolean =
        workspaceDao.countAtCell(page, cellX, cellY) > 0

    /**
     * First-run convenience: rather than land on a totally empty grid and
     * dock, fill the dock's slots and page 0 with the first installed apps
     * (already alphabetically sorted by [AppRepository]) the very first time
     * the workspace has zero placed items. A no-op once anything has ever
     * been placed, so it never fights a user who intentionally clears their
     * layout - callers gate repeat attempts with
     * [com.slygames.facade.data.local.datastore.LauncherPreferencesRepository].
     */
    suspend fun seedDefaultLayoutIfEmpty(apps: List<AppItem>, dockSlotCount: Int, columns: Int, rows: Int) {
        if (apps.isEmpty() || workspaceDao.countAll() > 0) return

        val dockApps = apps.take(dockSlotCount)
        val desktopApps = apps.drop(dockApps.size).take(columns * rows)

        val dockEntities = dockApps.mapIndexed { index, app -> app.toWorkspaceEntity(page = 0, cellX = index, cellY = 0, isDock = true) }
        val desktopEntities = desktopApps.mapIndexed { index, app ->
            app.toWorkspaceEntity(page = 0, cellX = index % columns, cellY = index / columns, isDock = false)
        }
        workspaceDao.insertAll(dockEntities + desktopEntities)
    }

    private fun AppItem.toWorkspaceEntity(page: Int, cellX: Int, cellY: Int, isDock: Boolean) = WorkspaceItemEntity(
        itemType = WorkspaceItemType.APP,
        packageName = packageName,
        className = activityName,
        label = label,
        screenPage = page,
        cellX = cellX,
        cellY = cellY,
        isDockItem = isDock
    )

    suspend fun createFolder(name: String, page: Int, cellX: Int, cellY: Int): Long {
        val folderId = folderDao.insert(FolderItemEntity(name = name, screenPage = page, cellX = cellX, cellY = cellY))
        workspaceDao.insert(
            WorkspaceItemEntity(
                itemType = WorkspaceItemType.FOLDER,
                screenPage = page,
                cellX = cellX,
                cellY = cellY,
                folderMetadataId = folderId
            )
        )
        return folderId
    }

    suspend fun renameFolder(folderId: Long, name: String) = folderDao.renameFolder(folderId, name)

    suspend fun addItemToFolder(itemId: Long, folderId: Long, rank: Int) {
        val entity = workspaceDao.getById(itemId) ?: return
        workspaceDao.update(entity.copy(containerId = folderId, cellX = rank, isDockItem = false))
    }

    suspend fun removeItemFromFolder(itemId: Long, page: Int, cellX: Int, cellY: Int) {
        val entity = workspaceDao.getById(itemId) ?: return
        workspaceDao.update(entity.copy(containerId = null, screenPage = page, cellX = cellX, cellY = cellY))
    }

    suspend fun deleteFolder(folderId: Long) {
        // Any WorkspaceItemEntity rows with containerId == folderId cascade-delete via the FK
        // once the FolderItemEntity row itself is removed.
        folderDao.getById(folderId)?.let { folderDao.delete(it) }
        // The FOLDER placeholder row on the desktop points at folderId but isn't itself
        // contained by it, so it's removed explicitly.
        workspaceDao.deleteFolderPlaceholder(folderId)
    }

    private fun WorkspaceItemEntity.toDomainOrNull(folderNames: Map<Long, String> = emptyMap()): WorkspaceItem? = when (itemType) {
        WorkspaceItemType.APP -> {
            val componentKey = "${packageName.orEmpty()}/${className.orEmpty()}"
            val appItem = appRepository.getByComponentKey(componentKey) ?: return null
            WorkspaceItem.App(id, screenPage, cellX, cellY, appItem)
        }
        WorkspaceItemType.SHORTCUT -> WorkspaceItem.Shortcut(
            id = id,
            screenPage = screenPage,
            cellX = cellX,
            cellY = cellY,
            packageName = packageName.orEmpty(),
            shortcutId = shortcutId.orEmpty(),
            label = label.orEmpty(),
            iconResourceName = null
        )
        WorkspaceItemType.WIDGET -> {
            val widgetId = appWidgetId ?: return null
            val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId)
            val widgetItem = WidgetItem(
                provider = providerInfo?.provider
                    ?: android.content.ComponentName(packageName.orEmpty(), className.orEmpty()),
                label = providerInfo?.loadLabel(context.packageManager) ?: label.orEmpty(),
                previewImage = providerInfo?.loadPreviewImage(context, 0),
                minSpanX = spanX,
                minSpanY = spanY,
                defaultSpanX = spanX,
                defaultSpanY = spanY,
                resizeMode = providerInfo?.resizeMode ?: 0,
                configureComponent = providerInfo?.configure,
                appWidgetId = widgetId
            )
            WorkspaceItem.Widget(id, screenPage, cellX, cellY, spanX, spanY, widgetItem)
        }
        WorkspaceItemType.FOLDER -> {
            // Folder contents are resolved on-demand via observeFolder(); the desktop-level
            // placeholder only needs its own metadata (name resolved from FolderItemEntity,
            // since the placeholder row itself carries no label).
            val resolvedFolderId = folderMetadataId ?: id
            WorkspaceItem.Folder(
                id = resolvedFolderId,
                screenPage = screenPage,
                cellX = cellX,
                cellY = cellY,
                name = folderNames[resolvedFolderId].orEmpty(),
                items = emptyList()
            )
        }
    }
}

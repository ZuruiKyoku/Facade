package com.slygames.facade.features.launcher.workspace

import android.view.View
import com.slygames.facade.data.model.WorkspaceItem

/**
 * Diffs a [WorkspacePageView]'s current children against a new
 * [WorkspaceItem] list and mutates the page in place - reusing/updating
 * views whose item id is still present, creating views for new items, and
 * removing views for items that left the page. There is deliberately no
 * adapter/ViewHolder recycling pool: a desktop page holds at most
 * `columns x rows` children, so plain add/remove is both simpler and,
 * unlike RecyclerView churn, keeps [android.appwidget.AppWidgetHostView]
 * instances alive across rebinds (destroying one tears down its remote view
 * connection).
 */
object WorkspaceGridBinder {

    /** Supplies the live [android.appwidget.AppWidgetHostView] for a placed widget; see `features.launcher.widget`. */
    fun interface WidgetViewProvider {
        fun provideView(widget: WorkspaceItem.Widget): View
    }

    fun bind(
        pageView: WorkspacePageView,
        items: List<WorkspaceItem>,
        widgetViewProvider: WidgetViewProvider? = null
    ) {
        val existingByItemId = (0 until pageView.childCount)
            .map { pageView.getChildAt(it) }
            .associateBy { (it.layoutParams as? WorkspaceLayoutParams)?.itemId ?: -1L }

        val keptItemIds = mutableSetOf<Long>()

        items.forEach { item ->
            keptItemIds += item.id
            val existing = existingByItemId[item.id]
            val reusable = existing != null && isReusable(existing, item)

            val view = if (reusable) {
                updateInPlace(existing!!, item)
                existing
            } else {
                existing?.let { pageView.removeView(it) }
                createView(pageView, item, widgetViewProvider)
            }

            (view.layoutParams as? WorkspaceLayoutParams)?.apply {
                cellX = item.cellX
                cellY = item.cellY
                spanX = item.spanX
                spanY = item.spanY
            } ?: run {
                view.layoutParams = WorkspaceLayoutParams(item.id, item.cellX, item.cellY, item.spanX, item.spanY)
            }

            if (view.parent == null) pageView.addView(view)
            pageView.relayoutChild(view)
        }

        existingByItemId.forEach { (itemId, view) ->
            if (itemId !in keptItemIds) pageView.removeView(view)
        }
    }

    private fun isReusable(view: View, item: WorkspaceItem): Boolean = when (item) {
        is WorkspaceItem.App, is WorkspaceItem.Shortcut -> view is WorkspaceIconView
        is WorkspaceItem.Folder -> view is FolderPreviewView
        is WorkspaceItem.Widget -> true // widget host views are opaque to this binder; trust the provider's identity
    }

    private fun updateInPlace(view: View, item: WorkspaceItem) {
        when (item) {
            is WorkspaceItem.App -> (view as? WorkspaceIconView)?.updateContent(item.appItem.icon, item.appItem.label)
            is WorkspaceItem.Shortcut -> (view as? WorkspaceIconView)?.updateContent(null, item.label)
            is WorkspaceItem.Folder -> (view as? FolderPreviewView)?.updateFolder(item)
            is WorkspaceItem.Widget -> Unit // handled entirely by the widget host manager
        }
    }

    private fun createView(
        pageView: WorkspacePageView,
        item: WorkspaceItem,
        widgetViewProvider: WidgetViewProvider?
    ): View = when (item) {
        is WorkspaceItem.App -> WorkspaceIconView(pageView.context, item.appItem.icon, item.appItem.label)
        is WorkspaceItem.Shortcut -> WorkspaceIconView(pageView.context, null, item.label)
        is WorkspaceItem.Folder -> FolderPreviewView(pageView.context, item).apply {
            onTap = { /* wired by the caller once the view is attached; see WorkspaceGridView */ }
        }
        is WorkspaceItem.Widget -> widgetViewProvider?.provideView(item)
            ?: placeholderWidgetView(pageView, item)
    }

    private fun placeholderWidgetView(pageView: WorkspacePageView, item: WorkspaceItem.Widget): View =
        android.widget.TextView(pageView.context).apply {
            text = item.widgetItem.label
            setBackgroundColor(0x22000000)
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
        }
}

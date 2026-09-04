package com.slygames.facade.features.launcher.widget

import android.appwidget.AppWidgetHostView
import android.content.ComponentName
import android.util.Log
import android.view.View
import com.slygames.facade.data.model.WorkspaceItem
import com.slygames.facade.features.launcher.workspace.WorkspaceGridBinder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches one live [AppWidgetHostView] per bound `appWidgetId` so
 * [com.slygames.facade.features.launcher.workspace.WorkspaceGridBinder]
 * never has to recreate (and thereby reconnect) a widget's remote view
 * across rebinds - it just asks this manager for the view it already made.
 *
 * Implements [WorkspaceGridBinder.WidgetViewProvider] directly so
 * `WorkspaceGridView.widgetViewProvider` can point straight at this class.
 */
@Singleton
class FacadeAppWidgetHostViewManager @Inject constructor(
    private val host: FacadeAppWidgetHost
) : WorkspaceGridBinder.WidgetViewProvider {

    private val hostViews = mutableMapOf<Int, AppWidgetHostView>()

    override fun provideView(widget: WorkspaceItem.Widget): View {
        val appWidgetId = widget.widgetItem.appWidgetId
        hostViews[appWidgetId]?.let { return it }

        val hostView = host.createHostView(appWidgetId)
        return if (hostView != null) {
            hostViews[appWidgetId] = hostView
            hostView
        } else {
            Log.w(TAG, "No provider info for widget $appWidgetId (provider ${widget.widgetItem.provider}); showing placeholder")
            placeholderView(appWidgetId, widget.widgetItem.provider)
        }
    }

    /** Allocates a fresh host-side id for a new widget placement, before any bind attempt. */
    fun allocateWidgetId(): Int = host.allocateNewWidgetId()

    /** True if Facade could bind without the system's confirmation dialog. */
    fun tryBindWithoutPrompt(appWidgetId: Int, provider: ComponentName): Boolean =
        host.bindWidgetIfAllowed(appWidgetId, provider)

    fun bindPermissionRequestExtras(appWidgetId: Int, provider: ComponentName) =
        host.bindPermissionExtras(appWidgetId, provider)

    fun releaseWidget(appWidgetId: Int) {
        hostViews.remove(appWidgetId)
        host.deleteWidgetId(appWidgetId)
    }

    fun startListening() = host.startListeningSafely()
    fun stopListening() = host.stopListeningSafely()

    private fun placeholderView(appWidgetId: Int, provider: ComponentName): View =
        android.widget.TextView(host.context).apply {
            text = provider.flattenToShortString()
            setBackgroundColor(0x22FF0000)
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
        }

    private companion object {
        const val TAG = "FacadeWidgetHostViewMgr"
    }
}

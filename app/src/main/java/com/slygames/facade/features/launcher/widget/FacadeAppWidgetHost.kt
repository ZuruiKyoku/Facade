package com.slygames.facade.features.launcher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Parcelable
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade's single [AppWidgetHost] instance. Android identifies each host
 * within a process by an arbitrary int id (unrelated to any single widget's
 * `appWidgetId`); Facade only ever runs one host, so [HOST_ID] is a fixed
 * constant rather than something generated per-launch.
 *
 * Lifecycle contract (leak-safety): [startListening] must be called from
 * `onStart()` of whatever hosts the workspace and [stopListening] from its
 * matching `onStop()` - see `MainActivity`. Skipping `stopListening()` keeps
 * every bound widget's remote view connection (and its process) alive after
 * Facade is backgrounded.
 */
@Singleton
class FacadeAppWidgetHost @Inject constructor(
    @ApplicationContext val context: Context
) : AppWidgetHost(context, HOST_ID) {

    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private var isListening = false

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = FacadeAppWidgetHostView(context)

    fun startListeningSafely() {
        if (isListening) return
        try {
            startListening()
            isListening = true
        } catch (e: Exception) {
            Log.w(TAG, "startListening failed", e)
        }
    }

    fun stopListeningSafely() {
        if (!isListening) return
        try {
            stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "stopListening failed", e)
        } finally {
            isListening = false
        }
    }

    fun allocateNewWidgetId(): Int = allocateAppWidgetId()

    fun deleteWidgetId(appWidgetId: Int) {
        try {
            deleteAppWidgetId(appWidgetId)
        } catch (e: Exception) {
            Log.w(TAG, "deleteAppWidgetId($appWidgetId) failed", e)
        }
    }

    fun getProviderInfo(appWidgetId: Int): AppWidgetProviderInfo? = appWidgetManager.getAppWidgetInfo(appWidgetId)

    /**
     * Attempts a permission-less bind (works when Facade already holds
     * `BIND_APPWIDGET`, which the system implicitly grants to the default
     * home app). Returns false when the caller must instead launch
     * [AppWidgetManager.ACTION_APPWIDGET_BIND] and let the user confirm.
     */
    fun bindWidgetIfAllowed(appWidgetId: Int, provider: android.content.ComponentName): Boolean =
        try {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        } catch (e: Exception) {
            Log.w(TAG, "bindAppWidgetIdIfAllowed failed for $provider", e)
            false
        }

    fun createHostView(appWidgetId: Int): AppWidgetHostView? {
        val info = getProviderInfo(appWidgetId) ?: return null
        return createView(context, appWidgetId, info).apply {
            setAppWidget(appWidgetId, info)
        }
    }

    /** Bundle payload Facade attaches to `ACTION_APPWIDGET_BIND` when it needs user confirmation. */
    fun bindPermissionExtras(appWidgetId: Int, provider: android.content.ComponentName): android.os.Bundle =
        android.os.Bundle().apply {
            putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider as Parcelable)
            putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

    companion object {
        private const val TAG = "FacadeAppWidgetHost"
        const val HOST_ID = 0x4641_4341 // "FACA" - arbitrary but stable per-process host id
    }
}

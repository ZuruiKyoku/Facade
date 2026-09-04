package com.slygames.facade.data.model

import android.content.ComponentName
import android.graphics.drawable.Drawable

/**
 * A native [android.appwidget.AppWidgetProviderInfo] available to be dropped
 * onto the workspace, plus the live binding once placed (see
 * [com.slygames.facade.features.launcher.widget.FacadeAppWidgetHost]).
 */
data class WidgetItem(
    val provider: ComponentName,
    val label: String,
    val previewImage: Drawable?,
    /** Minimum footprint in grid cells, derived from the provider's declared dp minimums. */
    val minSpanX: Int,
    val minSpanY: Int,
    /** Default footprint suggested at bind time. */
    val defaultSpanX: Int,
    val defaultSpanY: Int,
    val resizeMode: Int,
    val configureComponent: ComponentName?,
    /** Non-zero once bound to a live [android.appwidget.AppWidgetHost] instance. */
    val appWidgetId: Int = INVALID_APP_WIDGET_ID
) {
    val isBound: Boolean get() = appWidgetId != INVALID_APP_WIDGET_ID

    companion object {
        const val INVALID_APP_WIDGET_ID = -1
    }
}

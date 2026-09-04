package com.slygames.facade.features.launcher.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration

/**
 * Thin [AppWidgetHostView] subclass that lets [com.slygames.facade.features.launcher.workspace.WorkspaceGridView]
 * intercept a long-press for drag-and-drop before the hosted remote view
 * (which often consumes touch events itself, e.g. a scrollable widget)
 * swallows it.
 */
class FacadeAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPressIntercept: (() -> Unit)? = null

    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private var longPressRunnable: Runnable? = null
    private var downX = 0f
    private var downY = 0f

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                val runnable = Runnable { onLongPressIntercept?.invoke() }
                longPressRunnable = runnable
                postDelayed(runnable, longPressTimeout)
            }
            MotionEvent.ACTION_MOVE -> {
                val slop = ViewConfiguration.get(context).scaledTouchSlop
                if (kotlin.math.abs(event.x - downX) > slop || kotlin.math.abs(event.y - downY) > slop) {
                    cancelPendingLongPress()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelPendingLongPress()
        }
        return super.dispatchTouchEvent(event)
    }

    private fun cancelPendingLongPress() {
        longPressRunnable?.let { removeCallbacks(it) }
        longPressRunnable = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelPendingLongPress()
    }
}

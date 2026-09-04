package com.slygames.facade.features.launcher.workspace

import android.view.View
import com.slygames.facade.core.util.CoordinateMapper
import com.slygames.facade.data.model.WorkspaceItem

/**
 * Owns the touch-driven lifecycle of a single icon/widget drag: lifting the
 * child view (scale + elevation), translating it under the finger, resolving
 * the drop target cell through [CoordinateMapper], and reporting the outcome
 * (moved to an empty cell, dropped on another icon, or dropped on a folder)
 * back through [Callbacks]. [WorkspaceGridView] owns touch dispatch and page
 * paging; this class only owns "what happens to the dragged view itself".
 */
class WorkspaceDragController(private val callbacks: Callbacks) {

    interface Callbacks {
        fun currentPageView(): WorkspacePageView?
        fun currentPageIndex(): Int
        fun coordinateMapper(): CoordinateMapper?
        fun findItem(itemId: Long): WorkspaceItem?
        fun onDropped(itemId: Long, page: Int, cellX: Int, cellY: Int)
        fun onDroppedOnItem(draggedItemId: Long, targetItem: WorkspaceItem)
        fun onDroppedOnFolder(draggedItemId: Long, folder: WorkspaceItem.Folder)
        /** direction: -1 near the left page edge, 1 near the right page edge, 0 to cancel auto-scroll. */
        fun requestEdgeAutoScroll(direction: Int)
    }

    private var draggedView: View? = null
    private var draggedItemId: Long = -1L
    private var lastRawX = 0f
    private var lastRawY = 0f

    val isDragging: Boolean get() = draggedView != null

    fun beginDrag(view: View, itemId: Long, rawX: Float, rawY: Float) {
        draggedView = view
        draggedItemId = itemId
        lastRawX = rawX
        lastRawY = rawY
        view.animate().scaleX(LIFT_SCALE).scaleY(LIFT_SCALE).alpha(DRAG_ALPHA).setDuration(ANIM_DURATION_MS).start()
        view.translationZ = DRAG_ELEVATION_PX
        (view.parent as? android.view.ViewGroup)?.requestDisallowInterceptTouchEvent(true)
    }

    fun updateDrag(rawX: Float, rawY: Float) {
        val view = draggedView ?: return
        view.translationX += (rawX - lastRawX)
        view.translationY += (rawY - lastRawY)
        lastRawX = rawX
        lastRawY = rawY

        val pageView = callbacks.currentPageView() ?: return
        if (pageView.width == 0) return
        val edgeZone = pageView.width * EDGE_ZONE_FRACTION
        val centerXInPage = view.x + view.translationX + view.width / 2f
        when {
            centerXInPage < edgeZone -> callbacks.requestEdgeAutoScroll(-1)
            centerXInPage > pageView.width - edgeZone -> callbacks.requestEdgeAutoScroll(1)
            else -> callbacks.requestEdgeAutoScroll(0)
        }
    }

    fun endDrag() {
        val view = draggedView ?: return
        val itemId = draggedItemId
        val mapper = callbacks.coordinateMapper()
        val pageView = callbacks.currentPageView()

        val dropCenterX = view.x + view.translationX + view.width / 2f
        val dropCenterY = view.y + view.translationY + view.height / 2f

        view.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .translationX(0f).translationY(0f)
            .setDuration(ANIM_DURATION_MS)
            .withEndAction { view.translationZ = 0f }
            .start()

        if (mapper != null && pageView != null) {
            val cell = mapper.pixelToCell(dropCenterX, dropCenterY)
            val targetView = pageView.findChildAtCell(cell.cellX, cell.cellY)
            val targetLp = targetView?.layoutParams as? WorkspaceLayoutParams

            if (targetLp != null && targetLp.itemId != itemId) {
                when (val targetItem = callbacks.findItem(targetLp.itemId)) {
                    is WorkspaceItem.Folder -> callbacks.onDroppedOnFolder(itemId, targetItem)
                    null -> callbacks.onDropped(itemId, callbacks.currentPageIndex(), cell.cellX, cell.cellY)
                    else -> callbacks.onDroppedOnItem(itemId, targetItem)
                }
            } else {
                callbacks.onDropped(itemId, callbacks.currentPageIndex(), cell.cellX, cell.cellY)
            }
        }

        callbacks.requestEdgeAutoScroll(0)
        draggedView = null
        draggedItemId = -1L
    }

    fun cancelDrag() {
        draggedView?.animate()
            ?.scaleX(1f)?.scaleY(1f)?.alpha(1f)
            ?.translationX(0f)?.translationY(0f)
            ?.setDuration(ANIM_DURATION_MS)
            ?.withEndAction { draggedView?.translationZ = 0f }
            ?.start()
        callbacks.requestEdgeAutoScroll(0)
        draggedView = null
        draggedItemId = -1L
    }

    private companion object {
        const val LIFT_SCALE = 1.12f
        const val DRAG_ALPHA = 0.85f
        const val ANIM_DURATION_MS = 140L
        const val DRAG_ELEVATION_PX = 24f
        const val EDGE_ZONE_FRACTION = 0.08f
    }
}

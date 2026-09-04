package com.slygames.facade.features.launcher.workspace

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller
import com.slygames.facade.core.util.CoordinateMapper
import com.slygames.facade.data.model.WorkspaceItem
import kotlin.math.abs

/**
 * The paginated desktop grid: a horizontal sequence of [WorkspacePageView]
 * pages that this [ViewGroup] scrolls/flings between, with a built-in
 * long-press-to-drag controller ([WorkspaceDragController]) for reordering
 * icons and widgets. There is no adapter/RecyclerView here on purpose - all
 * pages are laid out as direct children so drag-and-drop, resize handles,
 * and nested [android.appwidget.AppWidgetHostView]s never pay a rebind cost
 * mid-gesture.
 */
class WorkspaceGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), WorkspaceDragController.Callbacks {

    var listener: WorkspaceGridListener? = null
    var onSwipeUp: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null

    /** Supplied by the widget host manager; see `features.launcher.widget.FacadeAppWidgetHostViewManager`. */
    var widgetViewProvider: WorkspaceGridBinder.WidgetViewProvider? = null

    private val pages = mutableListOf<WorkspacePageView>()
    private val itemsByPage = mutableMapOf<Int, List<WorkspaceItem>>()
    private val itemLookup = mutableMapOf<Long, WorkspaceItem>()

    private var gridColumns = 5
    private var gridRows = 6
    private var coordinateMapper: CoordinateMapper? = null

    private val scroller = OverScroller(context)
    private var velocityTracker: VelocityTracker? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

    private var currentPage = 0
    private var downX = 0f
    private var downY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastX = 0f
    private var isPagingDrag = false
    private var pressedChild: View? = null
    private var pressedItem: WorkspaceItem? = null
    private var longPressFired = false

    private val dragController = WorkspaceDragController(this)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var edgeScrollDirection = 0
    private val edgeScrollRunnable = object : Runnable {
        override fun run() {
            if (edgeScrollDirection == 0) return
            val target = (currentPage + edgeScrollDirection).coerceIn(0, pages.lastIndex.coerceAtLeast(0))
            if (target != currentPage) {
                smoothScrollToPage(target)
            }
            mainHandler.postDelayed(this, EDGE_SCROLL_INTERVAL_MS)
        }
    }

    private val longPressRunnable = Runnable {
        val child = pressedChild ?: return@Runnable
        val item = pressedItem ?: return@Runnable
        if (isPagingDrag) return@Runnable
        longPressFired = true
        listener?.onItemLongPressed(item)
        dragController.beginDrag(child, item.id, downRawX, downRawY)
    }

    // ------------------------------------------------------------------
    // Public configuration API
    // ------------------------------------------------------------------

    fun configureGrid(columns: Int, rows: Int) {
        if (gridColumns == columns && gridRows == rows) return
        gridColumns = columns
        gridRows = rows
        requestLayout()
    }

    /** Full re-sync of every page's contents; cheap enough to call on every repository emission since diffing happens per-page. */
    fun submitPages(newPagesByIndex: Map<Int, List<WorkspaceItem>>) {
        itemsByPage.clear()
        itemsByPage.putAll(newPagesByIndex)
        itemLookup.clear()
        newPagesByIndex.values.forEach { pageItems -> pageItems.forEach { itemLookup[it.id] = it } }

        val requiredPageCount = ((newPagesByIndex.keys.maxOrNull() ?: -1) + 1).coerceAtLeast(1)
        while (pages.size < requiredPageCount) addPageInternal()
        while (pages.size > requiredPageCount && pages.size > 1) removeLastPageInternal()

        pages.forEachIndexed { index, pageView ->
            bindPage(pageView, itemsByPage[index].orEmpty())
        }
        requestLayout()
    }

    fun currentPageIndex(): Int = currentPage

    // ------------------------------------------------------------------
    // Measure / layout
    // ------------------------------------------------------------------

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        if (width > 0 && height > 0) {
            coordinateMapper = CoordinateMapper(
                columns = gridColumns,
                rows = gridRows,
                cellWidthPx = width / gridColumns,
                cellHeightPx = height / gridRows
            )
        }

        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        pages.forEach { page ->
            page.coordinateMapper = coordinateMapper
            page.measure(childWidthSpec, childHeightSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        pages.forEachIndexed { index, page ->
            val left = index * width
            page.layout(left, 0, left + width, b - t)
        }
        if (!scroller.isFinished) return
        scrollTo(currentPage * width, 0)
    }

    // ------------------------------------------------------------------
    // Page management
    // ------------------------------------------------------------------

    private fun addPageInternal(): WorkspacePageView {
        val page = WorkspacePageView(context)
        pages.add(page)
        addView(page)
        return page
    }

    private fun removeLastPageInternal() {
        val page = pages.removeLastOrNull() ?: return
        removeView(page)
    }

    private fun bindPage(pageView: WorkspacePageView, items: List<WorkspaceItem>) {
        WorkspaceGridBinder.bind(pageView, items, widgetViewProvider)
        pageView.children().forEach { child ->
            wireChildInteractions(child)
        }
    }

    private fun wireChildInteractions(child: View) {
        val lp = child.layoutParams as? WorkspaceLayoutParams ?: return
        val item = itemLookup[lp.itemId] ?: return
        when (child) {
            is WorkspaceIconView -> {
                child.onTap = { listener?.onItemTapped(item) }
                child.onLongPress = { listener?.onItemLongPressed(item) }
            }
            is FolderPreviewView -> child.onTap = { listener?.onItemTapped(item) }
        }
    }

    private fun ViewGroup.children(): List<View> = (0 until childCount).map { getChildAt(it) }

    fun smoothScrollToPage(index: Int) {
        val target = index.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        currentPage = target
        val startX = scrollX
        val dx = target * width - startX
        scroller.startScroll(startX, 0, dx, 0, PAGE_SCROLL_DURATION_MS)
        postInvalidateOnAnimation()
        listener?.onPageChanged(target)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidateOnAnimation()
        }
    }

    // ------------------------------------------------------------------
    // Touch handling: page swipe vs. icon long-press drag vs. vertical gesture
    // ------------------------------------------------------------------

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) scroller.abortAnimation()
                downX = ev.x
                downY = ev.y
                downRawX = ev.rawX
                downRawY = ev.rawY
                lastX = ev.x
                isPagingDrag = false
                longPressFired = false
                pressedChild = findChildUnder(ev.x, ev.y)
                pressedItem = pressedChild?.let { child ->
                    (child.layoutParams as? WorkspaceLayoutParams)?.itemId?.let { itemLookup[it] }
                }
                obtainVelocityTracker().addMovement(ev)
                mainHandler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragController.isDragging) return true
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (!isPagingDrag && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                    isPagingDrag = true
                    mainHandler.removeCallbacks(longPressRunnable)
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
            }
        }
        return isPagingDrag || dragController.isDragging
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        obtainVelocityTracker().addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true

            MotionEvent.ACTION_MOVE -> {
                if (dragController.isDragging) {
                    dragController.updateDrag(event.rawX, event.rawY)
                    return true
                }
                if (isPagingDrag) {
                    val dx = (lastX - event.x).toInt()
                    scrollBy(dx, 0)
                    lastX = event.x
                    return true
                }
                val dy = event.y - downY
                val dx = event.x - downX
                if (!isPagingDrag && abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                    mainHandler.removeCallbacks(longPressRunnable)
                    if (dy < 0) onSwipeUp?.invoke() else onSwipeDown?.invoke()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(longPressRunnable)
                if (dragController.isDragging) {
                    dragController.endDrag()
                } else if (isPagingDrag) {
                    finishPagingGesture()
                } else if (!longPressFired) {
                    val child = pressedChild
                    val item = pressedItem
                    if (child != null && item != null && isPointInsideView(event.x, event.y, child)) {
                        listener?.onItemTapped(item)
                    } else if (pressedChild == null) {
                        val mapper = coordinateMapper
                        if (mapper != null) {
                            val cell = mapper.pixelToCell(event.x, event.y)
                            listener?.onWorkspaceLongPressed(currentPage, cell.cellX, cell.cellY)
                        }
                    }
                }
                isPagingDrag = false
                pressedChild = null
                pressedItem = null
                releaseVelocityTracker()
            }

            MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                if (dragController.isDragging) dragController.cancelDrag()
                isPagingDrag = false
                pressedChild = null
                pressedItem = null
                releaseVelocityTracker()
            }
        }
        return true
    }

    private fun finishPagingGesture() {
        velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
        val xVelocity = velocityTracker?.xVelocity ?: 0f

        val pageWidth = width.coerceAtLeast(1)
        val proposedPage = when {
            abs(xVelocity) > minFlingVelocity -> if (xVelocity < 0) currentPage + 1 else currentPage - 1
            else -> coordinateMapper?.nearestPage(scrollX, pageWidth) ?: currentPage
        }
        smoothScrollToPage(proposedPage)
    }

    private fun findChildUnder(x: Float, y: Float): View? {
        val page = pages.getOrNull(currentPage) ?: return null
        val mapper = coordinateMapper ?: return null
        val localX = x + scrollX - currentPage * width
        val cell = mapper.pixelToCell(localX, y)
        return page.findChildAtCell(cell.cellX, cell.cellY)
    }

    private fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val thisLoc = IntArray(2)
        getLocationOnScreen(thisLoc)
        val relLeft = loc[0] - thisLoc[0]
        val relTop = loc[1] - thisLoc[1]
        return x >= relLeft && x <= relLeft + view.width && y >= relTop && y <= relTop + view.height
    }

    private fun obtainVelocityTracker(): VelocityTracker =
        velocityTracker ?: VelocityTracker.obtain().also { velocityTracker = it }

    private fun releaseVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    // ------------------------------------------------------------------
    // WorkspaceDragController.Callbacks
    // ------------------------------------------------------------------

    override fun currentPageView(): WorkspacePageView? = pages.getOrNull(currentPage)

    override fun currentPageIndex(): Int = currentPage

    override fun coordinateMapper(): CoordinateMapper? = coordinateMapper

    override fun findItem(itemId: Long): WorkspaceItem? = itemLookup[itemId]

    override fun onDropped(itemId: Long, page: Int, cellX: Int, cellY: Int) {
        listener?.onItemMoved(itemId, page, cellX, cellY)
    }

    override fun onDroppedOnItem(draggedItemId: Long, targetItem: WorkspaceItem) {
        listener?.onItemDroppedOnItem(draggedItemId, targetItem)
    }

    override fun onDroppedOnFolder(draggedItemId: Long, folder: WorkspaceItem.Folder) {
        listener?.onItemDroppedOnFolder(draggedItemId, folder)
    }

    override fun requestEdgeAutoScroll(direction: Int) {
        edgeScrollDirection = direction
        mainHandler.removeCallbacks(edgeScrollRunnable)
        if (direction != 0) mainHandler.post(edgeScrollRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainHandler.removeCallbacksAndMessages(null)
        releaseVelocityTracker()
    }

    private companion object {
        const val PAGE_SCROLL_DURATION_MS = 260
        const val EDGE_SCROLL_INTERVAL_MS = 650L
    }
}

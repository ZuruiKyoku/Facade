package com.slygames.facade.features.launcher.workspace

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.slygames.facade.core.util.CoordinateMapper

/**
 * A single desktop page: a plain coordinate-based [ViewGroup] that measures
 * and positions each child directly from its [WorkspaceLayoutParams] via
 * [CoordinateMapper], with no intermediate list/recycler machinery. This is
 * what makes drag-and-drop reflow and widget resizing a single `layout()`
 * pass instead of an adapter notify + rebind cycle.
 */
class WorkspacePageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    /** Set by [WorkspaceGridView] whenever the grid config or measured page size changes. */
    var coordinateMapper: CoordinateMapper? = null
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mapper = coordinateMapper
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        if (mapper == null) return

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val lp = child.layoutParams as? WorkspaceLayoutParams ?: continue
            val rect = mapper.cellSpanToPixelRect(lp.cellX, lp.cellY, lp.spanX, lp.spanY)
            child.measure(
                MeasureSpec.makeMeasureSpec(rect.width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(rect.height, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val mapper = coordinateMapper ?: return
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val lp = child.layoutParams as? WorkspaceLayoutParams ?: continue
            val rect = mapper.cellSpanToPixelRect(lp.cellX, lp.cellY, lp.spanX, lp.spanY)
            child.layout(rect.left, rect.top, rect.right, rect.bottom)
        }
    }

    /** Re-lays out a single child in place (drag preview) without a full pass over every child. */
    fun relayoutChild(child: View) {
        val mapper = coordinateMapper ?: return
        val lp = child.layoutParams as? WorkspaceLayoutParams ?: return
        val rect = mapper.cellSpanToPixelRect(lp.cellX, lp.cellY, lp.spanX, lp.spanY)
        child.measure(
            MeasureSpec.makeMeasureSpec(rect.width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(rect.height, MeasureSpec.EXACTLY)
        )
        child.layout(rect.left, rect.top, rect.right, rect.bottom)
    }

    fun childAtItemId(itemId: Long): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if ((child.layoutParams as? WorkspaceLayoutParams)?.itemId == itemId) return child
        }
        return null
    }

    /** Hit-tests every child's cell span (not its pixel bounds) so drops snap to the nearest occupied item. */
    fun findChildAtCell(cellX: Int, cellY: Int): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as? WorkspaceLayoutParams ?: continue
            if (cellX in lp.cellX until (lp.cellX + lp.spanX) && cellY in lp.cellY until (lp.cellY + lp.spanY)) {
                return child
            }
        }
        return null
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        WorkspaceLayoutParams(context, attrs!!)

    override fun generateDefaultLayoutParams(): LayoutParams = WorkspaceLayoutParams(0, 0, 0, 1, 1)

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        if (p is WorkspaceLayoutParams) return p
        return WorkspaceLayoutParams(p ?: return generateDefaultLayoutParams())
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean = p is WorkspaceLayoutParams
}

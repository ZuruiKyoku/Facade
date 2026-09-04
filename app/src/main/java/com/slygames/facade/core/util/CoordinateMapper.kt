package com.slygames.facade.core.util

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Pure pixel <-> grid-cell math shared by the coordinate-based workspace
 * [android.view.ViewGroup] (drag-and-drop, page snapping, widget spans).
 * Kept free of any View/Context dependency so it is trivially unit-testable.
 *
 * @param columns number of cell columns per workspace page.
 * @param rows number of cell rows per workspace page.
 * @param cellWidthPx pixel width of a single grid cell, including gutter.
 * @param cellHeightPx pixel height of a single grid cell, including gutter.
 * @param originXPx left inset of the grid within its page (padding).
 * @param originYPx top inset of the grid within its page (padding).
 */
data class CoordinateMapper(
    val columns: Int,
    val rows: Int,
    val cellWidthPx: Int,
    val cellHeightPx: Int,
    val originXPx: Int = 0,
    val originYPx: Int = 0
) {

    init {
        require(columns > 0) { "columns must be > 0" }
        require(rows > 0) { "rows must be > 0" }
        require(cellWidthPx > 0) { "cellWidthPx must be > 0" }
        require(cellHeightPx > 0) { "cellHeightPx must be > 0" }
    }

    /** Converts an absolute pixel position (relative to the page) into the cell it falls in. */
    fun pixelToCell(xPx: Float, yPx: Float): CellCoordinate {
        val col = floor((xPx - originXPx) / cellWidthPx).toInt().coerceIn(0, columns - 1)
        val row = floor((yPx - originYPx) / cellHeightPx).toInt().coerceIn(0, rows - 1)
        return CellCoordinate(col, row)
    }

    /** Top-left pixel origin of cell ([cellX], [cellY]). */
    fun cellToPixel(cellX: Int, cellY: Int): PixelCoordinate {
        val x = originXPx + cellX * cellWidthPx
        val y = originYPx + cellY * cellHeightPx
        return PixelCoordinate(x, y)
    }

    /** Pixel bounds occupied by an item spanning [spanX] x [spanY] cells starting at ([cellX], [cellY]). */
    fun cellSpanToPixelRect(cellX: Int, cellY: Int, spanX: Int, spanY: Int): PixelRect {
        val (left, top) = cellToPixel(cellX, cellY)
        return PixelRect(
            left = left,
            top = top,
            right = left + spanX * cellWidthPx,
            bottom = top + spanY * cellHeightPx
        )
    }

    /** Whether a [spanX] x [spanY] item can be placed with its top-left at ([cellX], [cellY]) without going out of bounds. */
    fun fitsWithinGrid(cellX: Int, cellY: Int, spanX: Int, spanY: Int): Boolean =
        cellX >= 0 && cellY >= 0 && (cellX + spanX) <= columns && (cellY + spanY) <= rows

    /**
     * Given the horizontal scroll offset of a paginated workspace and its
     * page width, returns the page index closest to settling on (used for
     * fling/snap decisions during drag-and-drop paging).
     */
    fun nearestPage(scrollXPx: Int, pageWidthPx: Int): Int {
        if (pageWidthPx <= 0) return 0
        return (scrollXPx.toFloat() / pageWidthPx).roundToInt().coerceAtLeast(0)
    }
}

data class CellCoordinate(val cellX: Int, val cellY: Int)
data class PixelCoordinate(val x: Int, val y: Int)
data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

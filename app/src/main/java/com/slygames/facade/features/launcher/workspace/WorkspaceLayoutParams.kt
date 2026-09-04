package com.slygames.facade.features.launcher.workspace

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

/**
 * Per-child layout params for [WorkspacePageView]: a grid-cell position plus
 * span, and the persisted item id so the drag controller and view binder can
 * round-trip a touched child back to its [com.slygames.facade.data.model.WorkspaceItem].
 */
class WorkspaceLayoutParams(width: Int, height: Int) : ViewGroup.LayoutParams(width, height) {

    var itemId: Long = -1L
    var cellX: Int = 0
    var cellY: Int = 0
    var spanX: Int = 1
    var spanY: Int = 1

    constructor(source: ViewGroup.LayoutParams) : this(source.width, source.height)

    constructor(context: Context, attrs: AttributeSet) : this(
        WRAP_CONTENT,
        WRAP_CONTENT
    )

    constructor(itemId: Long, cellX: Int, cellY: Int, spanX: Int, spanY: Int) : this(
        MATCH_PARENT,
        MATCH_PARENT
    ) {
        this.itemId = itemId
        this.cellX = cellX
        this.cellY = cellY
        this.spanX = spanX
        this.spanY = spanY
    }
}

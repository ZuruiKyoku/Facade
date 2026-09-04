package com.slygames.facade.features.launcher.workspace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.slygames.facade.core.designsystem.WorkspaceIconLabelStyle
import com.slygames.facade.data.model.WorkspaceItem

/**
 * A folder's closed-state icon: a rounded "stack" backdrop with up to the
 * first four contained items' icons arranged in a 2x2 mini grid, plus the
 * folder name beneath it - matching the Nova/AOSP folder preview
 * convention. Drawn directly on [Canvas] for the same reason as
 * [WorkspaceIconView]: no extra View hierarchy per workspace slot.
 */
class FolderPreviewView @JvmOverloads constructor(
    context: Context,
    private var folder: WorkspaceItem.Folder
) : View(context) {

    private val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = WorkspaceIconLabelStyle.fontSize.value * resources.displayMetrics.scaledDensity
        setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
    }
    private val backdropRect = RectF()
    private val miniIconBounds = Rect()

    var onTap: (() -> Unit)? = null

    fun updateFolder(folder: WorkspaceItem.Folder) {
        this.folder = folder
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val backdropSize = minOf(width, height) * 0.68f
        val left = (width - backdropSize) / 2f
        val top = (height - backdropSize) / 2f - height * 0.06f
        backdropRect.set(left, top, left + backdropSize, top + backdropSize)
        val cornerRadius = backdropSize * 0.28f
        canvas.drawRoundRect(backdropRect, cornerRadius, cornerRadius, backdropPaint)

        val previewItems = folder.items.take(MAX_PREVIEW_ICONS)
        val cell = backdropSize / 2f
        val iconPadding = backdropSize * 0.09f
        val miniIconSize = (cell - iconPadding * 1.5f).toInt().coerceAtLeast(1)

        previewItems.forEachIndexed { index, item ->
            val icon = (item as? WorkspaceItem.App)?.appItem?.icon ?: return@forEachIndexed
            val col = index % 2
            val row = index / 2
            val cellLeft = (backdropRect.left + iconPadding + col * cell).toInt()
            val cellTop = (backdropRect.top + iconPadding + row * cell).toInt()
            miniIconBounds.set(cellLeft, cellTop, cellLeft + miniIconSize, cellTop + miniIconSize)
            icon.bounds = miniIconBounds
            icon.draw(canvas)
        }

        if (folder.name.isNotEmpty()) {
            canvas.drawText(folder.name, width / 2f, backdropRect.bottom + labelPaint.textSize + 6f, labelPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        onTap?.invoke()
        return true
    }

    private companion object {
        const val MAX_PREVIEW_ICONS = 4
    }
}

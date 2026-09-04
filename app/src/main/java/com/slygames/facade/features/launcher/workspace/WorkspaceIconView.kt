package com.slygames.facade.features.launcher.workspace

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.withTranslation
import com.slygames.facade.core.designsystem.WorkspaceIconLabelStyle

/**
 * Draws a single app/shortcut icon + label directly on [Canvas] instead of
 * inflating an ImageView+TextView pair. On a grid that can hold 40+ icons
 * per page across several pages, skipping the extra View hierarchy and
 * measure/layout passes per icon is what keeps drag-and-drop reflow at
 * 60fps.
 */
class WorkspaceIconView @JvmOverloads constructor(
    context: Context,
    private var icon: Drawable?,
    private var label: String,
    var iconScale: Float = 1f,
    var showLabel: Boolean = true
) : View(context) {

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = WorkspaceIconLabelStyle.fontSize.value * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
    }

    private val iconBounds = Rect()
    private var pressScale = 1f
    private var pressAnimator: ValueAnimator? = null

    var onTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    private val longPressRunnable = Runnable {
        if (isPressed) {
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            onLongPress?.invoke()
        }
    }

    fun updateContent(icon: Drawable?, label: String) {
        this.icon = icon
        this.label = label
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val iconSizePx = (minOf(width, height) * 0.62f * iconScale).toInt().coerceAtLeast(1)
        val left = (width - iconSizePx) / 2
        val top = (height - iconSizePx) / 2 - if (showLabel) (height * 0.06f).toInt() else 0
        iconBounds.set(left, top, left + iconSizePx, top + iconSizePx)

        canvas.withTranslation(width / 2f, height / 2f) {
            scale(pressScale, pressScale, 0f, 0f)
            translate(-width / 2f, -height / 2f)
            icon?.let {
                it.bounds = iconBounds
                it.draw(this)
            }
            if (showLabel && label.isNotEmpty()) {
                drawText(
                    label,
                    width / 2f,
                    (iconBounds.bottom + labelPaint.textSize + 6f),
                    labelPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                animatePress(0.9f)
                postDelayed(longPressRunnable, android.view.ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (!pointInView(event.x, event.y)) {
                    cancelPress()
                }
            }
            MotionEvent.ACTION_UP -> {
                val wasPressed = isPressed
                cancelPress()
                if (wasPressed && pointInView(event.x, event.y)) {
                    performClick()
                }
            }
            MotionEvent.ACTION_CANCEL -> cancelPress()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        onTap?.invoke()
        return true
    }

    private fun cancelPress() {
        isPressed = false
        removeCallbacks(longPressRunnable)
        animatePress(1f)
    }

    private fun animatePress(target: Float) {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(pressScale, target).apply {
            duration = 120
            interpolator = OvershootInterpolator()
            addUpdateListener {
                pressScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun pointInView(x: Float, y: Float): Boolean = x >= 0 && y >= 0 && x <= width && y <= height

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(longPressRunnable)
        pressAnimator?.cancel()
    }
}

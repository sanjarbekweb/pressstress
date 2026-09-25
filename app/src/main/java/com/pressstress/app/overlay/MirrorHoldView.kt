package com.pressstress.app.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

class MirrorHoldView(context: Context) : View(context) {
    var holdDurationMs: Long = 10_000L
    var onHoldStateChanged: (Boolean) -> Unit = {}
    var onHoldCompleted: () -> Unit = {}
    var onVerticalDrag: (deltaPixels: Int, finished: Boolean) -> Unit = { _, _ -> }

    private val density = resources.displayMetrics.density
    private val buttonSize = (64f * density).toInt()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
        textSize = 11f * density
        letterSpacing = 0.06f
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(105, 225, 238, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
        strokeCap = Paint.Cap.ROUND
    }

    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var downRawY = 0f
    private var lastRawY = 0f
    private var dragging = false
    private var completedDuringGesture = false

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Hold to interrupt the urge. Drag vertically to reposition."
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(buttonSize, buttonSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.46f

        fillPaint.shader = RadialGradient(
            cx - radius * 0.28f,
            cy - radius * 0.34f,
            radius * 1.35f,
            intArrayOf(
                Color.argb(225, 112, 143, 255),
                Color.argb(210, 61, 77, 202),
                Color.argb(226, 19, 27, 79),
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, fillPaint)

        ringPaint.shader = null
        ringPaint.strokeWidth = 1.5f * density
        ringPaint.color = Color.argb(180, 160, 185, 255)
        canvas.drawCircle(cx, cy, radius - ringPaint.strokeWidth, ringPaint)

        val arcRect = RectF(
            cx - radius * 0.72f,
            cy - radius * 0.72f,
            cx + radius * 0.72f,
            cy + radius * 0.72f,
        )
        canvas.drawArc(arcRect, 205f, 98f, false, highlightPaint)

        if (progress > 0f) {
            val progressRect = RectF(
                cx - radius - 2f * density,
                cy - radius - 2f * density,
                cx + radius + 2f * density,
                cy + radius + 2f * density,
            )
            ringPaint.strokeWidth = 3f * density
            ringPaint.color = Color.argb(245, 205, 220, 255)
            canvas.drawArc(progressRect, -90f, 360f * progress, false, ringPaint)
        }

        val baseline = cy - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText("HOLD", cx, baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawY = event.rawY
                lastRawY = event.rawY
                dragging = false
                completedDuringGesture = false
                startHold()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging && abs(event.rawY - downRawY) > touchSlop) {
                    dragging = true
                    cancelHold()
                }
                if (dragging) {
                    val delta = (event.rawY - lastRawY).toInt()
                    lastRawY = event.rawY
                    if (delta != 0) onVerticalDrag(delta, false)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    onVerticalDrag(0, true)
                } else if (!completedDuringGesture) {
                    cancelHold()
                    performClick()
                }
                dragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelHold()
                if (dragging) onVerticalDrag(0, true)
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        return true
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private fun startHold() {
        animator?.cancel()
        progress = 0f
        onHoldStateChanged(true)
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = holdDurationMs.coerceAtLeast(1_000L)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled && progress >= 0.999f) {
                        completedDuringGesture = true
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                        onHoldStateChanged(false)
                        onHoldCompleted()
                    }
                }
            })
            start()
        }
    }

    private fun cancelHold() {
        animator?.cancel()
        animator = null
        progress = 0f
        invalidate()
        onHoldStateChanged(false)
    }
}

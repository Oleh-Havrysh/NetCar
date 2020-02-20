package com.hakito.netcar.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hakito.netcar.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class RelativeTouchView(context: Context?, val attrs: AttributeSet?) : View(context, attrs) {

    private var basePoint: Vector? = null
    private var touchPoint: Vector? = null
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val axis: Axis

    private val workRect = RectF()

    val progress: Vector?
        get() {
            val workZoneRadius = getWorkZoneRadius()
            return touchPoint
                ?.minus(basePoint ?: return null)
                ?.clamp(-workZoneRadius, workZoneRadius)
                ?.divide(workZoneRadius)
                ?.invertY()
        }

    fun resetTouch() {
        basePoint = null
        touchPoint = null
    }

    init {
        if (isInEditMode) {
            basePoint = Vector(500f, 500f)
            touchPoint = Vector(300f, 200f)
            axis = Axis.HORIZONTAL
        } else {
            axis = getAxisFromAttributes()
        }
    }

    private fun getAxisFromAttributes(): Axis {
        attrs ?: return Axis.VERTICAL

        context.theme.obtainStyledAttributes(attrs, R.styleable.RelativeTouchView, 0, 0)
            .apply {
                try {
                    return when (getInteger(R.styleable.RelativeTouchView_axis, 0)) {
                        1 -> Axis.HORIZONTAL
                        2 -> Axis.VERTICAL
                        else -> Axis.BIDIRECTIONAL
                    }
                } finally {
                    recycle()
                }
            }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val point = Vector(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                basePoint = point
                touchPoint = point
            }
            MotionEvent.ACTION_MOVE -> {
                touchPoint = point
            }
            MotionEvent.ACTION_UP -> {
                basePoint = null
                touchPoint = null
            }
        }
        invalidate()
        return true
    }

    private fun getWorkZoneRadius() = min(width, height) * 0.45f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val workZoneRadius = getWorkZoneRadius()
        if (basePoint != null) {
            when (axis) {
                Axis.HORIZONTAL -> drawVawes(canvas, 0f, 5)
                Axis.VERTICAL -> drawVawes(canvas, 90f, 5)
                Axis.BIDIRECTIONAL -> canvas.drawCircle(
                    basePoint!!.x,
                    basePoint!!.y,
                    workZoneRadius,
                    paint
                )
            }
        }
        if (basePoint != null && touchPoint != null) {
            canvas.drawLine(basePoint!!.x, basePoint!!.y, touchPoint!!.x, touchPoint!!.y, paint)
            canvas.drawCircle(basePoint!!.x, basePoint!!.y, 10f, paint)
            canvas.drawCircle(touchPoint!!.x, touchPoint!!.y, 10f, paint)

            val radius = when (axis) {
                Axis.HORIZONTAL -> abs(progress!!.times(workZoneRadius).x)
                Axis.VERTICAL -> abs(progress!!.times(workZoneRadius).y)
                Axis.BIDIRECTIONAL -> progress!!.times(workZoneRadius).len()
            }
            canvas.drawCircle(
                basePoint!!.x,
                basePoint!!.y,
                radius,
                paint
            )
        }
    }

    private fun drawVawes(canvas: Canvas, startAngle: Float, vawesCount: Int) {
        val workZoneRadius = getWorkZoneRadius()
        workRect.set(
            basePoint!!.x - workZoneRadius,
            basePoint!!.y - workZoneRadius,
            basePoint!!.x + workZoneRadius,
            basePoint!!.y + workZoneRadius
        )
        for (i in 0..vawesCount) {
            canvas.drawArc(workRect, startAngle - 15f, 30f, false, paint)
            canvas.drawArc(workRect, startAngle + 165f, 30f, false, paint)
            workRect.inset(workZoneRadius / vawesCount, workZoneRadius / vawesCount)
        }
    }

    data class Vector(val x: Float, val y: Float) {

        fun divide(v: Float) = Vector(x / v, y / v)

        fun times(v: Float) = Vector(x * v, y * v)

        fun center(v: Vector) = Vector((x + v.x) / 2, (y + v.y) / 2)

        fun clamp(lowerBound: Float, upperBound: Float) =
            Vector(x.coerceIn(lowerBound, upperBound), y.coerceIn(lowerBound, upperBound))

        fun len(v: Vector) = sqrt((x - v.x).pow(2) + (y - v.y).pow(2))

        fun len() = len(ZERO)

        fun invertY() = Vector(x, -y)

        operator fun minus(v: Vector) = Vector(x - v.x, y - v.y)

        companion object {
            val ZERO = Vector(0f, 0f)
        }
    }

    enum class Axis {
        HORIZONTAL, VERTICAL, BIDIRECTIONAL
    }
}
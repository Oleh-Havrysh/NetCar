package com.hakito.netcar.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class RelativeTouchView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var basePoint: Vector? = null
    private var touchPoint: Vector? = null
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val progress: Vector?
        get() {
            val workZoneRadius = getWorkZoneRadius()
            return touchPoint
                ?.minus(basePoint ?: return null)
                ?.clamp(-workZoneRadius, workZoneRadius)
                ?.divide(workZoneRadius)
                ?.invertY()
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
            canvas.drawCircle(basePoint!!.x, basePoint!!.y, workZoneRadius, paint)
        }
        if (basePoint != null && touchPoint != null) {
            canvas.drawLine(basePoint!!.x, basePoint!!.y, touchPoint!!.x, touchPoint!!.y, paint)
            canvas.drawCircle(basePoint!!.x, basePoint!!.y, 10f, paint)
            canvas.drawCircle(touchPoint!!.x, touchPoint!!.y, 10f, paint)

            canvas.drawCircle(basePoint!!.x, basePoint!!.y, progress!!.times(workZoneRadius).len(), paint)
        }
    }

    data class Vector(val x: Float, val y: Float) {

        fun divide(v: Float) = Vector(x / v, y / v)

        fun times(v: Float) = Vector(x * v, y * v)

        fun center(v: Vector) = Vector((x + v.x) / 2, (y + v.y) / 2)

        fun clamp(lowerBound: Float, upperBound: Float) =
            Vector(clamp(x, lowerBound, upperBound), clamp(y, lowerBound, upperBound))

        fun len(v: Vector) = sqrt((x - v.x).pow(2) + (y - v.y).pow(2))

        fun len() = len(ZERO)

        fun invertY() = Vector(x, -y)

        operator fun minus(v: Vector) = Vector(x - v.x, y - v.y)

        companion object {
            val ZERO = Vector(0f, 0f)

            private fun clamp(value: Float, lowerBound: Float, upperBound: Float) =
                max(lowerBound, min(upperBound, value))
        }
    }
}
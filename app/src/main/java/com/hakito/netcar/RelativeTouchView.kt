package com.hakito.netcar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class RelativeTouchView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var basePoint: Vector? = null
    private var touchPoint: Vector? = null
    private val paint = Paint().apply {
        this.style = Paint.Style.STROKE
    }

    val progress: Vector?
        get() {
            return touchPoint?.minus(basePoint ?: return null)
        }

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (basePoint != null) {
            canvas.drawCircle(basePoint!!.x, basePoint!!.y, 200f, paint)
        }
        if (basePoint != null && touchPoint != null) {
            canvas.drawLine(basePoint!!.x, basePoint!!.y, touchPoint!!.x, touchPoint!!.y, paint)
            canvas.drawCircle(basePoint!!.x, basePoint!!.y, 10f, paint)
            canvas.drawCircle(touchPoint!!.x, touchPoint!!.y, 10f, paint)

            canvas.drawCircle(basePoint!!.x, basePoint!!.y, min(progress!!.len(), 200f), paint)

            val center = basePoint!!.center(touchPoint!!)
            canvas.drawText("${progress!!.len().toInt()}", center.x, center.y, paint)
        }
    }

    data class Vector(val x: Float, val y: Float) {
        fun center(v: Vector) = Vector((x + v.x) / 2, (y + v.y) / 2)
        fun len(v: Vector) = sqrt((x - v.x).pow(2) + (y - v.y).pow(2))
        fun len() = len(ZERO)
        operator fun minus(v: Vector) = Vector(x - v.x, y - v.y)

        companion object {
            val ZERO = Vector(0f, 0f)
        }
    }
}
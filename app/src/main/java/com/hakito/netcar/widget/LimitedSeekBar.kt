package com.hakito.netcar.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import kotlin.math.roundToInt

class LimitedSeekBar
constructor(context: Context, attrs: AttributeSet) :
    AppCompatSeekBar(context, attrs) {

    var percentMinLimit: Float? = null
        set(value) {
            field = value
            invalidate()
        }

    var percentMaxLimit: Float? = null
        set(value) {
            field = value
            invalidate()
        }

    var percentProgress: Float
        get() = progress.toFloat() / max
        set(value) {
            progress = (value * max).roundToInt()
        }

    var onProgressChangedListener: ((Float) -> Unit)? = null

    private val paint = Paint().apply {
        textSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9f, resources.displayMetrics)
    }

    init {
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    onProgressChanged(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun onProgressChanged(newProgress: Int) {
        val newPercentProgress = newProgress.toFloat() / max
        val minLimit = percentMinLimit ?: 0f
        val maxLimit = percentMaxLimit ?: 1f
        percentProgress = if (newPercentProgress < minLimit) {
            minLimit
        } else if (newPercentProgress > maxLimit) {
            maxLimit
        } else {
            newPercentProgress
        }
        onProgressChangedListener?.invoke(percentProgress)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText(String.format("%1$.3f", percentProgress), width / 2f, 20f, paint)
    }
}
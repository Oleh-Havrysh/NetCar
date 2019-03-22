package com.hakito.netcar.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

class LimitedSeekBar
constructor(context: Context, attrs: AttributeSet) :
    AppCompatSeekBar(context, attrs) {

    var minLimit: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    var maxLimit: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    var onProgressChangedListener: ((Int) -> Unit)? = null

    private val paint = Paint().apply {
        textSize = 20f
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
        val minLimit = minLimit ?: 0
        val maxLimit = maxLimit ?: max
        progress = if (newProgress < minLimit) {
            minLimit
        } else if (newProgress > maxLimit) {
            maxLimit
        } else {
            newProgress
        }
        onProgressChangedListener?.invoke(progress)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText(minLimit.toString(), 0f, 20f, paint)
        canvas.drawText(progress.toString(), width / 2f, 20f, paint)
        canvas.drawText(maxLimit.toString(), width - 60f, 20f, paint)
    }
}
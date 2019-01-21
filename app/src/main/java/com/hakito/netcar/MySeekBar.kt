package com.hakito.netcar

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

class MySeekBar
constructor(context: Context, attrs: AttributeSet) :
    AppCompatSeekBar(context, attrs) {

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val handledBySuper = super.onTouchEvent(event)
        if (event?.action == MotionEvent.ACTION_UP) {
            progress = 500
        }
        return handledBySuper
    }
}
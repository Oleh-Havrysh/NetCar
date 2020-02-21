package com.hakito.netcar.controls

import com.hakito.netcar.R
import com.hakito.netcar.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_controls_separate.*

class SeparateControlsFragment : BaseFragment(R.layout.fragment_controls_separate),
    ControlsInterface {

    override fun getThrottle() = throttleTouchView?.progress?.y ?: 0f

    override fun getSteer() = steerTouchView?.progress?.x ?: 0f

    override fun resetValues() {
        listOf(throttleTouchView, steerTouchView).forEach { it.resetTouch() }
    }

    override fun setColor(color: Int) {
        listOf(throttleTouchView, steerTouchView).forEach { it.linesColor = color }
    }
}
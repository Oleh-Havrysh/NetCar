package com.hakito.netcar.controls

import com.hakito.netcar.R
import com.hakito.netcar.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_controls_single.*

class SingleControlsFragment : BaseFragment(R.layout.fragment_controls_single),
    ControlsInterface {

    override fun getThrottle() = touchView?.progress?.y ?: 0f

    override fun getSteer() = touchView?.progress?.x ?: 0f

    override fun resetValues() {
        touchView.resetTouch()
    }

    override fun setColor(color: Int) {
        touchView.linesColor = color
    }
}
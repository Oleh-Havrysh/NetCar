package com.hakito.netcar.controls

import androidx.annotation.ColorInt

interface ControlsInterface {

    fun getThrottle(): Float

    fun getSteer(): Float

    fun resetValues()

    fun setColor(@ColorInt color: Int)
}
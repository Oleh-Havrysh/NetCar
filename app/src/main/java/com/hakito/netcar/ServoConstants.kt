package com.hakito.netcar

class ServoConstants {
    companion object {
        const val CENTER = 90
        const val AMPLITUDE = 90
        const val START = CENTER - AMPLITUDE
        const val END = CENTER + AMPLITUDE
    }
}

fun Float.toServoValue() = ServoConstants.START + this * ServoConstants.AMPLITUDE * 2
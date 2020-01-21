package com.hakito.netcar

class ServoConstants {
    companion object {
        const val CENTER = 1500
        const val AMPLITUDE = 500
        const val START = CENTER - AMPLITUDE
        const val END = CENTER + AMPLITUDE
    }
}

fun Float.toServoValue() = ServoConstants.START + this * ServoConstants.AMPLITUDE * 2
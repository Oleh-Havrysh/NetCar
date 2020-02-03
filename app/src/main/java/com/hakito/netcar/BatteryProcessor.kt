package com.hakito.netcar

import android.graphics.Color
import kotlin.math.roundToInt

class BatteryProcessor(private val preferences: ControlPreferences) {

    fun processRawVoltage(rawVoltage: Int): BatteryUi {
        val voltage = rawVoltage * preferences.voltageMultiplier
        val percents = ((voltage / CELLS_COUNT - MIN_VOLTAGE) / (MAX_VOLTAGE - MIN_VOLTAGE) * 100)
            .roundToInt()
            .coerceIn(0, 100)
        val voltageString = String.format("%d%%(%.2fV)", percents, voltage)

        val color = when {
            percents < 25 -> Color.RED
            percents > 75 -> Color.GREEN
            else -> Color.BLACK
        }

        return BatteryUi(voltageString, color)
    }

    class BatteryUi(val text: String, val color: Int)

    companion object {
        private const val MIN_VOLTAGE = 3.5f
        private const val MAX_VOLTAGE = 4.2f

        private const val CELLS_COUNT = 2
    }
}
package com.hakito.netcar

import android.graphics.Color
import kotlin.math.roundToInt

class BatteryProcessor(
    private val preferences: ControlPreferences
) {
    var onBatteryLow: (() -> Unit)? = null

    private var lowBatteryCount = 0

    fun processRawVoltage(rawVoltage: Int): BatteryUi {
        val voltage = rawVoltage * preferences.voltageMultiplier
        val cellsCount = (voltage / NOMINAL_VOLTAGE).roundToInt()
        if (cellsCount == 0) return BatteryUi("Batt error", Color.RED)
        val percents = ((voltage / cellsCount - MIN_VOLTAGE) / (MAX_VOLTAGE - MIN_VOLTAGE) * 100)
            .roundToInt()
            .coerceIn(0, 100)
        val voltageString = String.format("%d%%(%.2fV)", percents, voltage)

        val color = when {
            percents < 25 -> Color.RED
            else -> Color.BLACK
        }

        if (percents < 20) lowBatteryCount++ else lowBatteryCount = 0

        if (lowBatteryCount > 1000) {
            onBatteryLow?.invoke()
            lowBatteryCount = 0
        }

        return BatteryUi(voltageString, color)
    }

    class BatteryUi(val text: String, val color: Int)

    companion object {
        private const val MIN_VOLTAGE = 3.5f
        private const val MAX_VOLTAGE = 4.2f
        private const val NOMINAL_VOLTAGE = 3.7f
    }
}
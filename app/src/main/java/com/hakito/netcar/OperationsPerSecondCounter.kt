package com.hakito.netcar

import kotlin.math.roundToInt

class OperationsPerSecondCounter(private val probeSize: Int) {

    private val timestamps = mutableListOf<Long>()

    fun onPerformed() {
        timestamps.add(System.currentTimeMillis())
        while (timestamps.size > probeSize) timestamps.removeAt(0)
    }

    fun getRps() =
        timestamps
            .windowed(size = 2, step = 2, partialWindows = false) { it[1] - it[0] }
            .average()
            .let { 1000 / it }
            .let { if (it.isNaN()) 0 else it.roundToInt() }
}
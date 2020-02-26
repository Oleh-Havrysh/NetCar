package com.hakito.netcar.util

import com.hakito.netcar.OperationsPerSecondCounter

class StatisticsController(private val errorsController: ErrorsController) {

    private var maxSpeed = 0
    private var speed: Int? = 0

    private val controlCounter = OperationsPerSecondCounter(50)

    private var lastSteerValue = 0
    private var lastThrottleValue = 0

    fun handleInputParams(steerValue: Int, throttleValue: Int) {
        lastSteerValue = steerValue
        lastThrottleValue = throttleValue
    }

    fun onRequestPerformed() {
        controlCounter.onPerformed()
    }

    fun handleRpm(frontLeftRpm: Int?) {
        speed = frontLeftRpm?.let { getSpeed(it) }
            ?.also { maxSpeed = it.coerceAtLeast(maxSpeed) }
    }

    private fun getSpeed(rpm: Int): Int {
        val wheelLength = 7f * Math.PI
        return ((rpm * 60 * wheelLength) / (100 * 1000)).toInt()
    }

    fun getText() = """
steer: $lastSteerValue, throttle: $lastThrottleValue
speed = $speed kmh
maxSpeed = $maxSpeed kmh
control RPS = ${controlCounter.getRps()}
${errorsController.getText()}"""
}
package com.hakito.netcar

import com.hakito.netcar.sender.Sensors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class StabilizationController(private val preferences: ControlPreferences) {

    var isWheelSlipping = false
        private set

    var isSensorsCalibrated = false
        private set

    private var cruiseThrottle = 0f

    private var sensors = Sensors(0, 0, 0)

    var targetCruiseRpm = 0
        set(value) {
            field = value
            if (value == 0) cruiseThrottle = 0f
        }

    private var lastStabilizationTime = 0L

    fun onSensorsReceived(sensors: Sensors) {
        this.sensors = sensors
        if (!isSensorsCalibrated) {
            isSensorsCalibrated = sensors.frontLeftRpm != 0 && sensors.rearRpm != 0
        }

        val rearAlmostStopped = isWheelAlmostStopped(sensors.rearRpm)
        val frontAlmostStopped = isWheelAlmostStopped(sensors.frontLeftRpm)
        isWheelSlipping = when {
            rearAlmostStopped && frontAlmostStopped -> false
            rearAlmostStopped != frontAlmostStopped -> true
            else -> abs(sensors.frontLeftRpm.toFloat() / sensors.rearRpm - 1) > 0.3
        }
    }

    private fun isWheelAlmostStopped(rpm: Int) = rpm < 30

    private fun isCruiseEnabled() = targetCruiseRpm != 0

    fun isStabilizationWarning() = !isSensorsCalibrated || isWheelSlipping

    fun calcThrottle(userThrottle: Float) =
        when {
            !isSensorsCalibrated -> userThrottle
            isCruiseEnabled() -> {
                val targetRpm = if (preferences.preventSlipping)
                    targetCruiseRpm.constraint(0, sensors.frontLeftRpm + FRONT_WHEEL_DIFF)
                else targetCruiseRpm
                adjustAndGetCruiseThrottle(targetRpm, preferences.throttleMax)
            }
            preferences.preventSlipping -> {
                adjustAndGetCruiseThrottle(sensors.frontLeftRpm + FRONT_WHEEL_DIFF, userThrottle)
            }
            else -> userThrottle
        }

    private fun adjustAndGetCruiseThrottle(targetRpm: Int, maxThrottle: Float): Float {
        val rpmDiff = ((targetRpm - sensors.rearRpm) / 2000f)
            .constraint(-1f, 1f)
        return (cruiseThrottle + rpmDiff * preferences.cruiseGain * getAndSetDeltaTimeSeconds())
            .constraint(min(maxThrottle, preferences.throttleDeadzone), maxThrottle)
            .also { cruiseThrottle = it }
    }

    private fun Float.constraint(minConstraint: Float, maxConstraint: Float) =
        max(minConstraint, min(maxConstraint, this))

    private fun Int.constraint(minConstraint: Int, maxConstraint: Int) =
        max(minConstraint, min(maxConstraint, this))

    private fun getAndSetDeltaTimeSeconds(): Float {
        val current = System.currentTimeMillis()
        val delta = min(current - lastStabilizationTime, 100)
        lastStabilizationTime = current
        return delta / 1000f
    }

    companion object {
        private const val FRONT_WHEEL_DIFF = 100
    }
}
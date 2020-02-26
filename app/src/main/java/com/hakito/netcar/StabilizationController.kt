package com.hakito.netcar

import com.hakito.netcar.sender.Sensors
import kotlin.math.abs
import kotlin.math.min

class StabilizationController(private val preferences: ControlPreferences) {

    var isWheelSlipping = false
        private set

    var isSensorsCalibrated = false
        private set

    private var cruiseThrottle = 0f

    private var sensors = Sensors(0, 0, 0, 0)

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

    fun calcThrottle(userThrottle: Float): Float =
        when {
            userThrottle <= 0 -> userThrottle * 0.5f
            !isSensorsCalibrated
                    && (isCruiseEnabled()
                    || preferences.preventSlipping
                    || preferences.throttleControlsSpeed) -> userThrottle.coerceAtMost(0.15f)
            isCruiseEnabled() -> {
                val targetRpm = if (preferences.preventSlipping)
                    targetCruiseRpm.coerceIn(0, sensors.frontLeftRpm + getMaxFrontRearSpeedDiff())
                else targetCruiseRpm
                adjustAndGetCruiseThrottle(targetRpm, preferences.throttleMax)
            }
            preferences.throttleControlsSpeed -> {
                val targetRpm =
                    (userThrottle * 2000).toInt()
                        .coerceIn(0, sensors.frontLeftRpm + getMaxFrontRearSpeedDiff())
                adjustAndGetCruiseThrottle(targetRpm, preferences.throttleMax)
            }
            preferences.preventSlipping -> {
                adjustAndGetCruiseThrottle(
                    sensors.frontLeftRpm + getMaxFrontRearSpeedDiffWithTrottle(userThrottle),
                    userThrottle
                )
            }
            else -> userThrottle
        }

    private fun getMaxFrontRearSpeedDiff() = (preferences.cruiseSpeedDiff * 1000).toInt()

    private fun getMaxFrontRearSpeedDiffWithTrottle(throttle: Float) =
        if (preferences.cruiseDiffDependsOnThrottle) (getMaxFrontRearSpeedDiff() * throttle).toInt()
        else getMaxFrontRearSpeedDiff()

    private fun adjustAndGetCruiseThrottle(targetRpm: Int, maxThrottle: Float): Float {
        val rpmDiff = ((targetRpm - sensors.rearRpm) / 1000f)
            .coerceIn(-1f, 1f)
        return (cruiseThrottle + rpmDiff * preferences.cruiseGain * getAndSetDeltaTimeSeconds())
            .coerceIn(min(maxThrottle, preferences.throttleDeadzoneCompensation), maxThrottle)
            .also { cruiseThrottle = it }
    }

    private fun Float.interpolateProgress(minValue: Float, maxValue: Float) =
        minValue + this * (maxValue - minValue)

    fun calcSteer(userSteer: Float): Float {
        val relativeRpm = (sensors.frontLeftRpm / 2000f).coerceIn(0f, 1f)
        return userSteer * relativeRpm.interpolateProgress(1f, preferences.speedDependantSteerLimit)
    }

    private fun getAndSetDeltaTimeSeconds(): Float {
        val current = System.currentTimeMillis()
        val delta = min(current - lastStabilizationTime, 100)
        lastStabilizationTime = current
        return delta / 1000f
    }
}
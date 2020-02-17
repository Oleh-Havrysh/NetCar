package com.hakito.netcar

import android.content.Context
import androidx.core.content.edit
import com.hakito.netcar.entity.CarConfig
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ControlPreferences(context: Context) {

    private val preferences =
        context.getSharedPreferences("CONTROL_PREFERENCES", Context.MODE_PRIVATE)

    var carConfig: CarConfig
        set(value) {
            steerMin = value.steerMin
            steerCenter = value.steerCenter
            steerMax = value.steerMax
            invertSteer = value.invertSteer
            throttleMax = value.throttleMax
            voltageMultiplier = value.voltageMultiplier
            throttleDeadzoneCompensation = value.throttleDeadzoneCompensation
            cruiseGain = value.cruiseGain
            preventSlipping = value.preventSlipping
            cruiseSpeedDiff = value.cruiseSpeedDiff
            cruiseDiffDependsOnThrottle = value.cruiseDiffDependsOnThrottle
            speedDependantSteerLimit = value.speedDependantSteerLimit
        }
        get() = CarConfig(
            steerMin = steerMin,
            steerCenter = steerCenter,
            steerMax = steerMax,
            invertSteer = invertSteer,
            throttleMax = throttleMax,
            voltageMultiplier = voltageMultiplier,
            throttleDeadzoneCompensation = throttleDeadzoneCompensation,
            cruiseGain = cruiseGain,
            preventSlipping = preventSlipping,
            cruiseSpeedDiff = cruiseSpeedDiff,
            cruiseDiffDependsOnThrottle = cruiseDiffDependsOnThrottle,
            speedDependantSteerLimit = speedDependantSteerLimit
        )

    var steerMin by FloatDelegate("STEER_MIN")

    var steerCenter by FloatDelegate("STEER_CENTER", 0.5f)

    var steerMax by FloatDelegate("STEER_MAX", 1f)

    var invertSteer by BooleanDelegate("INVERT_STEER")

    var throttleMax by FloatDelegate("THROTTLE_MAX", 1f)

    var voltageMultiplier by FloatDelegate("VOLTAGE_MULTIPLIER", 1f)

    var requestTimeout by IntDelegate("REQUEST_TIMEOUT", 500)

    var cameraEnabled by BooleanDelegate("CAMERA_ENABLED")

    var cameraRotation by IntDelegate("CAMERA_ROTATION")

    var throttleDeadzoneCompensation by FloatDelegate("THROTTLE_DEADZONE")

    var cruiseGain by FloatDelegate("CRUISE_GAIN")

    var preventSlipping by BooleanDelegate("PREVENT_SLIPPING")

    var cruiseSpeedDiff by FloatDelegate("CRUISE_SPEED_DIFF")

    var cruiseDiffDependsOnThrottle by BooleanDelegate("CRUISE_DIFF_DEPENDS_ON_THROTTLE")

    var speedDependantSteerLimit by FloatDelegate("SPEED_DEPENDANT_STEER_LIMIT", 1f)

    var throttleControlsSpeed by BooleanDelegate("THROTTLE_CONTROLS_SPEED")

    var light by FloatDelegate("LIGHT")

    var backgroundBrightness by FloatDelegate("BACKGROUND_BRIGHTNESS", 0.2f)

    inner class FloatDelegate(private val key: String, private val default: Float = 0f) :
        ReadWriteProperty<ControlPreferences, Float> {

        override fun getValue(thisRef: ControlPreferences, property: KProperty<*>) =
            preferences.getFloat(key, default)

        override fun setValue(thisRef: ControlPreferences, property: KProperty<*>, value: Float) =
            preferences.edit { putFloat(key, value) }
    }

    inner class BooleanDelegate(private val key: String, private val default: Boolean = false) :
        ReadWriteProperty<ControlPreferences, Boolean> {

        override fun getValue(thisRef: ControlPreferences, property: KProperty<*>) =
            preferences.getBoolean(key, default)

        override fun setValue(thisRef: ControlPreferences, property: KProperty<*>, value: Boolean) =
            preferences.edit { putBoolean(key, value) }
    }

    inner class IntDelegate(private val key: String, private val default: Int = 0) :
        ReadWriteProperty<ControlPreferences, Int> {

        override fun getValue(thisRef: ControlPreferences, property: KProperty<*>) =
            preferences.getInt(key, default)

        override fun setValue(thisRef: ControlPreferences, property: KProperty<*>, value: Int) =
            preferences.edit { putInt(key, value) }
    }
}
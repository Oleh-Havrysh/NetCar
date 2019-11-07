package com.hakito.netcar

import android.content.Context
import androidx.core.content.edit

class ControlPreferences(context: Context) {

    private val preferences =
        context.getSharedPreferences(CONTROL_PREFERENCES, Context.MODE_PRIVATE)

    var steerMin: Float
        get() = preferences.getFloat(STEER_MIN, 0f)
        set(value) {
            preferences.edit {
                putFloat(STEER_MIN, value)
            }
        }

    var steerCenter: Float
        get() = preferences.getFloat(STEER_CENTER, 0.5f)
        set(value) {
            preferences.edit {
                putFloat(STEER_CENTER, value)
            }
        }

    var steerMax: Float
        get() = preferences.getFloat(STEER_MAX, 1f)
        set(value) {
            preferences.edit {
                putFloat(STEER_MAX, value)
            }
        }

    var invertSteer: Boolean
        get() = preferences.getBoolean(INVERT_STEER, false)
        set(value) {
            preferences.edit {
                putBoolean(INVERT_STEER, value)
            }
        }

    var throttleMax: Float
        get() = preferences.getFloat(THROTTLE_MAX, 0.5f)
        set(value) {
            preferences.edit {
                putFloat(THROTTLE_MAX, value)
            }
        }

    var voltageMultiplier: Float
        get() = preferences.getFloat(VOLTAGE_MULTIPLIER, 1f)
        set(value) {
            preferences.edit {
                putFloat(VOLTAGE_MULTIPLIER, value)
            }
        }

    var requestTimeout: Long
        get() = preferences.getLong(REQUEST_TIMEOUT, 500)
        set(value) {
            preferences.edit {
                putLong(REQUEST_TIMEOUT, value)
            }
        }

    var cameraEnabled: Boolean
        get() = preferences.getBoolean(CAMERA_ENABLED, true)
        set(value) {
            preferences.edit {
                putBoolean(CAMERA_ENABLED, value)
            }
        }

    var cameraRotation: Int
        get() = preferences.getInt(CAMERA_ROTATION, 0)
        set(value) {
            preferences.edit {
                putInt(CAMERA_ROTATION, value)
            }
        }

    companion object {
        private const val CONTROL_PREFERENCES = "CONTROL_PREFERENCES"
        private const val STEER_MIN = "STEER_MIN"
        private const val STEER_CENTER = "STEER_CENTER"
        private const val STEER_MAX = "STEER_MAX"
        private const val INVERT_STEER = "INVERT_STEER"
        private const val THROTTLE_MAX = "THROTTLE_MAX"
        private const val VOLTAGE_MULTIPLIER = "VOLTAGE_MULTIPLIER"
        private const val REQUEST_TIMEOUT = "REQUEST_TIMEOUT"
        private const val CAMERA_ENABLED = "CAMERA_ENABLED"
        private const val CAMERA_ROTATION = "CAMERA_ROTATION"
    }
}
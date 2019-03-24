package com.hakito.netcar

import android.content.Context
import androidx.core.content.edit

class ControlPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(CONTROL_PREFERENCES, Context.MODE_PRIVATE)

    var steerMin: Int
        get() = preferences.getInt(STEER_MIN, 0)
        set(value) {
            preferences.edit {
                putInt(STEER_MIN, value)
            }
        }

    var steerCenter: Int
        get() = preferences.getInt(STEER_CENTER, 90)
        set(value) {
            preferences.edit {
                putInt(STEER_CENTER, value)
            }
        }

    var steerMax: Int
        get() = preferences.getInt(STEER_MAX, 180)
        set(value) {
            preferences.edit {
                putInt(STEER_MAX, value)
            }
        }

    var invertSteer: Boolean
        get() = preferences.getBoolean(INVERT_STEER, false)
        set(value) {
            preferences.edit {
                putBoolean(INVERT_STEER, value)
            }
        }

    var throttleMax: Int
        get() = preferences.getInt(THROTTLE_MAX, 90)
        set(value) {
            preferences.edit {
                putInt(THROTTLE_MAX, value)
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
        get() = preferences.getLong(REQUEST_TIMEOUT, 100)
        set(value) {
            preferences.edit {
                putLong(REQUEST_TIMEOUT, value)
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
    }
}
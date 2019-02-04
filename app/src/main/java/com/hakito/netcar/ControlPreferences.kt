package com.hakito.netcar

import android.content.Context
import androidx.core.content.edit

class ControlPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(CONTROL_PREFERENCES, Context.MODE_PRIVATE)

    var steerMin: Int
        get() {
            return preferences.getInt(STEER_MIN, 0)
        }
        set(value) {
            preferences.edit {
                putInt(STEER_MIN, value)
            }
        }

    var steerCenter: Int
        get() {
            return preferences.getInt(STEER_CENTER, 90)
        }
        set(value) {
            preferences.edit {
                putInt(STEER_CENTER, value)
            }
        }

    var steerMax: Int
        get() {
            return preferences.getInt(STEER_MAX, 180)
        }
        set(value) {
            preferences.edit {
                putInt(STEER_MAX, value)
            }
        }

    companion object {
        private const val CONTROL_PREFERENCES = "CONTROL_PREFERENCES"
        private const val STEER_MIN = "STEER_MIN"
        private const val STEER_CENTER = "STEER_CENTER"
        private const val STEER_MAX = "STEER_MAX"
    }
}
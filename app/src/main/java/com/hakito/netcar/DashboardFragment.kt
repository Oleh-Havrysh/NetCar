package com.hakito.netcar

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_dashboard.*

class DashboardFragment : DialogFragment() {

    lateinit var controlPreferences: ControlPreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!)
            .setView(R.layout.fragment_dashboard)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlPreferences = ControlPreferences(context!!)
    }

    override fun onStart() {
        super.onStart()
        setupView()
    }

    private fun setupView() {
        dialog.lightPicker.apply {
            minValue = 0
            maxValue = 3
            displayedValues = arrayOf("Off", "Parking", "Low", "High")
        }

        dialog.steerStartPicker.apply {
            wrapSelectorWheel = false
            minValue = 0
            maxValue = 180
            value = controlPreferences.steerMin
            setOnValueChangedListener { _, _, newVal ->
                onSteerStartChanged(newVal)
            }
        }

        dialog.steerCenterPicker.apply {
            wrapSelectorWheel = false
            minValue = 0
            maxValue = 180
            value = controlPreferences.steerCenter
            setOnValueChangedListener { _, _, newVal ->
                onSteerCenterChanged(newVal)
            }
        }

        dialog.steerEndPicker.apply {
            wrapSelectorWheel = false
            minValue = 0
            maxValue = 180
            value = controlPreferences.steerMax
            setOnValueChangedListener { _, _, newVal ->
                onSteerEndChanged(newVal)
            }
        }

    }

    private fun onSteerStartChanged(value: Int) {
        dialog.steerCenterPicker.minValue = value
        dialog.steerEndPicker.minValue = value
        controlPreferences.steerMin = value
    }

    private fun onSteerCenterChanged(value: Int) {
        controlPreferences.steerCenter = value
    }

    private fun onSteerEndChanged(value: Int) {
        dialog.steerCenterPicker.maxValue = value
        dialog.steerStartPicker.maxValue = value
        controlPreferences.steerMax = value
    }
}
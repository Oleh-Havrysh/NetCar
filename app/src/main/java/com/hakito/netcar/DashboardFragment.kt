package com.hakito.netcar

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
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
        dialog!!.voltageMultiplierEditText.apply {
            setText(controlPreferences.voltageMultiplier.toString())
            addTextChangedListener {
                controlPreferences.voltageMultiplier = text.toString().toFloatOrNull() ?: 1f
            }
        }

        dialog!!.requestTimeoutEditText.apply {
            setText(controlPreferences.requestTimeout.toString())
            addTextChangedListener {
                controlPreferences.requestTimeout = text.toString().toIntOrNull() ?: 100
            }
        }

        dialog!!.invertSteerCheckBox.apply {
            isChecked = controlPreferences.invertSteer
            setOnCheckedChangeListener { _, isChecked ->
                controlPreferences.invertSteer = isChecked
            }
        }

        dialog!!.throttleLimitSeekBar.apply {
            percentProgress = controlPreferences.throttleMax
            onProgressChangedListener = {
                controlPreferences.throttleMax = it
            }
        }

        dialog!!.steerStartSeekBar.apply {
            percentProgress = controlPreferences.steerMin
            onProgressChangedListener = ::onSteerStartChanged
        }

        dialog!!.steerCenterSeekBar.apply {
            percentProgress = controlPreferences.steerCenter
            onProgressChangedListener = ::onSteerCenterChanged
        }

        dialog!!.steerEndSeekBar.apply {
            percentProgress = controlPreferences.steerMax
            onProgressChangedListener = ::onSteerEndChanged
        }


        dialog!!.cameraEnabledCheckBox.apply {
            isChecked = controlPreferences.cameraEnabled
            setOnCheckedChangeListener { _, isChecked ->
                controlPreferences.cameraEnabled = isChecked
            }
        }

        dialog!!.cameraRotationEditText.apply {
            setText(controlPreferences.cameraRotation.toString())
            addTextChangedListener {
                controlPreferences.cameraRotation = text.toString().toIntOrNull() ?: 0
            }
        }

        dialog!!.throttleDeadzoneCompensationSeekBar.apply {
            percentProgress = controlPreferences.throttleDeadzone
            onProgressChangedListener = ::onThrottleDeadzoneChanged
        }

        dialog!!.cruiseGainSeekBar.apply {
            percentProgress = controlPreferences.cruiseGain
            onProgressChangedListener = ::onCruiseGainChanged
        }

        dialog!!.preventSlippingCheckBox.apply {
            isChecked = controlPreferences.preventSlipping
            setOnCheckedChangeListener { _, isChecked ->
                controlPreferences.preventSlipping = isChecked
            }
        }

        dialog!!.steerStartSeekBar.percentMaxLimit = controlPreferences.steerCenter

        dialog!!.steerCenterSeekBar.percentMinLimit = controlPreferences.steerMin
        dialog!!.steerCenterSeekBar.percentMaxLimit = controlPreferences.steerMax

        dialog!!.steerEndSeekBar.percentMinLimit = controlPreferences.steerCenter
    }

    private fun onCruiseGainChanged(value: Float) {
        controlPreferences.cruiseGain = value
    }

    private fun onSteerStartChanged(value: Float) {
        dialog!!.steerCenterSeekBar.percentMinLimit = value
        controlPreferences.steerMin = value
    }

    private fun onSteerCenterChanged(value: Float) {
        dialog!!.steerStartSeekBar.percentMaxLimit = value
        dialog!!.steerEndSeekBar.percentMinLimit = value
        controlPreferences.steerCenter = value
    }

    private fun onSteerEndChanged(value: Float) {
        dialog!!.steerCenterSeekBar.percentMaxLimit = value
        controlPreferences.steerMax = value
    }

    private fun onThrottleDeadzoneChanged(value: Float) {
        controlPreferences.throttleDeadzone = value
    }
}
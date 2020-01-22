package com.hakito.netcar

import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.hakito.netcar.widget.LimitedSeekBar
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlin.reflect.KMutableProperty

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

        dialog!!.invertSteerCheckBox.bindBoolean(controlPreferences::invertSteer)

        dialog!!.throttleLimitSeekBar.bindToFloat(controlPreferences::throttleMax)

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

        dialog!!.cameraEnabledCheckBox.bindBoolean(controlPreferences::cameraEnabled)

        dialog!!.cameraRotationEditText.apply {
            setText(controlPreferences.cameraRotation.toString())
            addTextChangedListener {
                controlPreferences.cameraRotation = text.toString().toIntOrNull() ?: 0
            }
        }

        dialog!!.throttleDeadzoneCompensationSeekBar.bindToFloat(controlPreferences::throttleDeadzoneCompensation)

        dialog!!.cruiseGainSeekBar.bindToFloat(controlPreferences::cruiseGain)

        dialog!!.preventSlippingCheckBox.bindBoolean(controlPreferences::preventSlipping)

        dialog!!.cruiseDiffDependsOnThrottleCheckBox.bindBoolean(controlPreferences::cruiseDiffDependsOnThrottle)

        dialog!!.cruiseSpeedDiffSeekBar.bindToFloat(controlPreferences::cruiseSpeedDiff)

        dialog!!.steerStartSeekBar.percentMaxLimit = controlPreferences.steerCenter

        dialog!!.steerCenterSeekBar.percentMinLimit = controlPreferences.steerMin
        dialog!!.steerCenterSeekBar.percentMaxLimit = controlPreferences.steerMax

        dialog!!.steerEndSeekBar.percentMinLimit = controlPreferences.steerCenter
    }

    private fun CheckBox.bindBoolean(property: KMutableProperty<Boolean>) {
        isChecked = property.getter.call()
        setOnCheckedChangeListener { _, isChecked -> property.setter.call(isChecked) }
    }

    private fun LimitedSeekBar.bindToFloat(property: KMutableProperty<Float>) {
        percentProgress = property.getter.call()
        onProgressChangedListener = { property.setter.call(it) }
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
}
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
                controlPreferences.requestTimeout = text.toString().toLongOrNull() ?: 100L
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
            onProgressChangedListener = this@DashboardFragment::onSteerStartChanged
        }

        dialog!!.steerCenterSeekBar.apply {
            percentProgress = controlPreferences.steerCenter
            onProgressChangedListener = this@DashboardFragment::onSteerCenterChanged
        }

        dialog!!.steerEndSeekBar.apply {
            percentProgress = controlPreferences.steerMax
            onProgressChangedListener = this@DashboardFragment::onSteerEndChanged
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
            onProgressChangedListener = this@DashboardFragment::onThrottleDeadzoneChanged
        }

        dialog!!.steerStartSeekBar.percentMaxLimit = controlPreferences.steerCenter

        dialog!!.steerCenterSeekBar.percentMinLimit = controlPreferences.steerMin
        dialog!!.steerCenterSeekBar.percentMaxLimit = controlPreferences.steerMax

        dialog!!.steerEndSeekBar.percentMinLimit = controlPreferences.steerCenter

        var fromUser = true
        dialog!!.stabilizationEnabledCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (!fromUser) return@setOnCheckedChangeListener
            AlertDialog.Builder(context!!)
                .setTitle("Stabilization")
                .setMessage("Are you sure want to ${if (isChecked) "enable" else "disable"} stabilization?")
                .setPositiveButton("Yes") { _, _ -> }
                .setNegativeButton("No") { _, _ ->
                    fromUser = false
                    dialog!!.stabilizationEnabledCheckBox.isChecked = !isChecked
                    fromUser = true
                }
                .create()
                .show()
        }
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
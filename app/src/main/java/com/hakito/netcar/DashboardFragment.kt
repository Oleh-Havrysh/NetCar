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
        dialog.steerStartSeekBar.apply {
            progress = controlPreferences.steerMin
            onProgressChangedListener = this@DashboardFragment::onSteerStartChanged
        }

        dialog.steerCenterSeekBar.apply {
            progress = controlPreferences.steerCenter
            onProgressChangedListener = this@DashboardFragment::onSteerCenterChanged
        }

        dialog.steerEndSeekBar.apply {
            progress = controlPreferences.steerMax
            onProgressChangedListener = this@DashboardFragment::onSteerEndChanged
        }

        dialog.steerStartSeekBar.maxLimit = controlPreferences.steerCenter

        dialog.steerCenterSeekBar.minLimit = controlPreferences.steerMin
        dialog.steerCenterSeekBar.maxLimit = controlPreferences.steerMax

        dialog.steerEndSeekBar.minLimit = controlPreferences.steerCenter
    }

    private fun onSteerStartChanged(value: Int) {
        dialog.steerCenterSeekBar.minLimit = value
        controlPreferences.steerMin = value
    }

    private fun onSteerCenterChanged(value: Int) {
        dialog.steerStartSeekBar.maxLimit = value
        dialog.steerEndSeekBar.minLimit = value
        controlPreferences.steerCenter = value
    }

    private fun onSteerEndChanged(value: Int) {
        dialog.steerCenterSeekBar.maxLimit = value
        controlPreferences.steerMax = value
    }
}
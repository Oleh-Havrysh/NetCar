package com.hakito.netcar

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.hakito.netcar.base.BaseFragment
import com.hakito.netcar.cloud.CloudRepository
import com.hakito.netcar.util.bindToBoolean
import com.hakito.netcar.util.bindToFloat
import com.hakito.netcar.util.bindToInt
import com.hakito.netcar.util.launchWithProgressAndErrorHandling
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class DashboardFragment : BaseFragment(R.layout.fragment_dashboard) {

    private val controlPreferences: ControlPreferences by inject()

    private val cloudRepository: CloudRepository by inject()

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requestTimeoutEditText.bindToInt(controlPreferences::requestTimeout)

        cameraEnabledCheckBox.bindToBoolean(controlPreferences::cameraEnabled)
        cameraRotationEditText.bindToInt(controlPreferences::cameraRotation)

        backgroundBrightnessSeekBar.bindToFloat(controlPreferences::backgroundBrightness) {
            (activity as? OnBrightnessChangedListener)?.onBrightnessChanged(it)
        }

        backgroundBrightnessSeekBar.setOnTouchListener { _, event ->
            val viewsToHide =
                (view as ViewGroup).children.first().let { it as ViewGroup }.children
                    .filter { it.id != backgroundBrightnessSeekBar.id }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.background = ColorDrawable(Color.TRANSPARENT)
                    viewsToHide.forEach { it.isInvisible = true }
                }
                MotionEvent.ACTION_UP -> {
                    view.background = ColorDrawable(Color.WHITE)
                    viewsToHide.forEach { it.isVisible = true }
                }
            }
            false
        }

        voltageMultiplierEditText.addTextChangedListener {
            controlPreferences.voltageMultiplier = it.toString().toFloatOrNull() ?: 1f
        }

        lightSeekBar.bindToFloat(controlPreferences::light)

        invalidateCarConfig()

        saveButton.setOnClickListener { onSaveClick() }
        loadButton.setOnClickListener { onLoadClick() }

        loadConfigNames()

        voiceIndicationCheckBox.bindToBoolean(controlPreferences::voiceIndication)

        controlsTypeRadioGroup.check(
            when (controlPreferences.controlType) {
                ControlsType.SEPARATE -> R.id.separateControlsButton
                ControlsType.SINGLE -> R.id.singleControlButton
            }
        )

        controlsTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            controlPreferences.controlType = when (checkedId) {
                R.id.separateControlsButton -> ControlsType.SEPARATE
                R.id.singleControlButton -> ControlsType.SINGLE
                else -> throw IllegalStateException("Unsupported controls type")
            }
            activity?.recreate()
        }
    }

    private fun loadConfigNames() {
        launch {
            runCatching { cloudRepository.getConfigNames() }
                .fold({
                    val adapter =
                        ArrayAdapter<String>(
                            context ?: return@fold,
                            android.R.layout.simple_dropdown_item_1line,
                            it
                        )
                    configNameAutoCompleteTextView.setAdapter(adapter)
                }, { Toast.makeText(context ?: return@fold, it.message, Toast.LENGTH_LONG).show() })
        }
    }

    private fun onSaveClick() {
        launchWithProgressAndErrorHandling {
            cloudRepository.saveConfig(
                getConfigName(),
                controlPreferences.carConfig
            )
            Toast.makeText(context, "Config saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onLoadClick() {
        launchWithProgressAndErrorHandling {
            val config = cloudRepository.loadConfig(getConfigName())
                ?: throw Exception("Config was not found")
            controlPreferences.carConfig = config
            invalidateCarConfig()
            Toast.makeText(context, "Config loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getConfigName() = configNameAutoCompleteTextView.text.toString()

    private fun invalidateCarConfig() {
        voltageMultiplierEditText.setText(controlPreferences.voltageMultiplier.toString())

        invertSteerCheckBox.bindToBoolean(controlPreferences::invertSteer)

        throttleLimitSeekBar.bindToFloat(controlPreferences::throttleMax)

        steerStartSeekBar.apply {
            percentProgress = controlPreferences.steerMin
            onProgressChangedListener = ::onSteerStartChanged
        }

        steerCenterSeekBar.apply {
            percentProgress = controlPreferences.steerCenter
            onProgressChangedListener = ::onSteerCenterChanged
        }

        steerEndSeekBar.apply {
            percentProgress = controlPreferences.steerMax
            onProgressChangedListener = ::onSteerEndChanged
        }

        throttleDeadzoneCompensationSeekBar.bindToFloat(controlPreferences::throttleDeadzoneCompensation)

        cruiseGainSeekBar.bindToFloat(controlPreferences::cruiseGain)

        preventSlippingCheckBox.bindToBoolean(controlPreferences::preventSlipping)

        cruiseDiffDependsOnThrottleCheckBox.bindToBoolean(controlPreferences::cruiseDiffDependsOnThrottle)

        cruiseSpeedDiffSeekBar.bindToFloat(controlPreferences::cruiseSpeedDiff)

        speedDependantSteerLimitSeekBar.bindToFloat(controlPreferences::speedDependantSteerLimit)

        steerStartSeekBar.percentMaxLimit = controlPreferences.steerCenter

        steerCenterSeekBar.percentMinLimit = controlPreferences.steerMin
        steerCenterSeekBar.percentMaxLimit = controlPreferences.steerMax

        steerEndSeekBar.percentMinLimit = controlPreferences.steerCenter

        throttleControlsSpeedCheckBox.bindToBoolean(controlPreferences::throttleControlsSpeed)
    }

    private fun onSteerStartChanged(value: Float) {
        steerCenterSeekBar.percentMinLimit = value
        controlPreferences.steerMin = value
    }

    private fun onSteerCenterChanged(value: Float) {
        steerStartSeekBar.percentMaxLimit = value
        steerEndSeekBar.percentMinLimit = value
        controlPreferences.steerCenter = value
    }

    private fun onSteerEndChanged(value: Float) {
        steerCenterSeekBar.percentMaxLimit = value
        controlPreferences.steerMax = value
    }

    interface OnBrightnessChangedListener {

        fun onBrightnessChanged(brightness: Float)
    }
}
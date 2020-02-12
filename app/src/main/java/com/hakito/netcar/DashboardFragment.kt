package com.hakito.netcar

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.hakito.netcar.cloud.CloudRepository
import com.hakito.netcar.widget.LimitedSeekBar
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty

class DashboardFragment : DialogFragment(), CoroutineScope {

    lateinit var controlPreferences: ControlPreferences

    override val coroutineContext = Dispatchers.Main

    private val cloudRepository = CloudRepository()

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(context!!)
            .setView(R.layout.fragment_dashboard)
            .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlPreferences = ControlPreferences(context!!)
    }

    override fun onStart() {
        super.onStart()
        setupView()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        super.onDestroy()
    }

    private fun setupView() {
        dialog!!.requestTimeoutEditText.apply {
            setText(controlPreferences.requestTimeout.toString())
            addTextChangedListener {
                controlPreferences.requestTimeout = text.toString().toIntOrNull() ?: 100
            }
        }

        dialog!!.cameraEnabledCheckBox.bindToBoolean(controlPreferences::cameraEnabled)

        dialog!!.cameraRotationEditText.apply {
            setText(controlPreferences.cameraRotation.toString())
            addTextChangedListener {
                controlPreferences.cameraRotation = text.toString().toIntOrNull() ?: 0
            }
        }

        dialog!!.voltageMultiplierEditText.addTextChangedListener {
            controlPreferences.voltageMultiplier = it.toString().toFloatOrNull() ?: 1f
        }

        invalidateCarConfig()

        dialog!!.saveButton.setOnClickListener { onSaveClick() }
        dialog!!.loadButton.setOnClickListener { onLoadClick() }

        loadConfigNames()
    }

    private fun loadConfigNames() {
        launch {
            runCatching { cloudRepository.getConfigNames() }
                .fold({
                    val adapter =
                        ArrayAdapter<String>(
                            context!!,
                            android.R.layout.simple_dropdown_item_1line,
                            it
                        )
                    dialog!!.configNameAutoCompleteTextView.setAdapter(adapter)
                }, { Toast.makeText(context ?: return@fold, it.message, Toast.LENGTH_LONG).show() })
        }
    }

    private fun onSaveClick() {
        launch {
            cloudRepository.saveConfig(
                getConfigName(),
                controlPreferences.carConfig
            )
            Toast.makeText(context, "Config saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onLoadClick() {
        launch {
            val config = cloudRepository.loadConfig(getConfigName())
            if (config == null) {
                Toast.makeText(context, "Config was not found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            controlPreferences.carConfig = config
            invalidateCarConfig()
            Toast.makeText(context, "Config loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getConfigName() = dialog!!.configNameAutoCompleteTextView.text.toString()

    private fun invalidateCarConfig() {
        dialog!!.voltageMultiplierEditText.setText(controlPreferences.voltageMultiplier.toString())

        dialog!!.invertSteerCheckBox.bindToBoolean(controlPreferences::invertSteer)

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

        dialog!!.throttleDeadzoneCompensationSeekBar.bindToFloat(controlPreferences::throttleDeadzoneCompensation)

        dialog!!.cruiseGainSeekBar.bindToFloat(controlPreferences::cruiseGain)

        dialog!!.preventSlippingCheckBox.bindToBoolean(controlPreferences::preventSlipping)

        dialog!!.cruiseDiffDependsOnThrottleCheckBox.bindToBoolean(controlPreferences::cruiseDiffDependsOnThrottle)

        dialog!!.cruiseSpeedDiffSeekBar.bindToFloat(controlPreferences::cruiseSpeedDiff)

        dialog!!.speedDependantSteerLimitSeekBar.bindToFloat(controlPreferences::speedDependantSteerLimit)

        dialog!!.steerStartSeekBar.percentMaxLimit = controlPreferences.steerCenter

        dialog!!.steerCenterSeekBar.percentMinLimit = controlPreferences.steerMin
        dialog!!.steerCenterSeekBar.percentMaxLimit = controlPreferences.steerMax

        dialog!!.steerEndSeekBar.percentMinLimit = controlPreferences.steerCenter

        dialog!!.throttleControlsSpeedCheckBox.bindToBoolean(controlPreferences::throttleControlsSpeed)
    }

    private fun CheckBox.bindToBoolean(property: KMutableProperty<Boolean>) {
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
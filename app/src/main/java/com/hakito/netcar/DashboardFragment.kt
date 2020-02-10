package com.hakito.netcar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.hakito.netcar.cloud.CloudRepository
import com.hakito.netcar.util.withErrorHandler
import com.hakito.netcar.util.withProgress
import com.hakito.netcar.widget.LimitedSeekBar
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty

class DashboardFragment : Fragment(), CoroutineScope {

    lateinit var controlPreferences: ControlPreferences

    override val coroutineContext = Dispatchers.Main

    private val cloudRepository = CloudRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlPreferences = ControlPreferences(context!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requestTimeoutEditText.apply {
            setText(controlPreferences.requestTimeout.toString())
            addTextChangedListener {
                controlPreferences.requestTimeout = text.toString().toIntOrNull() ?: 100
            }
        }

        cameraEnabledCheckBox.bindToBoolean(controlPreferences::cameraEnabled)

        cameraRotationEditText.apply {
            setText(controlPreferences.cameraRotation.toString())
            addTextChangedListener {
                controlPreferences.cameraRotation = text.toString().toIntOrNull() ?: 0
            }
        }

        voltageMultiplierEditText.addTextChangedListener {
            controlPreferences.voltageMultiplier = it.toString().toFloatOrNull() ?: 1f
        }

        invalidateCarConfig()

        saveButton.setOnClickListener { onSaveClick() }
        loadButton.setOnClickListener { onLoadClick() }

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
                    configNameAutoCompleteTextView.setAdapter(adapter)
                }, { Toast.makeText(context ?: return@fold, it.message, Toast.LENGTH_LONG).show() })
        }
    }

    private fun onSaveClick() {
        launch {
            withProgress {
                withErrorHandler {
                    cloudRepository.saveConfig(
                        getConfigName(),
                        controlPreferences.carConfig
                    )
                    Toast.makeText(context, "Config saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onLoadClick() {
        launch {
            withProgress {
                withErrorHandler {
                    val config = cloudRepository.loadConfig(getConfigName())
                    if (config == null) {
                        Toast.makeText(context, "Config was not found", Toast.LENGTH_SHORT).show()
                        return@withErrorHandler
                    }
                    controlPreferences.carConfig = config
                    invalidateCarConfig()
                    Toast.makeText(context, "Config loaded", Toast.LENGTH_SHORT).show()
                }
            }
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
}
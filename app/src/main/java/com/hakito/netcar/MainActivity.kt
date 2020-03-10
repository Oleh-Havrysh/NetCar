package com.hakito.netcar

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import com.hakito.netcar.controls.ControlsInterface
import com.hakito.netcar.controls.SeparateControlsFragment
import com.hakito.netcar.controls.SingleControlsFragment
import com.hakito.netcar.sender.CarParams
import com.hakito.netcar.sender.CarResponse
import com.hakito.netcar.sender.CarSender
import com.hakito.netcar.util.ErrorsController
import com.hakito.netcar.util.ResponseTimeGraphController
import com.hakito.netcar.util.StatisticsController
import com.hakito.netcar.util.WheelRpmGraphController
import com.hakito.netcar.voice.indication.VoiceIndicator
import com.hakito.netcar.work.CarEnabledChecker
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.IOException
import kotlin.math.sign

class MainActivity : BaseActivity(), DashboardFragment.OnBrightnessChangedListener {

    private var sendingJob: Job? = null

    private val sender: CarSender by inject()

    private val controlPreferences: ControlPreferences by inject()

    private val stabilizationController: StabilizationController by inject()

    private val errorsController: ErrorsController by inject()

    private val wheelRpmGraphController: WheelRpmGraphController by inject()

    private val responseTimeGraphController: ResponseTimeGraphController by inject()

    private val statisticsController: StatisticsController by inject()

    private val responseHandlingChannel = Channel<CarResponse?>(Channel.CONFLATED)

    private val batteryProcessor: BatteryProcessor by inject()

    private val controls: ControlsInterface?
        get() = supportFragmentManager.findFragmentById(R.id.controlsFragmentContainer) as? ControlsInterface

    private val carEnabledChecker: CarEnabledChecker by inject()

    private val voiceIndicator: VoiceIndicator by inject()

    override val layoutRes = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showControls()

        dashboardButton.setOnClickListener {
            supportFragmentManager
                .commit {
                    replace(R.id.fragmentContainer, DashboardFragment())
                    addToBackStack(null)
                }
        }

        initTimeGraph()
        initRpmGraph()

        desiredRpmSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                stabilizationController.targetCruiseRpm = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        batteryProcessor.onBatteryLow = voiceIndicator::batteryLow

        launch {
            responseHandlingChannel.consumeEach(::onResponse)
        }

        gaugesCheckBox.setOnCheckedChangeListener { _, isChecked ->
            gaugesGroup.isVisible = isChecked
        }

        if (controlPreferences.voiceIndication) voiceIndicator.initialize()
    }

    override fun onStart() {
        super.onStart()
        carEnabledChecker.onAppStart()
    }

    override fun onStop() {
        carEnabledChecker.onAppStop()
        super.onStop()
    }

    override fun onDestroy() {
        voiceIndicator.shutdown()
        batteryProcessor.onBatteryLow = null
        super.onDestroy()
    }

    private fun showControls() {
        val fragment: Fragment = when (controlPreferences.controlType) {
            ControlsType.SEPARATE -> SeparateControlsFragment()
            ControlsType.SINGLE -> SingleControlsFragment()
        }
        supportFragmentManager.commitNow { add(R.id.controlsFragmentContainer, fragment) }
    }

    override fun onBrightnessChanged(brightness: Float) {
        adjustBrightness()
    }

    private fun adjustBrightness() {
        val brightness = (controlPreferences.backgroundBrightness * 255).toInt()
        val color = Color.rgb(brightness, brightness, brightness)
        window.setBackgroundDrawable(ColorDrawable(color))

        val linesColor =
            if (controlPreferences.backgroundBrightness < 0.5) Color.WHITE else Color.BLACK
        controls?.setColor(linesColor)
    }


    private fun initTimeGraph() {
        timeGraph.apply {
            addSeries(responseTimeGraphController.timeSeries)
            gridLabelRenderer.isHorizontalLabelsVisible = false
            viewport.apply {
                isXAxisBoundsManual = true
                setMinX(0.0)
                setMaxX(3.0)
                isYAxisBoundsManual = true
                setMinY(0.0)
                setMaxY(controlPreferences.requestTimeout.toDouble())
            }
        }
    }

    private fun initRpmGraph() {
        rpmGraph.apply {
            addSeries(wheelRpmGraphController.rpmFrontLeftSeries)
            //addSeries(rpmFrontRightSeries)
            addSeries(wheelRpmGraphController.rpmRearSeries)
            legendRenderer.isVisible = true
            gridLabelRenderer.isHorizontalLabelsVisible = false
            viewport.apply {
                isXAxisBoundsManual = true
                setMinX(0.0)
                setMaxX(3.0)
                isYAxisBoundsManual = true
                setMinY(0.0)
                setMaxY(2500.0)
            }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        adjustBrightness()
        controls?.resetValues()
        startSending()
    }

    override fun onPause() {
        super.onPause()
        stopSending()
    }

    private fun startSending() {
        sendingJob = launch(Dispatchers.IO) {
            while (true) {
                try {
                    val throttle =
                        (controls?.getThrottle() ?: 0f)
                            .times(controlPreferences.throttleMax)
                            .let(stabilizationController::calcThrottle)
                            .times(ServoConstants.AMPLITUDE)
                            .let { it + it.sign * ServoConstants.AMPLITUDE * controlPreferences.throttleDeadzoneCompensation }
                            .plus(ServoConstants.CENTER)
                            .toInt()
                    val steer = (controls?.getSteer() ?: 0f)
                        .let(stabilizationController::calcSteer)
                        .times(if (controlPreferences.invertSteer) -1 else 1)
                        .run { mapSteer(this) }
                    sendValue(steer, throttle)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun mapSteer(steerValue: Float): Float {
        val range = if (steerValue < 0)
            controlPreferences.steerCenter.toServoValue() - controlPreferences.steerMin.toServoValue()
        else
            controlPreferences.steerMax.toServoValue() - controlPreferences.steerCenter.toServoValue()
        return controlPreferences.steerCenter.toServoValue() + steerValue * range
    }

    private suspend fun sendValue(steer: Float?, throttle: Int) {
        val steerValue = steer?.toInt() ?: controlPreferences.steerCenter.toServoValue().toInt()
        statisticsController.handleInputParams(steerValue = steerValue, throttleValue = throttle)
        val response =
            try {
                sender.send(
                    CarParams(
                        steerValue,
                        throttle,
                        (controlPreferences.light * 1023).toInt()
                    )
                )
                    .also { statisticsController.onRequestPerformed() }
            } catch (e: IOException) {
                errorsController.onError(e)
                null
            }

        response?.sensors?.also(stabilizationController::onSensorsReceived)

        responseHandlingChannel.offer(response)
    }

    private fun onResponse(response: CarResponse?) {
        statTextView.text = statisticsController.getText()

        response?.sensors?.voltage?.let {
            val battery = batteryProcessor.processRawVoltage(it)
            batteryTextView.text = battery.text
            batteryTextView.setTextColor(battery.color)
        }

        responseTimeGraphController.appendResponseTime(response?.responseTime)
        if (response != null) {
            statisticsController.handleRpm(response.sensors.frontLeftRpm)
            wheelRpmGraphController.appendRpm(
                frontLeft = response.sensors.frontLeftRpm,
                frontRight = response.sensors.frontRightRpm,
                rear = response.sensors.rearRpm
            )
            rpmWarningImageView.isVisible = stabilizationController.isStabilizationWarning()
        }
    }

    private fun stopSending() {
        sendingJob?.cancel()
    }
}

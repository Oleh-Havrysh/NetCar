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
import com.hakito.netcar.sender.CarSenderImpl
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.max
import kotlin.math.sign

class MainActivity : BaseActivity(), DashboardFragment.OnBrightnessChangedListener {

    private var sendingJob: Job? = null

    private lateinit var sender: CarSender

    private lateinit var controlPreferences: ControlPreferences

    private var graphStartTime = System.currentTimeMillis()
    private var maxTime = 0L

    private val timeSeries = LineGraphSeries<DataPoint>()
    private val rpmFrontLeftSeries = LineGraphSeries<DataPoint>()
        .apply {
            this.title = "FL"
            this.color = Color.RED
        }
    private val rpmFrontRightSeries = LineGraphSeries<DataPoint>()
        .apply {
            this.title = "FR"
            this.color = Color.BLUE
        }
    private val rpmRearSeries = LineGraphSeries<DataPoint>()
        .apply {
            this.title = "R"
            this.color = Color.GREEN
        }

    private val errorsMap = mutableMapOf<String, Int>()

    private var maxSpeed = 0

    private val controlCounter = OperationsPerSecondCounter(50)

    private lateinit var stabilizationController: StabilizationController

    private var lastSteerValue = 0
    private var lastThrottleValue = 0

    private val responseHandlingChannel = Channel<CarResponse?>(Channel.CONFLATED)

    private lateinit var batteryProcessor: BatteryProcessor

    private val controls: ControlsInterface?
        get() = supportFragmentManager.findFragmentById(R.id.controlsFragmentContainer) as? ControlsInterface

    override val layoutRes = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlPreferences = ControlPreferences(this)

        showControls()

        stabilizationController = StabilizationController(controlPreferences)
        batteryProcessor = BatteryProcessor(controlPreferences)

        dashboardButton.setOnClickListener {
            supportFragmentManager
                .commit {
                    replace(R.id.fragmentContainer, DashboardFragment())
                    addToBackStack(null)
                }
        }

        initTimeGraph()
        initRpmGraph()

        sender = CarSenderImpl(controlPreferences)

        desiredRpmSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                stabilizationController.targetCruiseRpm = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        launch {
            responseHandlingChannel.consumeEach(::onResponse)
        }

        gaugesCheckBox.setOnCheckedChangeListener { _, isChecked ->
            gaugesGroup.isVisible = isChecked
        }
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

    private fun getGraphTime() = (System.currentTimeMillis() - graphStartTime) / 1000.0

    private fun initTimeGraph() {
        timeGraph.apply {
            addSeries(timeSeries)
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
            addSeries(rpmFrontLeftSeries)
            //addSeries(rpmFrontRightSeries)
            addSeries(rpmRearSeries)
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

    var cameraJob: Job? = null

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
        lastSteerValue = steerValue
        lastThrottleValue = throttle
        val response =
            try {
                sender.send(
                    CarParams(
                        steerValue,
                        throttle,
                        (controlPreferences.light * 1023).toInt()
                    )
                )
                    .also { controlCounter.onPerformed() }
            } catch (e: IOException) {
                e.printStackTrace()
                val errorName = e.message!!
                val count = errorsMap[errorName] ?: 0
                errorsMap[errorName] = count + 1
                null
            }

        if (response != null) {
            maxTime = max(response.responseTime, maxTime)
        }

        response?.sensors?.also(stabilizationController::onSensorsReceived)

        responseHandlingChannel.offer(response)
    }

    private fun onResponse(response: CarResponse?) {
        val speed = response?.sensors?.frontLeftRpm?.let { getSpeed(it) }
        speed?.apply { maxSpeed = max(maxSpeed, this) }
        statTextView.text =
            """
steer: $lastSteerValue, throttle: $lastThrottleValue
speed = $speed kmh
maxSpeed = $maxSpeed kmh
control RPS = ${controlCounter.getRps()}
$errorsMap"""

        response?.sensors?.voltage?.let {
            val battery = batteryProcessor.processRawVoltage(it)
            batteryTextView.text = battery.text
            batteryTextView.setTextColor(battery.color)
        }

        timeSeries.appendData(
            DataPoint(
                getGraphTime(),
                response?.responseTime?.toDouble() ?: 0.0
            ), true, 150
        )
        if (response != null) {
            rpmFrontLeftSeries.appendData(
                DataPoint(
                    getGraphTime(),
                    response.sensors.frontLeftRpm.toDouble()
                ), true, 150
            )
/*            rpmFrontRightSeries.appendData(
                DataPoint(
                    getGraphTime(),
                    response.sensors.frontRightRpm.toDouble()
                ), true, 150
            )*/
            rpmRearSeries.appendData(
                DataPoint(
                    getGraphTime(),
                    response.sensors.rearRpm.toDouble()
                ), true, 150
            )

            rpmWarningImageView.isVisible = stabilizationController.isStabilizationWarning()
        }
    }

    private fun getSpeed(rpm: Int): Int {
        val wheelLength = 7f * Math.PI
        return ((rpm * 60 * wheelLength) / (100 * 1000)).toInt()
    }

    private fun stopSending() {
        sendingJob?.cancel()
        cameraJob?.cancel()
    }
}

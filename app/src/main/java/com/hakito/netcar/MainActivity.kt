package com.hakito.netcar

import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import androidx.core.view.isVisible
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

class MainActivity : BaseActivity() {

    private var sendingJob: Job? = null

    private lateinit var sender: CarSender

    private lateinit var controlPreferences: ControlPreferences

    private var cycles = 0
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

    private val cameraCounter = OperationsPerSecondCounter(10)
    private val controlCounter = OperationsPerSecondCounter(50)

    private lateinit var stabilizationController: StabilizationController

    private var lastSteerValue = 0
    private var lastThrottleValue = 0

    private val responseHandlingChannel = Channel<CarResponse?>(Channel.CONFLATED)

    override val layoutRes = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlPreferences = ControlPreferences(this)

        stabilizationController = StabilizationController(controlPreferences)

        dashboardButton.setOnClickListener {
            DashboardFragment().show(supportFragmentManager, "")
        }

        initTimeGraph()
        initRpmGraph()

        sender = CarSenderImpl(controlPreferences)

        image.rotation = controlPreferences.cameraRotation.toFloat()

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
    }

    private fun initTimeGraph() {
        timeGraph.apply {
            addSeries(timeSeries)
            gridLabelRenderer.isHorizontalLabelsVisible = false
            viewport.apply {
                isXAxisBoundsManual = true
                setMinX(0.0)
                setMaxX(50.0)
                isYAxisBoundsManual = true
                setMinY(0.0)
                setMaxY(controlPreferences.requestTimeout.toDouble())
            }
        }
    }

    private fun initRpmGraph() {
        rpmGraph.apply {
            addSeries(rpmFrontLeftSeries)
            addSeries(rpmFrontRightSeries)
            addSeries(rpmRearSeries)
            legendRenderer.isVisible = true
            gridLabelRenderer.isHorizontalLabelsVisible = false
            viewport.apply {
                isXAxisBoundsManual = true
                setMinX(0.0)
                setMaxX(50.0)
                isYAxisBoundsManual = true
                setMinY(0.0)
                setMaxY(3000.0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
                    if (throttleTouchView.progress != null) {
                        desiredRpmSeekBar.progress = 0
                    }
                    val throttle =
                        (throttleTouchView.progress?.y ?: 0f)
                            .times(controlPreferences.throttleMax)
                            .let(stabilizationController::calcThrottle)
                            .times(ServoConstants.AMPLITUDE)
                            .let { it + it.sign * ServoConstants.AMPLITUDE * controlPreferences.throttleDeadzone }
                            .plus(ServoConstants.CENTER)
                            .toInt()
                    val steer = steerTouchView.progress?.x
                        ?.times(if (controlPreferences.invertSteer) -1 else 1)
                        ?.run { mapSteer(this) }
                    sendValue(steer, throttle)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

/*        cameraJob = launch(Dispatchers.IO) {
            while (true) {
                if (!controlPreferences.cameraEnabled) {
                    delay(1000)
                    continue
                }
                val bitmap = runCatching { sender.getImage() }.getOrNull()
                    ?.also { cameraCounter.onPerformed() }

                launch(Dispatchers.Main) {
                    bitmap?.also { image.setImageBitmap(it) }
                }
            }
        }*/
    }

    private fun mapSteer(steerValue: Float): Float {
        val range = if (steerValue < 0)
            controlPreferences.steerCenter.toServoValue() - controlPreferences.steerMin.toServoValue()
        else
            controlPreferences.steerMax.toServoValue() - controlPreferences.steerCenter.toServoValue()
        return controlPreferences.steerCenter.toServoValue() + steerValue * range
    }

    private suspend fun sendValue(steer: Float?, throttle: Int) {
        cycles++

        val steerValue = steer?.toInt() ?: controlPreferences.steerCenter.toServoValue().toInt()
        lastSteerValue = steerValue
        lastThrottleValue = throttle
        val response =
            try {
                sender.send(CarParams(steerValue, throttle))
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

        /*     val voltageString =
                 response?.voltageRaw?.times(controlPreferences.voltageMultiplier)
                     ?.let { String.format("%.2f", it) }
     */
        response?.sensors?.also(stabilizationController::onSensorsReceived)

        responseHandlingChannel.offer(response)
    }

    private fun onResponse(response: CarResponse?) {
        val speed = response?.sensors?.frontLeftRpm?.let { getSpeed(it) }
        speed?.apply { maxSpeed = max(maxSpeed, this) }
        statTextView.text = "max: $maxTime\n" +
                //"V=$voltageString\n" +
                "steer: $lastSteerValue, throttle: $lastThrottleValue\n" +
                "speed = $speed kmh\n" +
                "maxSpeed = $maxSpeed kmh\n" +
                //"camera FPS = ${cameraCounter.getRps()}\n" +
                "control RPS = ${controlCounter.getRps()}\n" +
                "$errorsMap\n"

        timeSeries.appendData(
            DataPoint(
                cycles.toDouble(),
                response?.responseTime?.toDouble() ?: 0.0
            ), true, 50
        )
        if (response != null) {
            rpmFrontLeftSeries.appendData(
                DataPoint(
                    cycles.toDouble(),
                    response.sensors.frontLeftRpm.toDouble()
                ), true, 50
            )
            rpmFrontRightSeries.appendData(
                DataPoint(
                    cycles.toDouble(),
                    response.sensors.frontRightRpm.toDouble()
                ), true, 50
            )
            rpmRearSeries.appendData(
                DataPoint(
                    cycles.toDouble(),
                    response.sensors.rearRpm.toDouble()
                ), true, 50
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

package com.hakito.netcar

import android.os.Bundle
import com.hakito.netcar.sender.CarParams
import com.hakito.netcar.sender.CarSender
import com.hakito.netcar.sender.CarSenderImpl
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
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

    private val errorsMap = mutableMapOf<String, Int>()

    private var maxSpeed = 0

    private val cameraCounter = OperationsPerSecondCounter(10)

    private val controlCounter = OperationsPerSecondCounter(50)

    override val layoutRes = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlPreferences = ControlPreferences(this)

        dashboardButton.setOnClickListener {
            DashboardFragment().show(supportFragmentManager, "")
        }

        initTimeGraph()

        sender = CarSenderImpl(controlPreferences)

        image.rotation = controlPreferences.cameraRotation.toFloat()
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
                    val throttle =
                        throttleTouchView.progress?.y
                            ?.times(controlPreferences.throttleMax * ServoConstants.AMPLITUDE)
                            ?.let { it + it.sign * ServoConstants.AMPLITUDE * controlPreferences.throttleDeadzone }
                            ?.plus(ServoConstants.CENTER)
                    val steer = steerTouchView.progress?.x
                        ?.times(if (controlPreferences.invertSteer) -1 else 1)
                        ?.run { mapSteer(this) }
                    sendValue(steer, throttle)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                yield()
            }
        }

        cameraJob = launch(Dispatchers.IO) {
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
        }
    }

    private fun mapSteer(steerValue: Float): Float {
        val range = if (steerValue < 0)
            controlPreferences.steerCenter.toServoValue() - controlPreferences.steerMin.toServoValue()
        else
            controlPreferences.steerMax.toServoValue() - controlPreferences.steerCenter.toServoValue()
        return controlPreferences.steerCenter.toServoValue() + steerValue * range
    }

    private suspend fun sendValue(steer: Float?, throttle: Float?) {
        cycles++

        val steerValue = steer?.toInt() ?: controlPreferences.steerCenter.toServoValue().toInt()
        val throttleValue = throttle?.toInt() ?: ServoConstants.CENTER

        val response =
            try {
                sender.send(CarParams(steerValue, throttleValue))
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

        val speed = response?.rpm?.let { getSpeed(it) }

        val voltageString =
            response?.voltageRaw?.times(controlPreferences.voltageMultiplier)
                ?.let { String.format("%.2f", it) }

        withContext(Dispatchers.Main) {
            speed?.apply {
                maxSpeed = max(maxSpeed, this)
            }
            statTextView.text = "max: $maxTime\n" +
                    "V=$voltageString\n" +
                    "steer: $steerValue, throttle: $throttleValue\n" +
                    "rpm=${response?.rpm}\n" +
                    "speed = $speed kmh\n" +
                    "maxSpeed = $maxSpeed kmh\n" +
                    "camera FPS = ${cameraCounter.getRps()}\n" +
                    "control RPS = ${controlCounter.getRps()}\n" +
                    "$errorsMap\n"

            timeSeries.appendData(
                DataPoint(
                    cycles.toDouble(),
                    response?.responseTime?.toDouble() ?: 0.0
                ), true, 50
            )
        }
    }

    private fun getSpeed(rpm: Int): Int {
        val wheelLength = 5.5f * Math.PI
        return ((rpm * 60 * wheelLength)
                / (100 * 1000)).toInt()
    }

    private fun stopSending() {
        sendingJob?.cancel()
        cameraJob?.cancel()
    }
}

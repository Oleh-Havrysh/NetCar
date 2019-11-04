package com.hakito.netcar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hakito.netcar.sender.CarParams
import com.hakito.netcar.sender.CarSender
import com.hakito.netcar.sender.CarSenderImpl
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.IOException
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private var sendingJob: Job? = null

    private lateinit var sender: CarSender

    private lateinit var controlPreferences: ControlPreferences

    private var cycles = 0
    private var maxTime = 0L

    private val timeSeries = LineGraphSeries<DataPoint>()

    private val errorsMap = mutableMapOf<String, Int>()

    private var maxSpeed = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controlPreferences = ControlPreferences(this)

        dashboardButton.setOnClickListener {
            DashboardFragment().show(supportFragmentManager, "")
        }

        initTimeGraph()

        speedometer.apply {
            setNeedleStepFactor(25f)
            setDeltaTimeInterval(0)
        }

        sender = CarSenderImpl(controlPreferences)
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
        sendingJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val throttle =
                        throttleTouchView.progress?.y
                            ?.times(controlPreferences.throttleMax * ServoConstants.AMPLITUDE)
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
        return
        cameraJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val bitmap = runCatching { sender.getImage() }.getOrNull()
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
                speedometer.moveToValue(toFloat())
                maxSpeed = max(maxSpeed, this)
            }
            statTextView.text = "max: $maxTime\n" +
                    "V=$voltageString\n" +
                    "steer: $steerValue, throttle: $throttleValue\n" +
                    "rpm=${response?.rpm}\n" +
                    "speed = $speed kmh\n" +
                    "maxSpeed = $maxSpeed kmh\n" +
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

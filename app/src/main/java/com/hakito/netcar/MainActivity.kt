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

    private val sender: CarSender = CarSenderImpl()

    private var cycles = 0
    private var successCount = 0
    private var failCount = 0
    private var maxTime = 0L

    private val timeSeries = LineGraphSeries<DataPoint>()

    private val errorsMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initTimeGraph()
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
                setMaxY(50.0)
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

    private fun startSending() {
        sendingJob = GlobalScope.launch(Dispatchers.IO) {

            while (true) {
                try {
                    val steer = normalize(steerTouchView.progress?.x)
                    val throttle = normalize(throttleTouchView.progress?.y?.unaryMinus())
                    sendValue(steer, throttle)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                yield()
            }
        }
    }

    private fun normalize(v: Float?): Float? {
        v ?: return null

        val scaled = v / 2
        val withOffset = scaled + 90
        val constrained = if (withOffset < 0) 0F else if (withOffset > 180) 180F else withOffset
        return constrained
    }

    private suspend fun sendValue(steer: Float?, throttle: Float?) {
        cycles++

        val steerValue = steer?.toInt() ?: 90
        val throttleValue = throttle?.toInt() ?: 90

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
            successCount++
            maxTime = max(response.responseTime, maxTime)
        } else {
            failCount++
        }


        val voltageString = response?.voltage?.let { String.format("%.2f", it) }
        withContext(Dispatchers.Main) {
            statTextView.text = "$successCount:$failCount\n" +
                    "max: $maxTime\n" +
                    "V=$voltageString\n" +
                    "$errorsMap"

            timeSeries.appendData(DataPoint(cycles.toDouble(), response?.responseTime?.toDouble() ?: 0.0), true, 50)
        }
    }

    private fun stopSending() {
        sendingJob?.cancel()
    }
}

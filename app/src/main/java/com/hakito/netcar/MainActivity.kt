package com.hakito.netcar

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private var sendingJob: Job? = null

    private val client = OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.MILLISECONDS)
        .build()

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
        val request = Request.Builder()
            .url("http://192.168.4.1:81/car?steer=$steerValue&throttle=$throttleValue")
            .get()
            .build()


        var response: Response? = null
        try {
            response = client.newCall(request).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            val errorName = e.message!!
            val count = errorsMap[errorName] ?: 0
            errorsMap[errorName] = count + 1
        }

        var voltage: Float? = null
        if (response?.isSuccessful ?: false) {
            successCount++
            val result = response!!.body()?.string()
            Log.d("SEND", "Result: $result")
            if (result != null) {
                val rawVoltage = result.split('=')[1].toInt()
                voltage = rawVoltage / 1023F * 10.73518518518519F
            }
        } else {
            failCount++
            Log.e("SEND", "Fail")
        }
        val time = response?.receivedResponseAtMillis()?.minus(response.sentRequestAtMillis())
        time?.apply {
            maxTime = max(this, maxTime)
        }

        val voltageString = voltage?.let { String.format("%.2f", it) }
        withContext(Dispatchers.Main) {
            statTextView.text = "$successCount:$failCount\n" +
                    "max: $maxTime\n" +
                    "V=$voltageString\n" +
                    "$errorsMap"

            timeSeries.appendData(DataPoint(cycles.toDouble(), time?.toDouble() ?: 0.0), true, 50)
        }
    }

    private fun stopSending() {
        sendingJob?.cancel()
    }
}

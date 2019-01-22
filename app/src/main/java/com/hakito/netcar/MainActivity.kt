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
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private var sendingJob: Job? = null

    private val client = OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.MILLISECONDS)
        .build()

    private var cycles = 0
    private var successCount = 0
    private var failCount = 0
    private var maxTime = 0L
    private var minTime = 100000L

    private val timeSeries = LineGraphSeries<DataPoint>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initTimeGraph()
    }

    private fun initTimeGraph() {
        timeGraph.apply {
            addSeries(timeSeries)
            title = "Request time"
            gridLabelRenderer.isHorizontalLabelsVisible = false
            viewport.apply {
                isXAxisBoundsManual = true
                setMinX(0.0)
                setMaxX(100.0)
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
                    sendValue(steerSeekBar.progress, throttleSeekBar.progress)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                yield()
            }
        }
    }

    private suspend fun sendValue(steer: Int, throttle: Int) {
        cycles++

        val request = Request.Builder()
            .url("http://192.168.4.1/car?steer=${steer}&throttle=${throttle}")
            .get()
            .build()


        var response: Response? = null
        try {
            response = client.newCall(request).execute()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (response?.code() == 200) {
            successCount++
            Log.d(
                "SEND",
                "Succ: ${response.body()?.string()} Time: ${response.receivedResponseAtMillis() - response.sentRequestAtMillis()}"
            )
        } else {
            failCount++
            Log.e("SEND", "Fail")
        }
        val time = response?.receivedResponseAtMillis()?.minus(response.sentRequestAtMillis())
        time?.apply {
            maxTime = max(this, maxTime)
            minTime = min(this, minTime)
        }


        withContext(Dispatchers.Main) {
            statTextView.text = "$cycles\n" +
                    "$successCount:$failCount\n" +
                    "Time: $time\n" +
                    "max: $maxTime\n" +
                    "min: $minTime"

            timeSeries.appendData(DataPoint(cycles.toDouble(), time?.toDouble() ?: 0.0), true, 100)
        }
    }

    private fun stopSending() {
        sendingJob?.cancel()
    }
}

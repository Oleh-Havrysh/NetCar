package com.hakito.netcar

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.hakito.netcar.sender.CarParams
import com.hakito.netcar.sender.CarSender
import com.hakito.netcar.sender.CarSenderImpl
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private var sendingJob: Job? = null

    private val sender: CarSender = CarSenderImpl()

    private lateinit var controlPreferences: ControlPreferences

    private var cycles = 0
    private var successCount = 0
    private var failCount = 0
    private var maxTime = 0L

    private val timeSeries = LineGraphSeries<DataPoint>()

    private val errorsMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controlPreferences = ControlPreferences(this)

        dashboardButton.setOnClickListener {
            DashboardFragment().show(supportFragmentManager, "")
        }

        initTimeGraph()
        //connectWifiNetwork()
    }

    private fun connectWifiNetwork() {
        val carNetwork = "TinyCar"
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val connection = wifiManager.connectionInfo
        if (connection.ssid != carNetwork) {
            wifiManager.isWifiEnabled = true
            wifiManager.disconnect()

            val wifiConfiguration = WifiConfiguration()
                .apply {
                    this.SSID = "\"$carNetwork\""
                    this.preSharedKey = "\"espespesp\""
                }
            val networkId = wifiManager.addNetwork(wifiConfiguration)
            wifiManager.enableNetwork(networkId, true)
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
                    val throttle = throttleTouchView.progress?.y?.run { normalize(-this) * 180 }
                    val throttlePercent = throttle?.run { abs((this - 90) / 90) } ?: 0f
                    val steer = steerTouchView.progress?.x?.run { mapSteer(normalize(this), throttlePercent) }
                    sendValue(steer, throttle)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                yield()
            }
        }
    }

    private fun mapSteer(steerValue: Float, throttle: Float): Float {
        val rangeScale = min(1f, max(0.1f, 1.2f - throttle))
        Log.d("rangeScale", rangeScale.toString())
        return if (steerValue < 0.5) {
            val leftRange = controlPreferences.steerCenter - controlPreferences.steerMin
            controlPreferences.steerCenter - (0.5f - steerValue) * 2 * leftRange * rangeScale
        } else {
            val rightRange = controlPreferences.steerMax - controlPreferences.steerCenter
            controlPreferences.steerCenter + (steerValue - 0.5f) * 2 * rightRange * rangeScale
        }
    }

    private fun normalize(v: Float): Float {
        val scaled = v / 400 + 0.5f
        val constrained = if (scaled <= 0) 0F else if (scaled > 1) 1F else scaled
        return constrained
    }

    private suspend fun sendValue(steer: Float?, throttle: Float?) {
        cycles++

        val steerValue = steer?.toInt() ?: controlPreferences.steerCenter
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
        val successRate = String.format("%.1f%%", 100f * successCount / cycles)

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        withContext(Dispatchers.Main) {
            statTextView.text = "Success: $successRate\n" +
                    "max: $maxTime\n" +
                    "V=$voltageString\n" +
                    "$errorsMap\n"

            timeSeries.appendData(DataPoint(cycles.toDouble(), response?.responseTime?.toDouble() ?: 0.0), true, 50)
        }
    }

    private fun stopSending() {
        sendingJob?.cancel()
    }
}

package com.hakito.netcar.util

import android.graphics.Color
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class ResponseTimeGraphController {

    private var graphStartTime = System.currentTimeMillis()

    val timeSeries = LineGraphSeries<DataPoint>()
        .apply {
            this.title = "Time"
            this.color = Color.BLUE
        }
    val rssiSeries = LineGraphSeries<DataPoint>()
        .apply {
            this.title = "RSSI"
            this.color = Color.GREEN
        }

    fun appendResponseTime(responseTime: Long?) {
        timeSeries.appendData(
            DataPoint(
                getGraphTime(),
                responseTime?.toDouble() ?: 0.0
            ), true, 150
        )
    }

    fun appendRssi(rssi: Int) {
        rssiSeries.appendData(
            DataPoint(
                getGraphTime(),
                -rssi.toDouble() * 3
            ), true, 150
        )
    }

    private fun getGraphTime() = (System.currentTimeMillis() - graphStartTime) / 1000.0
}
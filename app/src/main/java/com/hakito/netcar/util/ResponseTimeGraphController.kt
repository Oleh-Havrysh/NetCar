package com.hakito.netcar.util

import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class ResponseTimeGraphController {

    private var graphStartTime = System.currentTimeMillis()

    val timeSeries = LineGraphSeries<DataPoint>()

    fun appendResponseTime(responseTime: Long?) {
        timeSeries.appendData(
            DataPoint(
                getGraphTime(),
                responseTime?.toDouble() ?: 0.0
            ), true, 150
        )
    }

    private fun getGraphTime() = (System.currentTimeMillis() - graphStartTime) / 1000.0
}
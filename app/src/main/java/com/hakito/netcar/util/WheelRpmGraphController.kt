package com.hakito.netcar.util

import android.graphics.Color
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class WheelRpmGraphController {

    private var graphStartTime = System.currentTimeMillis()

    val rpmFrontLeftSeries = LineGraphSeries<DataPoint>()
        .apply {
            this.title = "FL"
            this.color = Color.RED
        }
    val rpmFrontRightSeries = LineGraphSeries<DataPoint>()
        .apply {
            this.title = "FR"
            this.color = Color.BLUE
        }
    val rpmRearSeries = LineGraphSeries<DataPoint>()
        .apply {
            this.title = "R"
            this.color = Color.GREEN
        }

    fun appendRpm(frontLeft: Int, frontRight: Int, rear: Int) {
        rpmFrontLeftSeries.appendData(
            DataPoint(
                getGraphTime(),
                frontLeft.toDouble()
            ), true, 150
        )
        rpmRearSeries.appendData(
            DataPoint(
                getGraphTime(),
                rear.toDouble()
            ), true, 150
        )
    }

    private fun getGraphTime() = (System.currentTimeMillis() - graphStartTime) / 1000.0
}
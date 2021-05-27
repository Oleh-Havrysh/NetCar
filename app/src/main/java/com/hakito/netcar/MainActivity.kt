package com.hakito.netcar

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import com.hakito.netcar.controls.ControlsInterface
import com.hakito.netcar.controls.SeparateControlsFragment
import com.hakito.netcar.controls.SingleControlsFragment
import com.hakito.netcar.sender.CarParams
import com.hakito.netcar.sender.CarResponse
import com.hakito.netcar.sender.CarSender
import com.hakito.netcar.util.ErrorsController
import com.hakito.netcar.util.ResponseTimeGraphController
import com.hakito.netcar.util.StatisticsController
import com.hakito.netcar.util.WheelRpmGraphController
import com.hakito.netcar.work.CarEnabledChecker
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.koin.android.ext.android.inject
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.math.sign

class MainActivity : BaseActivity(), DashboardFragment.OnBrightnessChangedListener {

    private var sendingJob: Job? = null

    private val sender: CarSender by inject()

    private val controlPreferences: ControlPreferences by inject()

    private val stabilizationController: StabilizationController by inject()

    private val errorsController: ErrorsController by inject()

    private val wheelRpmGraphController: WheelRpmGraphController by inject()

    private val responseTimeGraphController: ResponseTimeGraphController by inject()

    private val statisticsController: StatisticsController by inject()

    private val responseHandlingChannel = Channel<CarResponse?>(Channel.CONFLATED)

    private val batteryProcessor: BatteryProcessor by inject()

    private lateinit var wifiManager: WifiManager

    private val controls: ControlsInterface?
        get() = supportFragmentManager.findFragmentById(R.id.controlsFragmentContainer) as? ControlsInterface

    private val carEnabledChecker: CarEnabledChecker by inject()

    override val layoutRes = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = getSystemService(WifiManager::class.java)
        showControls()

        dashboardButton.setOnClickListener {
            supportFragmentManager
                .commit {
                    replace(R.id.fragmentContainer, DashboardFragment())
                    addToBackStack(null)
                }
        }

        initTimeGraph()
        initRpmGraph()

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

        gaugesCheckBox.setOnCheckedChangeListener { _, isChecked ->
            gaugesGroup.isVisible = isChecked
        }

        val wifiManager = getSystemService(WifiManager::class.java)
        wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Low latency tag")
            .acquire()

        reconnectButton.setOnClickListener {
            lifecycleScope.launchWhenCreated {
                withContext(Dispatchers.IO) { getDevices() }
                    .firstOrNull()
                    ?.also {
                        sender.carIp = it
                        Toast.makeText(this@MainActivity, "Connected $it", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
        }
    }

    suspend fun getDevices(): List<String> {
        val ipString = NetworkInterface.getNetworkInterfaces()
            .toList()
            .filterNot { it.isLoopback }
            .flatMap { it.interfaceAddresses }
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .singleOrNull()
            .toString()
            .substringAfter('/', "")
        if (ipString.isEmpty()) return emptyList()
        val prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1)

        return coroutineScope {
            (0..254)
                .map {
                    async {
                        val testIp = prefix + it.toString()
                        val address = InetAddress.getByName(testIp)
                        if (address.isReachable(250) && testIp != ipString) testIp
                        else null
                    }
                }
                .awaitAll()
                .toList()
                .filterNotNull()
        }
    }

    override fun onStart() {
        super.onStart()
        carEnabledChecker.onAppStart()
    }

    override fun onStop() {
        carEnabledChecker.onAppStop()
        super.onStop()
    }

    override fun onDestroy() {
        batteryProcessor.onBatteryLow = null
        super.onDestroy()
    }

    private fun showControls() {
        val fragment: Fragment = when (controlPreferences.controlType) {
            ControlsType.SEPARATE -> SeparateControlsFragment()
            ControlsType.SINGLE -> SingleControlsFragment()
        }
        supportFragmentManager.commitNow { replace(R.id.controlsFragmentContainer, fragment) }
    }

    override fun onBrightnessChanged(brightness: Float) {
        adjustBrightness()
    }

    private fun adjustBrightness() {
        val brightness = (controlPreferences.backgroundBrightness * 255).toInt()
        val color = Color.rgb(brightness, brightness, brightness)
        window.setBackgroundDrawable(ColorDrawable(color))

        val linesColor =
            if (controlPreferences.backgroundBrightness < 0.5) Color.WHITE else Color.BLACK
        controls?.setColor(linesColor)
    }


    private fun initTimeGraph() {
        timeGraph.apply {
            addSeries(responseTimeGraphController.timeSeries)
            addSeries(responseTimeGraphController.rssiSeries)
            legendRenderer.isVisible = true
            gridLabelRenderer.isHorizontalLabelsVisible = false
            viewport.apply {
                isXAxisBoundsManual = true
                setMinX(0.0)
                setMaxX(3.0)
                isYAxisBoundsManual = true
                setMinY(0.0)
                setMaxY(controlPreferences.requestTimeout.toDouble())
            }
        }
    }

    private fun initRpmGraph() {
        rpmGraph.apply {
            addSeries(wheelRpmGraphController.rpmFrontLeftSeries)
            //addSeries(rpmFrontRightSeries)
            addSeries(wheelRpmGraphController.rpmRearSeries)
            addSeries(wheelRpmGraphController.throttleSeries)
            legendRenderer.isVisible = true
            gridLabelRenderer.isHorizontalLabelsVisible = false
            viewport.apply {
                isXAxisBoundsManual = true
                setMinX(0.0)
                setMaxX(3.0)
                isYAxisBoundsManual = true
                setMinY(0.0)
                setMaxY(2500.0)
            }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        adjustBrightness()
        controls?.resetValues()
        startSending()
    }

    override fun onPause() {
        super.onPause()
        stopSending()
    }

    private fun startSending() {
        sendingJob = launch(Dispatchers.IO) {
            while (true) {
                try {
                    val throttle =
                        (controls?.getThrottle() ?: 0f)
                            .times(controlPreferences.throttleMax)
                            .let(stabilizationController::calcThrottle)
                            .times(ServoConstants.AMPLITUDE)
                            .let { it + it.sign * ServoConstants.AMPLITUDE * controlPreferences.throttleDeadzoneCompensation }
                            .plus(ServoConstants.CENTER)
                            .toInt()
                    val steer = (controls?.getSteer() ?: 0f)
                        .let(stabilizationController::calcSteer)
                        .times(if (controlPreferences.invertSteer) -1 else 1)
                        .run { mapSteer(this) }
                    sendValue(steer, throttle)
                } catch (e: IOException) {
                    e.printStackTrace()
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

    private suspend fun sendValue(steer: Float?, throttle: Int) {
        val steerValue = steer?.toInt() ?: controlPreferences.steerCenter.toServoValue().toInt()
        statisticsController.handleInputParams(steerValue = steerValue, throttleValue = throttle)
        val response =
            try {
                sender.send(
                    CarParams(
                        steerValue,
                        throttle
                    )
                )
                    .also { statisticsController.onRequestPerformed() }
            } catch (e: IOException) {
                errorsController.onError(e)
                null
            }

        response?.sensors?.also(stabilizationController::onSensorsReceived)
        responseHandlingChannel.offer(response)
        launch(Dispatchers.Main) {
            wheelRpmGraphController.appendThrottle((throttle - ServoConstants.CENTER) * 5)
        }
    }

    private fun onResponse(response: CarResponse?) {
        statTextView.text = statisticsController.getText()

        response?.sensors?.voltage?.let {
            val battery = batteryProcessor.processRawVoltage(it)
            batteryTextView.text = battery.text
            batteryTextView.setTextColor(battery.color)
        }

        responseTimeGraphController.appendResponseTime(response?.responseTime)
        if (response != null) {
            statisticsController.handleRpm(response.sensors.frontLeftRpm)
            wheelRpmGraphController.appendRpm(
                frontLeft = response.sensors.frontLeftRpm,
                frontRight = response.sensors.frontRightRpm,
                rear = response.sensors.rearRpm
            )
            rpmWarningImageView.isVisible = stabilizationController.isStabilizationWarning()
        }
    }

    private fun stopSending() {
        sendingJob?.cancel()
    }
}

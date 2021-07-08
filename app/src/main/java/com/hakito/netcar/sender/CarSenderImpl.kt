package com.hakito.netcar.sender

import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.delay
import okhttp3.HttpUrl
import okhttp3.internal.closeQuietly
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.net.Socket
import kotlin.system.measureTimeMillis

class CarSenderImpl() : CarSender {

    override var carIp = "192.168.4.1"

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var input: InputStream? = null
    private var reader: BufferedReader? = null

    override suspend fun send(params: CarParams): CarResponse {
        if (socket == null || socket?.isClosed == true || socket?.isConnected != true) {
            socket = Socket(carIp, 81)

            writer = socket!!.getOutputStream()
                .bufferedWriter()
            input = socket!!.getInputStream()
            reader = input!!
                .bufferedReader()
            Log.d("qaz", "new socket created")
        }
        try {
            socket?.let { socket ->
                val time = measureTimeMillis {
                    writer!!.write("t=${params.throttle}s=${params.steer}c=${params.steer + params.throttle}")
                    writer!!.flush()

                    Log.d("qaz", "Response ${reader!!.readLine()}")
                    reader!!.read()
                }
                delay(30)
                return CarResponse(time, Sensors(0, 0, 0, 0))
            }
        } catch (t: Throwable) {
            socket?.closeQuietly()
            socket = null
            throw t
        }
        return CarResponse(1000, Sensors(0, 0, 0, 0))
    }

    override suspend fun ping(): Boolean {
/*        val pingRequst = Request.Builder().url("http://$carIp:80/ping").build()
        return runCatching { client.newCall(pingRequst).execute() }.isSuccess*/
        return true
    }

    @VisibleForTesting
    fun buildUrl(params: CarParams): HttpUrl = HttpUrl.Builder()
        .scheme("http")
        .host(carIp)
        .port(81)
        .addPathSegments("car")
        .addQueryParameter("steer", params.steer.toString())
        .addQueryParameter("throttle", params.throttle.toString())
        .build()

    companion object {
        private const val VOLTAGE_RAW_MAX = 1023
    }
}
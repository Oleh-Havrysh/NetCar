package com.hakito.netcar.sender

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.hakito.netcar.ControlPreferences
import kotlinx.coroutines.delay
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.Socket
import java.util.concurrent.TimeUnit

class CarSenderImpl(preferences: ControlPreferences) : CarSender {

    private val client = OkHttpClient.Builder()
        .callTimeout(preferences.requestTimeout.toLong(), TimeUnit.MILLISECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val gson = Gson()

    private var socket: Socket? = null

    override suspend fun send(params: CarParams): CarResponse {
        val request = Request.Builder()
            .url(buildUrl(params))
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val time = response.receivedResponseAtMillis() - response.sentRequestAtMillis()
            return CarResponse(
                time,
                gson.fromJson(response.body()!!.string(), Sensors::class.java)
            )
        }

        throw IOException("Request failed")
    }

    suspend fun sendViaSocket(params: CarParams): CarResponse {
        if (socket?.isConnected != true) {
            socket?.close()
            socket = Socket("192.168.4.1", 81).apply {
                this.keepAlive = true
                this.tcpNoDelay = true
            }
        }
        val writer = socket!!.getOutputStream().writer()
        writer.write("s=${params.steer}t=${params.throttle}\n")
        writer.flush()
        delay(10)

        val reader = socket!!.getInputStream().bufferedReader()
        val response = reader.readLine()
        return CarResponse(
            1,
            gson.fromJson(response, Sensors::class.java)
        )
    }

    override suspend fun ping(): Boolean {
        val pingRequst = Request.Builder().url("http://192.168.4.1:80/ping").build()
        return runCatching { client.newCall(pingRequst).execute() }.isSuccess
    }

    @VisibleForTesting
    fun buildUrl(params: CarParams): HttpUrl = HttpUrl.Builder()
        .scheme("http")
        .host("192.168.4.1")
        .port(81)
        .addPathSegments("car")
        .addQueryParameter("steer", params.steer.toString())
        .addQueryParameter("throttle", params.throttle.toString())
        .build()

    companion object {
        private const val VOLTAGE_RAW_MAX = 1023
    }
}
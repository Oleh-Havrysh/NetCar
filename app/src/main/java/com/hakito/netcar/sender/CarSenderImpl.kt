package com.hakito.netcar.sender

import androidx.annotation.VisibleForTesting
import com.hakito.netcar.ControlPreferences
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class CarSenderImpl(preferences: ControlPreferences) : CarSender {

    private val client = OkHttpClient.Builder()
        .callTimeout(preferences.requestTimeout, TimeUnit.MILLISECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    override suspend fun send(params: CarParams): CarResponse {
        val request = Request.Builder()
            .url(buildUrl(params))
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val voltageRaw = parseVoltageRaw(response.body()!!.string())
            val time = response.receivedResponseAtMillis() - response.sentRequestAtMillis()
            return CarResponse(voltageRaw, time)
        }

        throw IOException("Request failed")
    }

    //TODO: use some response format e.g. JSON
    private fun parseVoltageRaw(response: String): Float {
        val rawVoltage = response.split('=')[1].toInt()
        val voltage = rawVoltage.toFloat() / VOLTAGE_RAW_MAX
        return voltage
    }

    @VisibleForTesting
    fun buildUrl(params: CarParams): HttpUrl {
        return HttpUrl.Builder()
            .scheme("http")
            .host("192.168.4.1")
            .port(81)
            .addPathSegments("car")
            .addQueryParameter("steer", params.steer.toString())
            .addQueryParameter("throttle", params.throttle.toString())
            .build()
    }

    companion object {
        private const val VOLTAGE_RAW_MAX = 1023
    }
}
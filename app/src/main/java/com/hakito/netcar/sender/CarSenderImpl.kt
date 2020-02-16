package com.hakito.netcar.sender

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.hakito.netcar.ControlPreferences
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class CarSenderImpl(preferences: ControlPreferences) : CarSender {

    private val client = OkHttpClient.Builder()
        .callTimeout(preferences.requestTimeout.toLong(), TimeUnit.MILLISECONDS)
        /*.addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })*/
        .build()

    private val gson = Gson()

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

    override suspend fun getImage(): Bitmap {
        val request = Request.Builder()
            .url("http://192.168.4.1:80/camera")
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            return BitmapFactory.decodeStream(response.body()!!.byteStream())
        }
        throw IOException("Request failed")
    }

    @VisibleForTesting
    fun buildUrl(params: CarParams): HttpUrl = HttpUrl.Builder()
        .scheme("http")
        .host("192.168.4.1")
        .port(80)
        .addPathSegments("car")
        .addQueryParameter("steer", params.steer.toString())
        .addQueryParameter("throttle", params.throttle.toString())
        .addQueryParameter("light", params.light.toString())
        .build()

    companion object {
        private const val VOLTAGE_RAW_MAX = 1023
    }
}
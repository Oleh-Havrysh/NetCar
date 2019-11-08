package com.hakito.netcar.sender

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.VisibleForTesting
import com.hakito.netcar.ControlPreferences
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

class CarSenderImpl(preferences: ControlPreferences) : CarSender {

    private val client = OkHttpClient.Builder()
        .callTimeout(preferences.requestTimeout, TimeUnit.MILLISECONDS)
        .socketFactory(CustomSocketFactory())
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    override suspend fun send(params: CarParams): CarResponse {
        val request = Request.Builder()
            .url(buildUrl(params))
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseString = response.body()!!.string()
            val voltageRaw = runCatching { parseVoltageRaw(responseString) }.getOrNull() ?: 0f
            val rpm = try {
                parseRpm(responseString)
            } catch (e: Exception) {
                0
            }
            val time = response.receivedResponseAtMillis() - response.sentRequestAtMillis()
            return CarResponse(voltageRaw, time, rpm)
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

    private fun parseRpm(response: String): Int {
        val fields = response.split(',')
        return fields[1].split('=')[1].toInt()
    }

    //TODO: use some response format e.g. JSON
    private fun parseVoltageRaw(response: String): Float {
        val fields = response.split(',')
        val rawVoltage = fields[0].split('=')[1].toInt()
        val voltage = rawVoltage.toFloat() / VOLTAGE_RAW_MAX
        return voltage
    }

    @VisibleForTesting
    fun buildUrl(params: CarParams): HttpUrl = HttpUrl.Builder()
        .scheme("http")
        .host("192.168.4.1")
        .port(80)
        .addPathSegments("car")
        .addQueryParameter("steer", params.steer.toString())
        .addQueryParameter("throttle", params.throttle.toString())
        .build()


    private class CustomSocketFactory : SocketFactory() {

        override fun createSocket() = Socket().also(::setupSocket)

        override fun createSocket(host: String, port: Int) = Socket(host, port).also(::setupSocket)

        override fun createSocket(address: InetAddress, port: Int) =
            Socket(address, port).also(::setupSocket)

        override fun createSocket(
            host: String, port: Int,
            clientAddress: InetAddress, clientPort: Int
        ) = Socket(host, port, clientAddress, clientPort).also(::setupSocket)

        override fun createSocket(
            address: InetAddress, port: Int,
            clientAddress: InetAddress, clientPort: Int
        ) = Socket(address, port, clientAddress, clientPort).also(::setupSocket)

        private fun setupSocket(socket: Socket) {
            socket.keepAlive = true
            socket.tcpNoDelay = true
        }
    }

    companion object {
        private const val VOLTAGE_RAW_MAX = 1023
    }
}
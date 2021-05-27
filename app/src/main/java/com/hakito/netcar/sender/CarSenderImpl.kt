package com.hakito.netcar.sender

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.hakito.netcar.ControlPreferences
import kotlinx.coroutines.delay
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.BufferedWriter
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

class CarSenderImpl(preferences: ControlPreferences) : CarSender {

    override var carIp = "192.168.4.1"

    private val client = OkHttpClient.Builder()
        .callTimeout(preferences.requestTimeout.toLong(), TimeUnit.MILLISECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .socketFactory(object : SocketFactory() {
            override fun createSocket(host: String?, port: Int): Socket {
                TODO("Not yet implemented")
            }

            override fun createSocket(
                host: String?,
                port: Int,
                localHost: InetAddress?,
                localPort: Int
            ): Socket {
                TODO("Not yet implemented")
            }

            override fun createSocket(host: InetAddress?, port: Int): Socket {
                TODO("Not yet implemented")
            }

            override fun createSocket(
                address: InetAddress?,
                port: Int,
                localAddress: InetAddress?,
                localPort: Int
            ): Socket {
                TODO("Not yet implemented")
            }

            override fun createSocket(): Socket {
                return Socket()
                    .apply {
                        keepAlive = true
                        tcpNoDelay = true
                        receiveBufferSize = 1024
                        sendBufferSize = 1024
                    }
                    .apply {
                        Log.d(
                            "qaz",
                            "ka = $keepAlive, nd = $tcpNoDelay, $receiveBufferSize $sendBufferSize"
                        )
                    }
            }
        })
        .build()

    private val gson = Gson()

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null

    override suspend fun send(params: CarParams): CarResponse {
        if (socket == null || socket?.isClosed == true || socket?.isConnected != true) {
            socket = Socket(carIp, 81).apply {
/*                this.tcpNoDelay = true
                this.keepAlive = true
                this.sendBufferSize = 64
                this.receiveBufferSize = 64*/
            }
            writer = socket!!.getOutputStream()
                .bufferedWriter()
            Log.d("qaz", "new socket created")
        }
        socket?.let { socket ->
            writer!!.write("t=${params.throttle}s=${params.steer}\n")
            writer!!.flush()
            delay(30)
        }
        return CarResponse(10, Sensors(0, 0, 0, 0))

        /*val request = Request.Builder()
            .url(buildUrl(params))
            .addHeader("Connection",  "Keep-Alive")
            .get()
            .build()

        val response = suspendCancellableCoroutine<Response> {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    it.resume(response)
                }
            })
        }
        if (response.isSuccessful) {
            val time = response.receivedResponseAtMillis - response.sentRequestAtMillis
            return CarResponse(
                time,
                gson.fromJson(
                    com.google.gson.stream.JsonReader(response.body!!.charStream()),
                    Sensors::class.java
                )
            )
        }

        throw IOException("Request failed")*/
    }

    override suspend fun ping(): Boolean {
        val pingRequst = Request.Builder().url("http://$carIp:80/ping").build()
        return runCatching { client.newCall(pingRequst).execute() }.isSuccess
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
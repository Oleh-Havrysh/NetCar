package com.hakito.netcar.sender

import android.util.Log
import okhttp3.internal.closeQuietly
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class CarSenderUdpImpl() : CarSender {

    override var carIp = "192.168.4.1"

    private var socket: DatagramSocket? = null
    val sendBuffer = ByteArray(64)
    val sendPacket = DatagramPacket(sendBuffer, sendBuffer.size)
    val responseBuffer = ByteArray(32)
    val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)

    override suspend fun send(params: CarParams): CarResponse {
        if (socket == null || socket?.isClosed == true || socket?.isConnected != true) {
            socket?.closeQuietly()
            socket = DatagramSocket()
            socket!!.soTimeout = 100
            socket!!.connect(InetAddress.getByName(carIp), 4210)
            Log.d("qaz", "new socket created")
        }
        try {
            val request = "t${params.throttle}s${params.steer}c${params.steer + params.throttle}"
            request.toByteArray().copyInto(sendBuffer  )
            socket!!.send(sendPacket)

            socket!!.receive(responsePacket)
            val response = String(responsePacket.data, 0, responsePacket.length)
            val voltage = response.substringAfter("v:").substringBefore('\r').toIntOrNull() ?: 0
            Log.d("qaz", response)
            return CarResponse(1000, Sensors(0, 0, 0, voltage))
        } catch (t: Throwable) {
            socket?.closeQuietly()
            socket = null
            throw t
        }
        return CarResponse(1000, Sensors(0, 0, 0, 0))
    }

    override suspend fun ping(): Boolean {
        return true
    }
}
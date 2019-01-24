package com.hakito.netcar.sender

interface CarSender {

    suspend fun send(params: CarParams): CarResponse
}

data class CarParams(val steer: Int, var throttle: Int)

data class CarResponse(val voltage: Float, val responseTime: Long)
package com.hakito.netcar.sender

interface CarSender {

    suspend fun send(params: CarParams): CarResponse

    suspend fun ping(): Boolean
}

data class CarParams(val steer: Int, var throttle: Int)

data class CarResponse(val responseTime: Long, val sensors: Sensors)

data class Sensors(
    val frontLeftRpm: Int,
    val frontRightRpm: Int,
    val rearRpm: Int,
    val voltage: Int
)
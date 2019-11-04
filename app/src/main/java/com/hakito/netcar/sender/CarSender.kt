package com.hakito.netcar.sender

import android.graphics.Bitmap

interface CarSender {

    suspend fun send(params: CarParams): CarResponse

    suspend fun getImage(): Bitmap
}

data class CarParams(val steer: Int, var throttle: Int)

data class CarResponse(val voltageRaw: Float, val responseTime: Long, val rpm: Int)

data class Sensors(val ax: Int, val ay: Int, val az: Int, val gx: Int, val gy: Int, val gz: Int)
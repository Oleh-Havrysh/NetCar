package com.hakito.netcar.entity

data class CarConfig(
    val steerMin: Float,
    val steerCenter: Float,
    val steerMax: Float,
    val invertSteer: Boolean,
    val throttleMax: Float,
    val voltageMultiplier: Float,
    val throttleDeadzoneCompensation: Float,
    val cruiseGain: Float,
    val preventSlipping: Boolean,
    val cruiseSpeedDiff: Float,
    val cruiseDiffDependsOnThrottle: Boolean,
    val speedDependantSteerLimit: Float
) {
    constructor() : this(
        steerMin = 0f,
        steerCenter = 0f,
        steerMax = 0f,
        invertSteer = false,
        throttleMax = 0f,
        voltageMultiplier = 0f,
        throttleDeadzoneCompensation = 0f,
        cruiseGain = 0f,
        preventSlipping = false,
        cruiseSpeedDiff = 0f,
        cruiseDiffDependsOnThrottle = false,
        speedDependantSteerLimit = 0f
    )
}
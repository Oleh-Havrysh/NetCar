package com.hakito.netcar.cloud

class FirebaseCarConfig(
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
)
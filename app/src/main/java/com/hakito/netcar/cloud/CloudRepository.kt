package com.hakito.netcar.cloud

import com.google.firebase.database.FirebaseDatabase
import com.hakito.netcar.entity.CarConfig

class CloudRepository {

    private val database = FirebaseDatabase.getInstance()

    suspend fun loadConfig(name: String): CarConfig? =
        database.getReference("configs/$name").getValue()

    suspend fun saveConfig(name: String, config: CarConfig) =
        database.getReference("configs/$name")
            .setValue(config.toFirebase())
            .await()

    suspend fun getConfigNames(): List<String> =
        database.getReference("configs")
            .getSnapshot()
            ?.children
            ?.map { it.key!! }
            ?: emptyList()

    private fun CarConfig.toFirebase() = FirebaseCarConfig(
        steerMin = steerMin,
        steerCenter = steerCenter,
        steerMax = steerMax,
        invertSteer = invertSteer,
        throttleMax = throttleMax,
        voltageMultiplier = voltageMultiplier,
        throttleDeadzoneCompensation = throttleDeadzoneCompensation,
        cruiseGain = cruiseGain,
        preventSlipping = preventSlipping,
        cruiseSpeedDiff = cruiseSpeedDiff,
        cruiseDiffDependsOnThrottle = cruiseDiffDependsOnThrottle,
        speedDependantSteerLimit = speedDependantSteerLimit
    )

    private fun FirebaseCarConfig.toDomain() = CarConfig(
        steerMin = steerMin,
        steerCenter = steerCenter,
        steerMax = steerMax,
        invertSteer = invertSteer,
        throttleMax = throttleMax,
        voltageMultiplier = voltageMultiplier,
        throttleDeadzoneCompensation = throttleDeadzoneCompensation,
        cruiseGain = cruiseGain,
        preventSlipping = preventSlipping,
        cruiseSpeedDiff = cruiseSpeedDiff,
        cruiseDiffDependsOnThrottle = cruiseDiffDependsOnThrottle,
        speedDependantSteerLimit = speedDependantSteerLimit
    )

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
}
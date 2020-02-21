package com.hakito.netcar.cloud

import com.hakito.netcar.entity.CarConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class CloudRepository {

    private val api = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .sslSocketFactory(TLSSocketFactory(), object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                .build()
        )
        .baseUrl("https://carstabilizationsystem.firebaseio.com/")
        .build()
        .create<CloudApi>()

    suspend fun loadConfig(name: String) =
        api.getConfig(name)
            ?.toDomain()

    suspend fun saveConfig(name: String, config: CarConfig) =
        api.putConfig(config.toFirebase(), name)

    suspend fun getConfigNames() =
        api.getAllConfigs()
            .keys
            .toList()

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

    companion object {

        val instance by lazy { CloudRepository() }
    }
}
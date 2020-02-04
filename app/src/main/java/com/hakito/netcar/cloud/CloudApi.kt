package com.hakito.netcar.cloud

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface CloudApi {

    @GET("configs/{name}.json")
    suspend fun getConfig(@Path("name") name: String): FirebaseCarConfig?

    @GET("configs.json")
    suspend fun getAllConfigs(): Map<String, FirebaseCarConfig>

    @PUT("configs/{name}.json")
    suspend fun putConfig(@Body config: FirebaseCarConfig, @Path("name") name: String)
}
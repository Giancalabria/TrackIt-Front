package com.trackit.data.network

import com.trackit.data.model.OrsRequest
import com.trackit.data.model.OrsResponse
import com.trackit.data.model.PhotonResponse
import retrofit2.http.*

interface PhotonApi {
    @GET("https://photon.komoot.io/api/")
    suspend fun searchAddress(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null
    ): PhotonResponse
}

interface OpenRouteServiceApi {
    @POST("https://api.openrouteservice.org/v2/directions/driving-car/geojson")
    suspend fun getRoute(
        @Header("Authorization") apiKey: String,
        @Body request: OrsRequest
    ): OrsResponse
}

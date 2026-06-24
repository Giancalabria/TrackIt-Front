package com.trackit.data.network

import com.trackit.data.model.OrsOptimizationRequest
import com.trackit.data.model.OrsOptimizationResponse
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

    @GET("https://photon.komoot.io/reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("limit") limit: Int = 1
    ): PhotonResponse
}

interface OpenRouteServiceApi {
    @POST("https://api.openrouteservice.org/v2/directions/driving-car/geojson")
    suspend fun getRoute(
        @Header("Authorization") apiKey: String,
        @Body request: OrsRequest
    ): OrsResponse
}

interface OpenRouteServiceOptimizationApi {
    @POST("https://api.openrouteservice.org/optimization")
    suspend fun optimizeRoutes(
        @Header("Authorization") apiKey: String,
        @Body request: OrsOptimizationRequest
    ): OrsOptimizationResponse
}

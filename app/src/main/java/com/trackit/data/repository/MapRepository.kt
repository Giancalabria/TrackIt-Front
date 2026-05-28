package com.trackit.data.repository

import com.trackit.data.model.OrsRequest
import com.trackit.data.model.OrsResponse
import com.trackit.data.model.PhotonResponse
import com.trackit.data.network.OpenRouteServiceApi
import com.trackit.data.network.PhotonApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface IMapRepository {
    suspend fun searchAddress(query: String): PhotonResponse
    suspend fun getRoute(startLon: Double, startLat: Double, endLon: Double, endLat: Double): OrsResponse
}

class MapRepository private constructor() : IMapRepository {

    private val photonApi = Retrofit.Builder()
        .baseUrl("https://photon.komoot.io/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PhotonApi::class.java)

    private val orsApi = Retrofit.Builder()
        .baseUrl("https://api.openrouteservice.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenRouteServiceApi::class.java)

    private val orsApiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjQzNDFiOTE0Mjk5MzQ5YWI5MTFmNDhkZmM5NDQ2MWYxIiwiaCI6Im11cm11cjY0In0="

    override suspend fun searchAddress(query: String): PhotonResponse = withContext(Dispatchers.IO) {
        photonApi.searchAddress(query)
    }

    override suspend fun getRoute(
        startLon: Double,
        startLat: Double,
        endLon: Double,
        endLat: Double
    ): OrsResponse = withContext(Dispatchers.IO) {
        val request = OrsRequest(
            coordinates = listOf(
                listOf(startLon, startLat),
                listOf(endLon, endLat)
            )
        )
        orsApi.getRoute(orsApiKey, request)
    }

    companion object {
        @Volatile
        private var instance: MapRepository? = null

        fun getInstance(): MapRepository {
            return instance ?: synchronized(this) {
                instance ?: MapRepository().also { instance = it }
            }
        }
    }
}

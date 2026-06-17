package com.trackit.data.repository

import com.trackit.data.model.OrsRequest
import com.trackit.data.model.OrsResponse
import com.trackit.data.model.PhotonResponse
import com.trackit.data.network.OpenRouteServiceApi
import com.trackit.data.network.PhotonApi
import com.trackit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface IMapRepository {
    suspend fun searchAddress(query: String, lat: Double? = null, lon: Double? = null): PhotonResponse
    suspend fun getRoute(startLon: Double, startLat: Double, endLon: Double, endLat: Double): OrsResponse

    /** Multi-stop route. [coordinates] is an ordered list of [lon, lat] points. */
    suspend fun getRouteThrough(coordinates: List<List<Double>>): OrsResponse
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

    override suspend fun searchAddress(query: String, lat: Double?, lon: Double?): PhotonResponse =
        withContext(Dispatchers.IO) {
            photonApi.searchAddress(
                query = query,
                lat = lat,
                lon = lon
            )
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
        orsApi.getRoute(BuildConfig.ORS_API_KEY, request)
    }

    override suspend fun getRouteThrough(coordinates: List<List<Double>>): OrsResponse =
        withContext(Dispatchers.IO) {
            orsApi.getRoute(BuildConfig.ORS_API_KEY, OrsRequest(coordinates = coordinates))
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

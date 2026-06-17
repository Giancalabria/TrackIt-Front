package com.trackit.data.repository

import android.content.Context
import com.trackit.data.local.TrackItDatabase
import com.trackit.data.sync.NetworkConnectivityObserver
import io.github.jan.supabase.SupabaseClient

/**
 * Minimal service locator. Holds the Supabase client plus the singleton, offline-first
 * repositories that the whole app shares (Room is the single source of truth).
 *
 * [init] must be called once from [com.trackit.TrackItApp.onCreate].
 */
object SupabaseLocator {

    lateinit var client: SupabaseClient
        private set

    private lateinit var appContext: Context
    private lateinit var database: TrackItDatabase

    val authRepository: IAuthRepository by lazy { SupabaseAuthRepository(client) }

    val packageRepository: IPackageRepository by lazy {
        OfflineFirstPackageRepository(client, database, appContext)
    }

    val fleetRepository: IFleetRepository by lazy {
        OfflineFirstFleetRepository(client, database, appContext)
    }

    val networkObserver: NetworkConnectivityObserver by lazy {
        NetworkConnectivityObserver(appContext)
    }

    fun init(context: Context, supabaseClient: SupabaseClient) {
        appContext = context.applicationContext
        client = supabaseClient
        database = TrackItDatabase.getInstance(appContext)
    }
}

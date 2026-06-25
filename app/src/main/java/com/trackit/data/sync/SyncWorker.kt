package com.trackit.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trackit.data.local.TrackItDatabase
import com.trackit.data.local.mapper.toDomain
import com.trackit.data.local.mapper.toEntity
import com.trackit.data.model.Package
import com.trackit.data.model.Truck
import com.trackit.data.model.hasRouteStart
import com.trackit.data.repository.SupabaseLocator
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val database = TrackItDatabase.getInstance(appContext)
    private val supabase = SupabaseLocator.client

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncWorker", "Starting synchronization...")

            // 1. Sync Packages
            syncPackages()

            // 2. Sync Trucks
            syncTrucks()

            Log.d("SyncWorker", "Synchronization completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Synchronization failed", e)
            // Cap retries so a persistent failure (e.g. schema mismatch) doesn't loop forever.
            if (runAttemptCount >= MAX_RETRIES) Result.failure() else Result.retry()
        }
    }

    private suspend fun syncPackages() {
        Log.d("SyncWorker", "Syncing packages...")
        
        // A. Upload local changes
        val pending = database.packageDao().getPendingSync()
        for (entity in pending) {
            if (entity.deletedLocally) {
                supabase.postgrest["packages"].delete {
                    filter { eq("id", entity.id) }
                }
                database.packageDao().deleteById(entity.id)
            } else {
                val domain = entity.toDomain()
                supabase.postgrest["packages"].upsert(domain)
                database.packageDao().markSynced(listOf(entity.id))
            }
        }

        // B. Pull remote changes.
        // IMPORTANT: never overwrite rows that are still pending locally, otherwise an
        // offline edit made during this sync window would be lost (and its pendingSync
        // flag cleared). Those rows are kept and pushed on the next run (last-write-wins).
        val remotePackages = supabase.postgrest["packages"]
            .select().decodeList<Package>()

        // C. Clean up local records that no longer exist on the server
        val remoteIds = remotePackages.map { it.id }
        if (remoteIds.isNotEmpty()) {
            database.packageDao().deleteRemovedFromRemote(remoteIds)
        }

        val stillPendingIds = database.packageDao().getPendingSync().map { it.id }.toSet()
        val toUpsert = remotePackages
            .filterNot { it.id in stillPendingIds }
            .map { it.toEntity(pendingSync = false) }

        if (toUpsert.isNotEmpty()) {
            database.packageDao().upsertAll(toUpsert)
        }
    }

    private suspend fun syncTrucks() {
        Log.d("SyncWorker", "Syncing trucks...")
        
        // A. Upload local changes
        val pending = database.truckDao().getPendingSync()
        for (entity in pending) {
            val domain = entity.toDomain()
            supabase.postgrest["trucks"].upsert(domain)
            database.truckDao().markSynced(listOf(entity.id))
        }

        // B. Pull remote changes
        val remoteTrucks = supabase.postgrest["trucks"]
            .select().decodeList<Truck>()

        // C. Clean up local trucks
        val remoteIds = remoteTrucks.map { it.id }
        if (remoteIds.isNotEmpty()) {
            database.truckDao().deleteRemovedFromRemote(remoteIds)
        }

        val stillPendingIds = database.truckDao().getPendingSync().map { it.id }.toSet()
        val toUpsert = remoteTrucks
            .filter { it.hasRouteStart() }
            .filterNot { it.id in stillPendingIds }
            .map { it.toEntity(pendingSync = false) }

        if (toUpsert.isNotEmpty()) {
            database.truckDao().upsertAll(toUpsert)
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
    }
}

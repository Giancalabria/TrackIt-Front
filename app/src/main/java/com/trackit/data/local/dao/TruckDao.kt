package com.trackit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.trackit.data.local.entity.TruckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TruckDao {

    @Query("SELECT * FROM trucks")
    fun observeAll(): Flow<List<TruckEntity>>

    @Query("SELECT * FROM trucks WHERE driverId = :driverId LIMIT 1")
    suspend fun getByDriverId(driverId: String): TruckEntity?

    @Query("SELECT * FROM trucks WHERE id = :id")
    suspend fun getById(id: String): TruckEntity?

    @Query("SELECT * FROM trucks WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<TruckEntity>

    @Upsert
    suspend fun upsert(entity: TruckEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TruckEntity>)

    @Query("UPDATE trucks SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM trucks WHERE id NOT IN (:ids) AND pendingSync = 0")
    suspend fun deleteRemovedFromRemote(ids: List<String>)
}

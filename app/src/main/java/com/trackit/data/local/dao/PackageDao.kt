package com.trackit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.trackit.data.local.entity.PackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {

    @Query("SELECT * FROM packages WHERE deletedLocally = 0")
    fun observeAll(): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE id = :id AND deletedLocally = 0")
    fun observeById(id: String): Flow<PackageEntity?>

    @Query("SELECT * FROM packages WHERE id = :id")
    suspend fun getById(id: String): PackageEntity?

    @Query("SELECT * FROM packages WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<PackageEntity>

    @Upsert
    suspend fun upsert(entity: PackageEntity)

    @Upsert
    suspend fun upsertAll(entities: List<PackageEntity>)

    @Query("UPDATE packages SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM packages WHERE id = :id")
    suspend fun deleteById(id: String)
}

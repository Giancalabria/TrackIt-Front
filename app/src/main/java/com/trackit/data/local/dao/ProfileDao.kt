package com.trackit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.trackit.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<ProfileEntity>

    @Upsert
    suspend fun upsert(entity: ProfileEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ProfileEntity>)

    @Query("UPDATE profiles SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}

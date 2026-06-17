package com.trackit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.trackit.data.local.dao.PackageDao
import com.trackit.data.local.dao.ProfileDao
import com.trackit.data.local.dao.TruckDao
import com.trackit.data.local.entity.PackageEntity
import com.trackit.data.local.entity.ProfileEntity
import com.trackit.data.local.entity.TruckEntity

@Database(
    entities = [PackageEntity::class, TruckEntity::class, ProfileEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TrackItDatabase : RoomDatabase() {

    abstract fun packageDao(): PackageDao
    abstract fun truckDao(): TruckDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var instance: TrackItDatabase? = null

        fun getInstance(context: Context): TrackItDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackItDatabase::class.java,
                    "trackit.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

package com.wifimapper.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wifimapper.data.database.entity.AccessPointEntity
import com.wifimapper.data.database.entity.MeasurementEntity
import com.wifimapper.data.database.entity.SessionEntity
import com.wifimapper.data.database.entity.TrajectoryPointEntity

@Database(
    entities = [
        SessionEntity::class,
        MeasurementEntity::class,
        TrajectoryPointEntity::class,
        AccessPointEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}

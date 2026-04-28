package com.wifimapper.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wifimapper.data.database.entity.AccessPointEntity
import com.wifimapper.data.database.entity.MeasurementEntity
import com.wifimapper.data.database.entity.SessionEntity
import com.wifimapper.data.database.entity.TrajectoryPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionById(id: String): Flow<SessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrajectoryPoint(point: TrajectoryPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccessPoint(accessPoint: AccessPointEntity)

    @Query("SELECT * FROM measurements WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getMeasurementsForSession(sessionId: String): List<MeasurementEntity>

    @Query("SELECT * FROM trajectory WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getTrajectoryForSession(sessionId: String): List<TrajectoryPointEntity>

    @Query("SELECT * FROM access_points WHERE sessionId = :sessionId")
    suspend fun getAccessPointsForSession(sessionId: String): List<AccessPointEntity>

    @Query("DELETE FROM measurements WHERE sessionId = :sessionId")
    suspend fun deleteMeasurementsForSession(sessionId: String)

    @Query("DELETE FROM trajectory WHERE sessionId = :sessionId")
    suspend fun deleteTrajectoryForSession(sessionId: String)

    @Query("DELETE FROM access_points WHERE sessionId = :sessionId")
    suspend fun deleteAccessPointsForSession(sessionId: String)
}

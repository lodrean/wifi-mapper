package com.wifimapper.domain.repository

import com.wifimapper.domain.model.Measurement
import com.wifimapper.domain.model.Session
import com.wifimapper.domain.model.TrajectoryPoint
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>
    fun getSessionById(id: String): Flow<Session?>
    suspend fun createSession(name: String, stepLengthMeters: Float): String
    suspend fun updateSession(session: Session)
    suspend fun deleteSession(id: String)
    suspend fun addMeasurement(measurement: Measurement)
    suspend fun addTrajectoryPoint(point: TrajectoryPoint)
    suspend fun getMeasurementsForSession(sessionId: String): List<Measurement>
    suspend fun getTrajectoryForSession(sessionId: String): List<TrajectoryPoint>
}

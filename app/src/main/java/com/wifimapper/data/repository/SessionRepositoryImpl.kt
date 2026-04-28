package com.wifimapper.data.repository

import com.wifimapper.data.database.SessionDao
import com.wifimapper.data.database.entity.AccessPointEntity
import com.wifimapper.data.database.entity.MeasurementEntity
import com.wifimapper.data.database.entity.SessionEntity
import com.wifimapper.data.database.entity.TrajectoryPointEntity
import com.wifimapper.domain.model.AccessPoint
import com.wifimapper.domain.model.Measurement
import com.wifimapper.domain.model.Session
import com.wifimapper.domain.model.TrajectoryPoint
import com.wifimapper.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SessionRepositoryImpl(
    private val dao: SessionDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<Session>> {
        return dao.getAllSessions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getSessionById(id: String): Flow<Session?> {
        return dao.getSessionById(id).map { entity ->
            entity?.let { sessionEntity ->
                val measurements = dao.getMeasurementsForSession(id)
                val trajectory = dao.getTrajectoryForSession(id)
                val accessPoints = dao.getAccessPointsForSession(id)
                sessionEntity.toDomainModel(
                    measurements = measurements.map { it.toDomainModel() },
                    trajectory = trajectory.map { it.toDomainModel() },
                    accessPoints = accessPoints.map { it.toDomainModel() }
                )
            }
        }
    }

    override suspend fun createSession(name: String, stepLengthMeters: Float): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.insertSession(
            SessionEntity(
                id = id,
                name = name,
                createdAt = now,
                updatedAt = now,
                isActive = true,
                stepLengthMeters = stepLengthMeters
            )
        )
        return id
    }

    override suspend fun updateSession(session: Session) {
        dao.updateSession(
            SessionEntity(
                id = session.id,
                name = session.name,
                createdAt = session.createdAt,
                updatedAt = System.currentTimeMillis(),
                isActive = session.isActive,
                stepLengthMeters = session.stepLengthMeters
            )
        )
    }

    override suspend fun deleteSession(id: String) {
        dao.deleteMeasurementsForSession(id)
        dao.deleteTrajectoryForSession(id)
        dao.deleteAccessPointsForSession(id)
        dao.deleteSession(id)
    }

    override suspend fun addMeasurement(measurement: Measurement) {
        dao.insertMeasurement(measurement.toEntity())
    }

    override suspend fun addTrajectoryPoint(point: TrajectoryPoint) {
        dao.insertTrajectoryPoint(point.toEntity())
    }

    override suspend fun getMeasurementsForSession(sessionId: String): List<Measurement> {
        return dao.getMeasurementsForSession(sessionId).map { it.toDomainModel() }
    }

    override suspend fun getTrajectoryForSession(sessionId: String): List<TrajectoryPoint> {
        return dao.getTrajectoryForSession(sessionId).map { it.toDomainModel() }
    }

    private fun SessionEntity.toDomainModel(
        measurements: List<Measurement> = emptyList(),
        trajectory: List<TrajectoryPoint> = emptyList(),
        accessPoints: List<AccessPoint> = emptyList()
    ) = Session(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isActive = isActive,
        stepLengthMeters = stepLengthMeters,
        measurements = measurements,
        trajectory = trajectory,
        accessPoints = accessPoints
    )

    private fun MeasurementEntity.toDomainModel() = Measurement(
        id = id,
        sessionId = sessionId,
        x = x,
        y = y,
        rssiDbm = rssiDbm,
        bssid = bssid,
        timestamp = timestamp
    )

    private fun Measurement.toEntity() = MeasurementEntity(
        id = id,
        sessionId = sessionId,
        x = x,
        y = y,
        rssiDbm = rssiDbm,
        bssid = bssid,
        ssid = "",
        timestamp = timestamp
    )

    private fun TrajectoryPointEntity.toDomainModel() = TrajectoryPoint(
        id = id,
        sessionId = sessionId,
        x = x,
        y = y,
        headingDegrees = headingDegrees,
        timestamp = timestamp,
        isStep = isStep
    )

    private fun TrajectoryPoint.toEntity() = TrajectoryPointEntity(
        id = id,
        sessionId = sessionId,
        x = x,
        y = y,
        headingDegrees = headingDegrees,
        timestamp = timestamp,
        isStep = isStep
    )

    private fun AccessPointEntity.toDomainModel() = AccessPoint(
        bssid = bssid,
        ssid = ssid,
        frequencyMHz = frequencyMHz,
        standard = standard
    )
}

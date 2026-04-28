package com.wifimapper.data.export

import com.wifimapper.domain.model.AccessPoint
import com.wifimapper.domain.model.Measurement
import com.wifimapper.domain.model.Session
import com.wifimapper.domain.model.TrajectoryPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapExportDto(
    val version: Int = 1,
    val name: String,
    @SerialName("createdAt")
    val createdAtIso: String,
    @SerialName("updatedAt")
    val updatedAtIso: String,
    val stepLengthMeters: Float,
    val accessPoints: List<AccessPointDto>,
    val measurements: List<MeasurementDto>,
    val trajectory: List<TrajectoryDto>
)

@Serializable
data class AccessPointDto(
    val bssid: String,
    val ssid: String,
    val frequencyMHz: Int,
    val standard: String = ""
)

@Serializable
data class MeasurementDto(
    val id: String,
    val x: Float,
    val y: Float,
    val rssiDbm: Int,
    val bssid: String,
    val timestamp: Long
)

@Serializable
data class TrajectoryDto(
    val x: Float,
    val y: Float,
    val headingDegrees: Float,
    val timestamp: Long,
    val isStep: Boolean = false
)

fun Session.toExportDto(): MapExportDto = MapExportDto(
    version = 1,
    name = name,
    createdAtIso = java.time.Instant.ofEpochMilli(createdAt).toString(),
    updatedAtIso = java.time.Instant.ofEpochMilli(updatedAt).toString(),
    stepLengthMeters = stepLengthMeters,
    accessPoints = accessPoints.map { it.toDto() },
    measurements = measurements.map { it.toDto() },
    trajectory = trajectory.map { it.toDto() }
)

fun AccessPoint.toDto(): AccessPointDto = AccessPointDto(
    bssid = bssid,
    ssid = ssid,
    frequencyMHz = frequencyMHz,
    standard = standard
)

fun Measurement.toDto(): MeasurementDto = MeasurementDto(
    id = id,
    x = x,
    y = y,
    rssiDbm = rssiDbm,
    bssid = bssid,
    timestamp = timestamp
)

fun TrajectoryPoint.toDto(): TrajectoryDto = TrajectoryDto(
    x = x,
    y = y,
    headingDegrees = headingDegrees,
    timestamp = timestamp,
    isStep = isStep
)

fun MapExportDto.toDomainModel(): Session = Session(
    id = java.util.UUID.randomUUID().toString(),
    name = name,
    createdAt = java.time.Instant.parse(createdAtIso).toEpochMilli(),
    updatedAt = java.time.Instant.parse(updatedAtIso).toEpochMilli(),
    isActive = false,
    stepLengthMeters = stepLengthMeters,
    accessPoints = accessPoints.map { it.toDomainModel() },
    measurements = measurements.map { it.toDomainModel() },
    trajectory = trajectory.map { it.toDomainModel() }
)

fun AccessPointDto.toDomainModel(): AccessPoint = AccessPoint(
    bssid = bssid,
    ssid = ssid,
    frequencyMHz = frequencyMHz,
    standard = standard
)

fun MeasurementDto.toDomainModel(): Measurement = Measurement(
    id = id,
    sessionId = "",
    x = x,
    y = y,
    rssiDbm = rssiDbm,
    bssid = bssid,
    timestamp = timestamp
)

fun TrajectoryDto.toDomainModel(): TrajectoryPoint = TrajectoryPoint(
    id = java.util.UUID.randomUUID().toString(),
    sessionId = "",
    x = x,
    y = y,
    headingDegrees = headingDegrees,
    timestamp = timestamp,
    isStep = isStep
)

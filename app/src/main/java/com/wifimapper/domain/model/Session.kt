package com.wifimapper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = false,
    val stepLengthMeters: Float = 0.75f,
    val accessPoints: List<AccessPoint> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val trajectory: List<TrajectoryPoint> = emptyList()
)

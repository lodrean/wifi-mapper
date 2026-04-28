package com.wifimapper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TrajectoryPoint(
    val id: String,
    val sessionId: String,
    val x: Float,
    val y: Float,
    val headingDegrees: Float,
    val timestamp: Long,
    val isStep: Boolean = false
)

package com.wifimapper.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trajectory")
data class TrajectoryPointEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val x: Float,
    val y: Float,
    val headingDegrees: Float,
    val timestamp: Long,
    val isStep: Boolean = false
)

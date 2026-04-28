package com.wifimapper.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = false,
    val stepLengthMeters: Float = 0.75f
)

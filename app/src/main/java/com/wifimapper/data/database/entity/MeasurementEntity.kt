package com.wifimapper.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val x: Float,
    val y: Float,
    val rssiDbm: Int,
    val bssid: String,
    val ssid: String,
    val timestamp: Long
)

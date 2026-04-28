package com.wifimapper.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "access_points")
data class AccessPointEntity(
    @PrimaryKey
    val bssid: String,
    val sessionId: String,
    val ssid: String,
    val frequencyMHz: Int,
    val standard: String = ""
)

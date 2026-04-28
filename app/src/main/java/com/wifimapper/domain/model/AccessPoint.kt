package com.wifimapper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AccessPoint(
    val bssid: String,
    val ssid: String,
    val frequencyMHz: Int,
    val standard: String = ""
)

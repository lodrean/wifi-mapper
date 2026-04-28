package com.wifimapper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Measurement(
    val id: String,
    val sessionId: String,
    val x: Float,
    val y: Float,
    val rssiDbm: Int,
    val bssid: String,
    val timestamp: Long
)

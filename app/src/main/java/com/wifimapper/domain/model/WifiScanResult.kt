package com.wifimapper.domain.model

data class WifiScanResult(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMHz: Int,
    val timestamp: Long
)

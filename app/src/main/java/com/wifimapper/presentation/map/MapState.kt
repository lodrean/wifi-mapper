package com.wifimapper.presentation.map

import androidx.compose.ui.geometry.Offset

 data class MapState(
    val sessionId: String = "",
    val sessionName: String = "",
    val isTracking: Boolean = false,
    val isLoading: Boolean = false,
    val currentPosition: Offset? = null,
    val currentHeading: Float = 0f,
    val stepCount: Int = 0,
    val measurements: List<MeasurementUi> = emptyList(),
    val trajectory: List<TrajectoryPointUi> = emptyList(),
    val wifiNetworks: List<WifiNetworkUi> = emptyList(),
    val selectedNetwork: String? = null,
    val errorMessage: String? = null,
    val showWifiThrottlingWarning: Boolean = false,
    val stepLengthMeters: Float = 0.75f,
    val isManualMode: Boolean = false,
    val sensorsAvailable: Boolean = true
)

 data class WifiNetworkUi(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMHz: Int
)

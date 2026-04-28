package com.wifimapper.presentation.map

import androidx.compose.ui.graphics.Color

 data class MeasurementUi(
    val x: Float,
    val y: Float,
    val rssiDbm: Int,
    val ssid: String
)

 data class TrajectoryPointUi(
    val x: Float,
    val y: Float
)

 data class MapViewport(
    val scale: Float = 50f, // pixels per meter
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

fun rssiToColor(rssiDbm: Int): Color = when {
    rssiDbm >= -50 -> Color(0xFF4CAF50) // Green
    rssiDbm >= -60 -> Color(0xFF8BC34A) // Light Green
    rssiDbm >= -70 -> Color(0xFFFFEB3B) // Yellow
    rssiDbm >= -80 -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336) // Red
}

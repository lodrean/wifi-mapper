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
    rssiDbm >= -40 -> Color(0xFF006400) // Dark Green (excellent)
    rssiDbm >= -50 -> Color(0xFF4CAF50) // Green (very good)
    rssiDbm >= -55 -> Color(0xFF8BC34A) // Light Green (good)
    rssiDbm >= -60 -> Color(0xFFCDDC39) // Lime (fairly good)
    rssiDbm >= -65 -> Color(0xFFFFEB3B) // Yellow (fair)
    rssiDbm >= -70 -> Color(0xFFFFC107) // Amber (weak)
    rssiDbm >= -75 -> Color(0xFFFF9800) // Orange (poor)
    rssiDbm >= -80 -> Color(0xFFFF5722) // Deep Orange (bad)
    rssiDbm >= -85 -> Color(0xFFF44336) // Red (very bad)
    else -> Color(0xFFB71C1C)           // Dark Red (extremely bad)
}

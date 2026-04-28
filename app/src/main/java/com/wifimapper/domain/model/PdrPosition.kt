package com.wifimapper.domain.model

data class PdrPosition(
    val x: Float,
    val y: Float,
    val headingDegrees: Float,
    val stepCount: Int,
    val isCalibrated: Boolean = false
)

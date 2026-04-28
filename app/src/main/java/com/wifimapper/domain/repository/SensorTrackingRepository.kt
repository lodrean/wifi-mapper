package com.wifimapper.domain.repository

import com.wifimapper.domain.model.PdrPosition
import kotlinx.coroutines.flow.Flow

interface SensorTrackingRepository {
    fun startTracking(stepLengthMeters: Float)
    fun stopTracking()
    fun getPositionStream(): Flow<PdrPosition>
    fun isAvailable(): Boolean
    fun resetPosition(x: Float = 0f, y: Float = 0f, heading: Float = 0f)
}

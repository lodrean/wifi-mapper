package com.wifimapper.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.wifimapper.domain.model.PdrPosition
import com.wifimapper.domain.repository.SensorTrackingRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SensorTrackingRepositoryImpl(
    context: Context
) : SensorTrackingRepository, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _positionChannel = Channel<PdrPosition>(Channel.CONFLATED)
    private val positionFlow = _positionChannel.receiveAsFlow()

    private var stepLengthMeters = 0.75f
    private var isTracking = false

    // Position state
    private var currentX = 0f
    private var currentY = 0f
    private var currentHeading = 0f
    private var stepCount = 0

    // Sensor data buffers
    private val accelValues = FloatArray(3)
    private val magValues = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // Step detection state
    private var lastAccelMagnitude = 0f
    private var accelMagnitudeHistory = mutableListOf<Float>()
    private var lastStepTime = 0L
    private val stepThreshold = 1.2f // m/s^2 above gravity
    private val minStepIntervalMs = 300L

    // Gyro integration for heading (fallback when magnetometer is unreliable)
    private var gyroHeading = 0f
    private var lastGyroTimestamp = 0L

    override fun startTracking(stepLengthMeters: Float) {
        this.stepLengthMeters = stepLengthMeters
        isTracking = true
        currentX = 0f
        currentY = 0f
        currentHeading = 0f
        stepCount = 0
        lastStepTime = 0L

        val samplingRate = SensorManager.SENSOR_DELAY_GAME

        rotationVector?.let {
            sensorManager.registerListener(this, it, samplingRate)
        } ?: run {
            accelerometer?.let { sensorManager.registerListener(this, it, samplingRate) }
            magneticField?.let { sensorManager.registerListener(this, it, samplingRate) }
        }

        gyroscope?.let { sensorManager.registerListener(this, it, samplingRate) }
    }

    override fun stopTracking() {
        isTracking = false
        sensorManager.unregisterListener(this)
    }

    override fun getPositionStream(): Flow<PdrPosition> = positionFlow

    override fun isAvailable(): Boolean {
        return accelerometer != null && (magneticField != null || rotationVector != null)
    }

    override fun resetPosition(x: Float, y: Float, heading: Float) {
        currentX = x
        currentY = y
        currentHeading = heading
        gyroHeading = heading
        emitPosition()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isTracking) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                currentHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                // Normalize to 0-360
                if (currentHeading < 0) currentHeading += 360f
            }

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelValues, 0, 3)
                detectStep()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magValues, 0, 3)
                updateHeadingFromAccelMag()
            }

            Sensor.TYPE_GYROSCOPE -> {
                updateHeadingFromGyroscope(event)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateHeadingFromAccelMag() {
        if (rotationVector != null) return // Prefer rotation vector
        if (accelValues.all { it == 0f } || magValues.all { it == 0f }) return

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magValues)) {
            SensorManager.getOrientation(rotationMatrix, orientation)
            val magHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
            // Complementary filter: mostly gyro, some mag
            currentHeading = 0.9f * gyroHeading + 0.1f * normalizeAngle(magHeading)
        }
    }

    private fun updateHeadingFromGyroscope(event: SensorEvent) {
        val now = event.timestamp
        if (lastGyroTimestamp != 0L) {
            val dt = (now - lastGyroTimestamp) / 1_000_000_000f // nanos to seconds
            // z-axis rotation rate
            val rotationRateZ = event.values[2]
            gyroHeading += Math.toDegrees(rotationRateZ.toDouble()).toFloat() * dt
            gyroHeading = normalizeAngle(gyroHeading)

            if (rotationVector == null && magneticField == null) {
                currentHeading = gyroHeading
            }
        }
        lastGyroTimestamp = now
    }

    private fun detectStep() {
        // Remove gravity approximation with high-pass filter
        val magnitude = sqrt(
            accelValues[0] * accelValues[0] +
            accelValues[1] * accelValues[1] +
            accelValues[2] * accelValues[2]
        )

        // High-pass filter: remove gravity (~9.8)
        val filteredMagnitude = magnitude - 9.8f

        accelMagnitudeHistory.add(filteredMagnitude)
        if (accelMagnitudeHistory.size > 10) {
            accelMagnitudeHistory.removeAt(0)
        }

        // Simple peak detection
        val now = System.currentTimeMillis()
        if (filteredMagnitude > stepThreshold &&
            filteredMagnitude > lastAccelMagnitude &&
            now - lastStepTime > minStepIntervalMs
        ) {
            // Check if it's a peak
            if (accelMagnitudeHistory.size >= 3) {
                val prev = accelMagnitudeHistory[accelMagnitudeHistory.size - 2]
                val prev2 = accelMagnitudeHistory[accelMagnitudeHistory.size - 3]
                if (filteredMagnitude > prev && prev > prev2) {
                    onStepDetected()
                    lastStepTime = now
                }
            }
        }

        lastAccelMagnitude = filteredMagnitude
    }

    private fun onStepDetected() {
        stepCount++
        val headingRad = Math.toRadians(currentHeading.toDouble())
        currentX += stepLengthMeters * sin(headingRad).toFloat()
        currentY += stepLengthMeters * cos(headingRad).toFloat()
        emitPosition()
    }

    private fun emitPosition() {
        _positionChannel.trySend(
            PdrPosition(
                x = currentX,
                y = currentY,
                headingDegrees = currentHeading,
                stepCount = stepCount
            )
        )
    }

    private fun normalizeAngle(angle: Float): Float {
        var result = angle % 360f
        if (result < 0) result += 360f
        return result
    }
}

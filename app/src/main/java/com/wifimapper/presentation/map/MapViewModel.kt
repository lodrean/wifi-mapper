package com.wifimapper.presentation.map

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifimapper.domain.model.Measurement
import com.wifimapper.domain.model.PdrPosition
import com.wifimapper.domain.model.TrajectoryPoint
import com.wifimapper.domain.model.WifiScanResult
import com.wifimapper.domain.repository.SensorTrackingRepository
import com.wifimapper.domain.repository.SessionRepository
import com.wifimapper.domain.repository.WifiScanRepository
import com.wifimapper.domain.usecase.CreateSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MapViewModel(
    private val createSessionUseCase: CreateSessionUseCase,
    private val sessionRepository: SessionRepository,
    private val wifiScanRepository: WifiScanRepository,
    private val sensorTrackingRepository: SensorTrackingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        MapState(
            sensorsAvailable = sensorTrackingRepository.isAvailable(),
            isManualMode = !sensorTrackingRepository.isAvailable()
        )
    )
    val state: StateFlow<MapState> = _state.asStateFlow()

    private val _events = Channel<MapEvent>()
    val events = _events.receiveAsFlow()

    private var trackingJob: Job? = null
    private var wifiScanJob: Job? = null
    private var currentPosition = PdrPosition(0f, 0f, 0f, 0)
    private val trajectoryBuffer = mutableListOf<TrajectoryPoint>()
    private val measurementBuffer = mutableListOf<Measurement>()

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.OnStartSession -> startSession()
            is MapAction.OnStopSession -> stopSession()
            is MapAction.OnResetPosition -> resetPosition()
            is MapAction.OnSelectNetwork -> selectNetwork(action.bssid)
            is MapAction.OnStepLengthChange -> updateStepLength(action.value)
            is MapAction.OnMapTap -> onMapTap(action.x, action.y)
            is MapAction.OnToggleManualMode -> toggleManualMode()
            is MapAction.OnDismissError -> _state.value = _state.value.copy(errorMessage = null)
            is MapAction.OnDismissThrottlingWarning -> _state.value = _state.value.copy(showWifiThrottlingWarning = false)
        }
    }

    private fun startSession() {
        if (_state.value.isTracking) return

        viewModelScope.launch {
            try {
                val sessionId = createSessionUseCase(
                    name = "Session ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                    stepLengthMeters = _state.value.stepLengthMeters
                )

                _state.value = _state.value.copy(
                    sessionId = sessionId,
                    isTracking = true,
                    isLoading = true,
                    currentPosition = Offset(0f, 0f),
                    trajectory = emptyList(),
                    measurements = emptyList(),
                    stepCount = 0
                )

                sensorTrackingRepository.resetPosition(0f, 0f, 0f)
                sensorTrackingRepository.startTracking(_state.value.stepLengthMeters)

                // Observe position updates
                trackingJob = sensorTrackingRepository.getPositionStream()
                    .onEach { position ->
                        onPositionUpdate(position)
                    }
                    .launchIn(viewModelScope)

                // Start periodic WiFi scanning
                startWifiScanning()

                _state.value = _state.value.copy(isLoading = false)

            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to start session", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to start session: ${e.message}"
                )
            }
        }
    }

    private fun stopSession() {
        if (!_state.value.isTracking) return

        viewModelScope.launch {
            sensorTrackingRepository.stopTracking()
            trackingJob?.cancel()
            wifiScanJob?.cancel()

            // Flush remaining buffer to DB
            flushBuffersToDb()

            // Update session as inactive
            val session = sessionRepository.getSessionById(_state.value.sessionId).let { flow ->
                var result: com.wifimapper.domain.model.Session? = null
                flow.collect { result = it }
                result
            }

            session?.let {
                sessionRepository.updateSession(it.copy(isActive = false))
            }

            _state.value = _state.value.copy(isTracking = false)
            _events.send(MapEvent.ShowSnackbar("Session saved successfully"))
        }
    }

    private fun resetPosition() {
        sensorTrackingRepository.resetPosition(0f, 0f, 0f)
        currentPosition = PdrPosition(0f, 0f, 0f, 0)
        _state.value = _state.value.copy(
            currentPosition = Offset(0f, 0f),
            trajectory = emptyList(),
            stepCount = 0
        )
    }

    private fun selectNetwork(bssid: String?) {
        _state.value = _state.value.copy(selectedNetwork = bssid)
    }

    private fun updateStepLength(value: Float) {
        _state.value = _state.value.copy(stepLengthMeters = value)
        if (_state.value.isTracking) {
            // Restart tracking with new step length
            sensorTrackingRepository.stopTracking()
            sensorTrackingRepository.startTracking(value)
        }
    }

    private fun onPositionUpdate(position: PdrPosition) {
        currentPosition = position
        val newOffset = Offset(position.x, position.y)

        val trajectoryPoint = TrajectoryPoint(
            id = UUID.randomUUID().toString(),
            sessionId = _state.value.sessionId,
            x = position.x,
            y = position.y,
            headingDegrees = position.headingDegrees,
            timestamp = System.currentTimeMillis(),
            isStep = position.stepCount > _state.value.stepCount
        )
        trajectoryBuffer.add(trajectoryPoint)

        // Update UI state
        val currentTrajectory = _state.value.trajectory.toMutableList()
        currentTrajectory.add(TrajectoryPointUi(position.x, position.y))

        _state.value = _state.value.copy(
            currentPosition = newOffset,
            currentHeading = position.headingDegrees,
            stepCount = position.stepCount,
            trajectory = currentTrajectory
        )

        // Flush buffer periodically
        if (trajectoryBuffer.size >= 10) {
            viewModelScope.launch { flushBuffersToDb() }
        }

        // Trigger WiFi scan on each step
        if (position.stepCount > _state.value.stepCount) {
            viewModelScope.launch { performWifiScan() }
        }
    }

    private fun startWifiScanning() {
        wifiScanJob = viewModelScope.launch {
            while (true) {
                performWifiScan()
                delay(15000L) // Scan every 15 seconds to respect throttling
            }
        }
    }

    private suspend fun performWifiScan() {
        try {
            val results = wifiScanRepository.scan()
            updateWifiNetworks(results)
            saveMeasurements(results)
        } catch (e: Exception) {
            Log.e("MapViewModel", "WiFi scan failed", e)
        }
    }

    private fun updateWifiNetworks(results: List<WifiScanResult>) {
        val networks = results.map { result ->
            WifiNetworkUi(
                ssid = result.ssid,
                bssid = result.bssid,
                rssiDbm = result.rssiDbm,
                frequencyMHz = result.frequencyMHz
            )
        }
        _state.value = _state.value.copy(wifiNetworks = networks)
    }

    private fun saveMeasurements(results: List<WifiScanResult>) {
        val sessionId = _state.value.sessionId
        val selectedBssid = _state.value.selectedNetwork

        results.forEach { result ->
            // If no network selected, save all. Otherwise save only selected.
            if (selectedBssid == null || result.bssid == selectedBssid) {
                val measurement = Measurement(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    x = currentPosition.x,
                    y = currentPosition.y,
                    rssiDbm = result.rssiDbm,
                    bssid = result.bssid,
                    timestamp = System.currentTimeMillis()
                )
                measurementBuffer.add(measurement)
            }
        }

        // Update UI measurements
        val currentMeasurements = _state.value.measurements.toMutableList()
        results.filter { selectedBssid == null || it.bssid == selectedBssid }
            .forEach { result ->
                currentMeasurements.add(
                    MeasurementUi(
                        x = currentPosition.x,
                        y = currentPosition.y,
                        rssiDbm = result.rssiDbm,
                        ssid = result.ssid
                    )
                )
            }

        _state.value = _state.value.copy(measurements = currentMeasurements)

        if (measurementBuffer.size >= 20) {
            viewModelScope.launch { flushBuffersToDb() }
        }
    }

    private suspend fun flushBuffersToDb() {
        val sessionId = _state.value.sessionId
        if (sessionId.isEmpty()) return

        trajectoryBuffer.forEach { point ->
            sessionRepository.addTrajectoryPoint(point)
        }
        trajectoryBuffer.clear()

        measurementBuffer.forEach { measurement ->
            sessionRepository.addMeasurement(measurement)
        }
        measurementBuffer.clear()
    }

    private fun onMapTap(x: Float, y: Float) {
        if (!_state.value.isTracking) return
        if (!_state.value.isManualMode) return

        currentPosition = PdrPosition(x, y, currentPosition.headingDegrees, currentPosition.stepCount + 1)

        val trajectoryPoint = TrajectoryPoint(
            id = java.util.UUID.randomUUID().toString(),
            sessionId = _state.value.sessionId,
            x = x,
            y = y,
            headingDegrees = currentPosition.headingDegrees,
            timestamp = System.currentTimeMillis(),
            isStep = true
        )
        trajectoryBuffer.add(trajectoryPoint)

        val currentTrajectory = _state.value.trajectory.toMutableList()
        currentTrajectory.add(TrajectoryPointUi(x, y))

        _state.value = _state.value.copy(
            currentPosition = androidx.compose.ui.geometry.Offset(x, y),
            stepCount = currentPosition.stepCount,
            trajectory = currentTrajectory
        )

        viewModelScope.launch {
            performWifiScan()
            if (trajectoryBuffer.size >= 5) {
                flushBuffersToDb()
            }
        }
    }

    private fun toggleManualMode() {
        val newMode = !_state.value.isManualMode
        _state.value = _state.value.copy(isManualMode = newMode)

        if (_state.value.isTracking) {
            if (newMode) {
                sensorTrackingRepository.stopTracking()
                trackingJob?.cancel()
            } else {
                sensorTrackingRepository.startTracking(_state.value.stepLengthMeters)
                trackingJob = sensorTrackingRepository.getPositionStream()
                    .onEach { position -> onPositionUpdate(position) }
                    .launchIn(viewModelScope)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_state.value.isTracking) {
            sensorTrackingRepository.stopTracking()
            trackingJob?.cancel()
            wifiScanJob?.cancel()
            viewModelScope.launch { flushBuffersToDb() }
        }
    }
}

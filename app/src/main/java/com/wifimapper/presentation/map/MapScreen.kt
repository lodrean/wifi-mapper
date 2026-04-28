package com.wifimapper.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wifimapper.R
import com.wifimapper.presentation.map.components.HeatmapCanvas
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapRoot(
    viewModel: MapViewModel,
    onNavigateBack: () -> Unit,
    sessionId: String? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is MapEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is MapEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.onAction(MapAction.OnLoadSession(sessionId))
        }
    }

    MapScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    state: MapState,
    snackbarHostState: SnackbarHostState,
    onAction: (MapAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state.isTracking) state.sessionName else "WiFi Mapper",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.isTracking) {
                            Text(
                                text = stringResource(R.string.session_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    if (state.isTracking) {
                        IconButton(onClick = { onAction(MapAction.OnResetPosition) }) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = stringResource(R.string.cd_my_location)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (state.isTracking) {
                        onAction(MapAction.OnStopSession)
                    } else {
                        onAction(MapAction.OnStartSession)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (state.isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                text = {
                    Text(if (state.isTracking) "Stop" else "Start")
                },
                containerColor = if (state.isTracking) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map Canvas
            HeatmapCanvas(
                measurements = state.measurements,
                trajectory = state.trajectory,
                currentPosition = state.currentPosition,
                modifier = Modifier.fillMaxSize(),
                isManualMode = state.isManualMode && state.isTracking,
                onTap = { offset ->
                    onAction(MapAction.OnMapTap(offset.x, offset.y))
                }
            )

            // Loading overlay
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Bottom info panel
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.LocationOn,
                            label = "Steps",
                            value = state.stepCount.toString()
                        )
                        StatItem(
                            icon = Icons.Default.Wifi,
                            label = "Points",
                            value = state.measurements.size.toString()
                        )
                        StatItem(
                            icon = Icons.Default.LocationSearching,
                            label = "Pos",
                            value = state.currentPosition?.let { "%.1f, %.1f".format(it.x, it.y) } ?: "—"
                        )
                    }

                    if (state.isTracking) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Manual mode toggle
                        if (!state.sensorsAvailable) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Manual mode (sensors unavailable)") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            AssistChip(
                                onClick = { onAction(MapAction.OnToggleManualMode) },
                                label = { Text(if (state.isManualMode) "Manual mode" else "Sensor mode") },
                                leadingIcon = {
                                    Icon(
                                        if (state.isManualMode) Icons.Default.LocationOn else Icons.Default.LocationSearching,
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        // Network selector
                        NetworkSelector(
                            networks = state.wifiNetworks,
                            selectedBssid = state.selectedNetwork,
                            onSelect = { onAction(MapAction.OnSelectNetwork(it)) }
                        )
                    }
                }
            }

            // WiFi throttling warning
            if (state.showWifiThrottlingWarning) {
                AlertDialog(
                    onDismissRequest = { onAction(MapAction.OnDismissThrottlingWarning) },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                    title = { Text("WiFi Scan Limit") },
                    text = { Text(stringResource(R.string.wifi_throttling_warning)) },
                    confirmButton = {
                        TextButton(onClick = { onAction(MapAction.OnDismissThrottlingWarning) }) {
                            Text("OK")
                        }
                    }
                )
            }

            // Error dialog
            state.errorMessage?.let { error ->
                AlertDialog(
                    onDismissRequest = { onAction(MapAction.OnDismissError) },
                    title = { Text("Error") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { onAction(MapAction.OnDismissError) }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkSelector(
    networks: List<WifiNetworkUi>,
    selectedBssid: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    val selectedNetwork = networks.find { it.bssid == selectedBssid }
    val displayText = selectedNetwork?.let { "${it.ssid} (${it.rssiDbm} dBm)" } ?: "All networks"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Filter by network") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All networks") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
                leadingIcon = {
                    Icon(Icons.Default.WifiTethering, contentDescription = null)
                }
            )

            networks.sortedByDescending { it.rssiDbm }.forEach { network ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(network.ssid.ifEmpty { "Hidden" })
                            Text(
                                text = "${network.rssiDbm} dBm • ${network.bssid}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(network.bssid)
                        expanded = false
                    },
                    leadingIcon = {
                        val signalIcon = when {
                            network.rssiDbm >= -60 -> Icons.Default.Wifi
                            else -> Icons.Default.WifiTethering
                        }
                        Icon(signalIcon, contentDescription = null)
                    }
                )
            }
        }
    }
}

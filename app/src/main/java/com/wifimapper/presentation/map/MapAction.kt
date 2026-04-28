package com.wifimapper.presentation.map

sealed interface MapAction {
    data object OnStartSession : MapAction
    data object OnStopSession : MapAction
    data object OnResetPosition : MapAction
    data class OnSelectNetwork(val bssid: String?) : MapAction
    data class OnStepLengthChange(val value: Float) : MapAction
    data class OnMapTap(val x: Float, val y: Float) : MapAction
    data object OnToggleManualMode : MapAction
    data object OnDismissError : MapAction
    data object OnDismissThrottlingWarning : MapAction
    data class OnLoadSession(val sessionId: String) : MapAction
}

package com.wifimapper.presentation.map

sealed interface MapEvent {
    data class ShowSnackbar(val message: String) : MapEvent
    data object NavigateBack : MapEvent
}

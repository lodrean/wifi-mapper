package com.wifimapper.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class MapRoute(val sessionId: String? = null)

@Serializable
object SettingsRoute

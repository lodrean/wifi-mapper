package com.wifimapper.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wifimapper.R
import com.wifimapper.presentation.home.HomeRoot
import com.wifimapper.presentation.map.MapRoot
import com.wifimapper.presentation.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel
import com.wifimapper.presentation.map.MapViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<HomeRoute> {
                HomeRoot(
                    onNavigateToMap = {
                        navController.navigate(MapRoute)
                    }
                )
            }
            composable<MapRoute> {
                val viewModel: MapViewModel = koinViewModel()
                MapRoot(
                    viewModel = viewModel,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val currentEntry = navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry.value?.destination

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.maps)) },
            selected = currentDestination?.hasRoute(HomeRoute::class) == true,
            onClick = {
                navController.navigate(HomeRoute) {
                    popUpTo(HomeRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            label = { Text(stringResource(R.string.new_session)) },
            selected = currentDestination?.hasRoute(MapRoute::class) == true,
            onClick = {
                navController.navigate(MapRoute) {
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.settings)) },
            selected = currentDestination?.hasRoute(SettingsRoute::class) == true,
            onClick = {
                navController.navigate(SettingsRoute) {
                    launchSingleTop = true
                }
            }
        )
    }
}

package com.trackit.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trackit.feature.driver.detail.PackageDetailScreen
import com.trackit.feature.driver.map.DriverMapScreen
import com.trackit.feature.driver.route.RouteScreen
import com.trackit.feature.profile.ProfileScreen

private data class DriverDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun DriverNavGraph(
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    val destinations = listOf(
        DriverDestination(Routes.DRIVER_ROUTE, "Ruta", Icons.Default.Route),
        DriverDestination(Routes.DRIVER_MAP, "Mapa", Icons.Default.Map),
        DriverDestination(Routes.DRIVER_PROFILE, "Perfil", Icons.Default.Person)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = destinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DRIVER_ROUTE,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DRIVER_ROUTE) {
                RouteScreen(
                    onPackageClick = { packageId ->
                        navController.navigate(Routes.driverDetail(packageId))
                    }
                )
            }
            composable(Routes.DRIVER_MAP) {
                DriverMapScreen()
            }
            composable(Routes.DRIVER_PROFILE) {
                ProfileScreen(
                    onLogout = {
                        rootNavController.navigate(Routes.LOGIN) {
                            popUpTo(rootNavController.graph.id) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable(Routes.DRIVER_DETAIL) {
                PackageDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

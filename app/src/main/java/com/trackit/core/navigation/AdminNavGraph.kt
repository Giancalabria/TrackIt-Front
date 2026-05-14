package com.trackit.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
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
import com.trackit.feature.admin.fleet.FleetScreen
import com.trackit.feature.admin.globalmap.GlobalMapScreen
import com.trackit.feature.profile.ProfileScreen

private data class AdminDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AdminNavGraph(
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    val destinations = listOf(
        AdminDestination(Routes.ADMIN_FLEET, "Flota", Icons.Default.LocalShipping),
        AdminDestination(Routes.ADMIN_GLOBAL_MAP, "Mapa Global", Icons.Default.Map),
        AdminDestination(Routes.ADMIN_PROFILE, "Perfil", Icons.Default.Person)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ADMIN_FLEET,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.ADMIN_FLEET) {
                FleetScreen()
            }
            composable(Routes.ADMIN_GLOBAL_MAP) {
                GlobalMapScreen()
            }
            composable(Routes.ADMIN_PROFILE) {
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
        }
    }
}

package com.trackit.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trackit.data.model.UserRole
import com.trackit.feature.auth.LoginScreen
import com.trackit.feature.auth.RegisterScreen
import com.trackit.feature.driver.truck.DriverTruckSetupScreen

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun TrackItNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val driverItems = listOf(
        NavItem(Routes.DRIVER_ROUTE, "Ruta", Icons.Default.Route),
        NavItem(Routes.DRIVER_PROFILE, "Perfil", Icons.Default.Person)
    )

    val warehouseItems = listOf(
        NavItem(Routes.WAREHOUSE_HOME, "Inicio", Icons.Default.Home),
        NavItem(Routes.WAREHOUSE_HISTORY, "Historial", Icons.Default.History),
        NavItem(Routes.WAREHOUSE_PROFILE, "Perfil", Icons.Default.Person)
    )

    val adminItems = listOf(
        NavItem(Routes.ADMIN_FLEET, "Flota", Icons.Default.LocalShipping),
        NavItem(Routes.ADMIN_GLOBAL_MAP, "Mapa Global", Icons.Default.Map),
        NavItem(Routes.ADMIN_PROFILE, "Perfil", Icons.Default.Person)
    )

    val bottomBarItems = when {
        currentDestination?.hierarchy?.any { it.route == Routes.DRIVER } == true -> driverItems
        currentDestination?.hierarchy?.any { it.route == Routes.WAREHOUSE } == true -> warehouseItems
        currentDestination?.hierarchy?.any { it.route == Routes.ADMIN } == true -> adminItems
        else -> emptyList()
    }

    Scaffold(
        bottomBar = {
            if (bottomBarItems.isNotEmpty()) {
                NavigationBar {
                    bottomBarItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { user ->
                        val destination = when (user.role) {
                            UserRole.DRIVER -> Routes.DRIVER_SETUP_TRUCK
                            UserRole.WAREHOUSE -> Routes.WAREHOUSE
                            UserRole.ADMIN -> Routes.ADMIN
                        }
                        navController.navigate(destination) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = { user ->
                        val destination = when (user.role) {
                            UserRole.DRIVER -> Routes.DRIVER_SETUP_TRUCK
                            UserRole.WAREHOUSE -> Routes.WAREHOUSE
                            UserRole.ADMIN -> Routes.ADMIN
                        }
                        navController.navigate(destination) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.DRIVER_SETUP_TRUCK) {
                DriverTruckSetupScreen(
                    onSetupComplete = {
                        navController.navigate(Routes.DRIVER) {
                            popUpTo(Routes.DRIVER_SETUP_TRUCK) { inclusive = true }
                        }
                    }
                )
            }

            driverNavGraph(navController)
            warehouseNavGraph(navController)
            adminNavGraph(navController)
        }
    }
}

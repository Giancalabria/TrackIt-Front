package com.trackit.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.trackit.feature.driver.detail.PackageDetailScreen
import com.trackit.feature.driver.map.DriverMapScreen
import com.trackit.feature.driver.route.RouteScreen
import com.trackit.feature.profile.ProfileScreen

fun NavGraphBuilder.driverNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Routes.DRIVER_ROUTE,
        route = Routes.DRIVER
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
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) {
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

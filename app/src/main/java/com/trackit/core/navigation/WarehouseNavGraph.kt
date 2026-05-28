package com.trackit.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.trackit.feature.profile.ProfileScreen
import com.trackit.feature.warehouse.history.HistoryScreen
import com.trackit.feature.warehouse.home.WarehouseHomeScreen
import com.trackit.feature.warehouse.intake.IntakeScreen
import com.trackit.feature.warehouse.loadtruck.LoadTruckScreen

fun NavGraphBuilder.warehouseNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Routes.WAREHOUSE_HOME,
        route = Routes.WAREHOUSE
    ) {
        composable(Routes.WAREHOUSE_HOME) {
            WarehouseHomeScreen(
                onLoadTruckClick = { navController.navigate(Routes.WAREHOUSE_LOAD_TRUCK) },
                onIntakeClick = { navController.navigate(Routes.WAREHOUSE_INTAKE) }
            )
        }
        composable(Routes.WAREHOUSE_LOAD_TRUCK) {
            LoadTruckScreen()
        }
        composable(Routes.WAREHOUSE_INTAKE) {
            IntakeScreen()
        }
        composable(Routes.WAREHOUSE_HISTORY) {
            HistoryScreen()
        }
        composable(Routes.WAREHOUSE_PROFILE) {
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
    }
}

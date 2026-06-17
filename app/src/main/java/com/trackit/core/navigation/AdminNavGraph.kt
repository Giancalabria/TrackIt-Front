package com.trackit.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.trackit.feature.admin.assign.AssignRouteScreen
import com.trackit.feature.admin.createuser.CreateUserScreen
import com.trackit.feature.admin.fleet.FleetScreen
import com.trackit.feature.admin.globalmap.GlobalMapScreen
import com.trackit.feature.profile.ProfileScreen

fun NavGraphBuilder.adminNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Routes.ADMIN_FLEET,
        route = Routes.ADMIN
    ) {
        composable(Routes.ADMIN_FLEET) {
            FleetScreen(
                onTruckClick = { driverId ->
                    navController.navigate(Routes.adminAssignRoute(driverId))
                }
            )
        }
        composable(Routes.ADMIN_GLOBAL_MAP) {
            GlobalMapScreen()
        }
        composable(Routes.ADMIN_PROFILE) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
                onCreateUser = {
                    navController.navigate(Routes.ADMIN_CREATE_USER)
                }
            )
        }
        composable(Routes.ADMIN_CREATE_USER) {
            CreateUserScreen(
                onBack = { navController.popBackStack() },
                onUserCreated = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.ADMIN_ASSIGN_ROUTE,
            arguments = listOf(navArgument("driverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val driverId = backStackEntry.arguments?.getString("driverId") ?: ""
            AssignRouteScreen(
                driverId = driverId,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
    }
}

package com.trackit.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trackit.data.model.UserRole
import com.trackit.feature.auth.LoginScreen

@Composable
fun TrackItNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { user ->
                    val destination = when (user.role) {
                        UserRole.DRIVER -> Routes.DRIVER
                        UserRole.WAREHOUSE -> Routes.WAREHOUSE
                        UserRole.ADMIN -> Routes.ADMIN
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Routes.DRIVER) {
            DriverNavGraph(rootNavController = navController)
        }
        composable(Routes.WAREHOUSE) {
            WarehouseNavGraph(rootNavController = navController)
        }
        composable(Routes.ADMIN) {
            AdminNavGraph(rootNavController = navController)
        }
    }
}

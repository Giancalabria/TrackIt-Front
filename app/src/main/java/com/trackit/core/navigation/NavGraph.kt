package com.trackit.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trackit.core.onboarding.CoachMarkController
import com.trackit.core.onboarding.CoachMarkKeys
import com.trackit.core.onboarding.CoachMarkOverlay
import com.trackit.core.onboarding.CoachMarkRegistry
import com.trackit.core.onboarding.LocalCoachMarkRegistry
import com.trackit.core.onboarding.OnboardingPreferences
import com.trackit.core.onboarding.RoleCoachMarkSteps
import com.trackit.core.onboarding.coachMarkTarget
import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import com.trackit.feature.auth.LoginScreen
import com.trackit.feature.auth.SplashScreen
import com.trackit.feature.driver.truck.DriverTruckSetupScreen
import com.trackit.feature.onboarding.AppOnboardingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val coachMarkKey: String?
)

private fun homeRouteFor(user: User): String = when (user.role) {
    UserRole.DRIVER -> Routes.DRIVER_SETUP_TRUCK
    UserRole.WAREHOUSE -> Routes.WAREHOUSE_HOME
    UserRole.ADMIN -> Routes.ADMIN_FLEET
}

private fun roleHomeRoute(role: UserRole): String? = when (role) {
    UserRole.DRIVER -> Routes.DRIVER_ROUTE
    UserRole.WAREHOUSE -> Routes.WAREHOUSE_HOME
    UserRole.ADMIN -> Routes.ADMIN_FLEET
}

@Composable
fun TrackItNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onboardingPrefs = remember { OnboardingPreferences(context) }
    val coachMarkRegistry = remember { CoachMarkRegistry() }

    var pendingRoleTourUser by remember { mutableStateOf<User?>(null) }
    var activeCoachMarkController by remember { mutableStateOf<CoachMarkController?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val driverItems = listOf(
        NavItem(Routes.DRIVER_ROUTE, "Ruta", Icons.Default.Route, CoachMarkKeys.DRIVER_NAV_ROUTE),
        NavItem(Routes.DRIVER_MAP, "Mapa", Icons.Default.Map, CoachMarkKeys.DRIVER_NAV_MAP),
        NavItem(Routes.DRIVER_PROFILE, "Perfil", Icons.Default.Person, CoachMarkKeys.DRIVER_NAV_PROFILE)
    )

    val warehouseItems = listOf(
        NavItem(Routes.WAREHOUSE_HOME, "Inicio", Icons.Default.Home, CoachMarkKeys.WAREHOUSE_NAV_HOME),
        NavItem(Routes.WAREHOUSE_HISTORY, "Historial", Icons.Default.History, CoachMarkKeys.WAREHOUSE_NAV_HISTORY),
        NavItem(Routes.WAREHOUSE_PROFILE, "Perfil", Icons.Default.Person, CoachMarkKeys.WAREHOUSE_NAV_PROFILE)
    )

    val adminItems = listOf(
        NavItem(Routes.ADMIN_FLEET, "Flota", Icons.Default.LocalShipping, CoachMarkKeys.ADMIN_NAV_FLEET),
        NavItem(Routes.ADMIN_GLOBAL_MAP, "Mapa Global", Icons.Default.Map, CoachMarkKeys.ADMIN_NAV_GLOBAL_MAP),
        NavItem(Routes.ADMIN_PROFILE, "Perfil", Icons.Default.Person, CoachMarkKeys.ADMIN_NAV_PROFILE)
    )

    val bottomBarItems = when {
        currentDestination?.hierarchy?.any { it.route == Routes.DRIVER } == true -> driverItems
        currentDestination?.hierarchy?.any { it.route == Routes.WAREHOUSE } == true -> warehouseItems
        currentDestination?.hierarchy?.any { it.route == Routes.ADMIN } == true -> adminItems
        else -> emptyList()
    }

    fun finishRoleTour(user: User) {
        scope.launch {
            onboardingPrefs.setRoleTourCompleted(user.id)
        }
        pendingRoleTourUser = null
        activeCoachMarkController = null
    }

    fun scheduleRoleTour(user: User) {
        scope.launch {
            if (onboardingPrefs.hasCompletedRoleTour(user.id)) return@launch
            pendingRoleTourUser = user
        }
    }

    fun onLoginSuccess(user: User) {
        navController.navigate(homeRouteFor(user)) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
        scheduleRoleTour(user)
    }

    LaunchedEffect(pendingRoleTourUser, currentDestination?.route, activeCoachMarkController) {
        val user = pendingRoleTourUser ?: return@LaunchedEffect
        if (activeCoachMarkController != null) return@LaunchedEffect

        val onRoleHome = currentDestination?.route == roleHomeRoute(user.role)
        if (!onRoleHome) return@LaunchedEffect

        delay(300L)

        val graphRoot = when (user.role) {
            UserRole.DRIVER -> Routes.DRIVER
            UserRole.WAREHOUSE -> Routes.WAREHOUSE
            UserRole.ADMIN -> Routes.ADMIN
        }
        val controller = CoachMarkController(
            steps = RoleCoachMarkSteps.forRole(user.role),
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(graphRoot) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onFinished = { finishRoleTour(user) }
        )
        activeCoachMarkController = controller
        controller.start()
    }

    CompositionLocalProvider(LocalCoachMarkRegistry provides coachMarkRegistry) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (bottomBarItems.isNotEmpty()) {
                        NavigationBar {
                            bottomBarItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == item.route
                                } == true
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            modifier = if (item.coachMarkKey != null) {
                                                Modifier.coachMarkTarget(item.coachMarkKey)
                                            } else {
                                                Modifier
                                            }
                                        )
                                    },
                                    label = { Text(item.label) },
                                    selected = selected,
                                    onClick = {
                                        val graphRoot = when {
                                            currentDestination?.hierarchy?.any {
                                                it.route == Routes.DRIVER
                                            } == true -> Routes.DRIVER
                                            currentDestination?.hierarchy?.any {
                                                it.route == Routes.WAREHOUSE
                                            } == true -> Routes.WAREHOUSE
                                            else -> Routes.ADMIN
                                        }
                                        navController.navigate(item.route) {
                                            popUpTo(graphRoot) {
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
                    startDestination = Routes.SPLASH,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Routes.SPLASH) {
                        SplashScreen(
                            onResolved = { user ->
                                scope.launch {
                                    if (!onboardingPrefs.hasCompletedAppOnboarding()) {
                                        navController.navigate(Routes.APP_ONBOARDING) {
                                            popUpTo(Routes.SPLASH) { inclusive = true }
                                        }
                                    } else {
                                        val destination = if (user != null) {
                                            homeRouteFor(user)
                                        } else {
                                            Routes.LOGIN
                                        }
                                        navController.navigate(destination) {
                                            popUpTo(Routes.SPLASH) { inclusive = true }
                                        }
                                        if (user != null) {
                                            scheduleRoleTour(user)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable(Routes.APP_ONBOARDING) {
                        AppOnboardingScreen(
                            onComplete = {
                                scope.launch {
                                    onboardingPrefs.setAppOnboardingCompleted()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.APP_ONBOARDING) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Routes.LOGIN) {
                        LoginScreen(onLoginSuccess = ::onLoginSuccess)
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

            activeCoachMarkController?.let { controller ->
                if (controller.isActive) {
                    CoachMarkOverlay(
                        controller = controller,
                        registry = coachMarkRegistry
                    )
                }
            }
        }
    }
}

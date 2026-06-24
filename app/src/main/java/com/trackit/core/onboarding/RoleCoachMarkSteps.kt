package com.trackit.core.onboarding

import com.trackit.core.navigation.Routes
import com.trackit.data.model.UserRole

object RoleCoachMarkSteps {

    fun forRole(role: UserRole): List<CoachMarkStep> = when (role) {
        UserRole.DRIVER -> driverSteps()
        UserRole.WAREHOUSE -> warehouseSteps()
        UserRole.ADMIN -> adminSteps()
    }

    private fun driverSteps() = listOf(
        CoachMarkStep(
            targetKey = CoachMarkKeys.DRIVER_NAV_ROUTE,
            title = "Tu ruta del día",
            message = "Acá ves los paquetes asignados en el orden de entrega.",
            navigateToRoute = Routes.DRIVER_ROUTE
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.DRIVER_ROUTE_HEADER,
            title = "Entregas y escaneo",
            message = "Tocá un paquete para ver el detalle o usá el ícono de escáner para marcarlo como entregado.",
            navigateToRoute = Routes.DRIVER_ROUTE
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.DRIVER_NAV_MAP,
            title = "Mapa y navegación",
            message = "Consultá el mapa, buscá direcciones y trazá la ruta hacia tus paradas.",
            navigateToRoute = Routes.DRIVER_MAP
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.DRIVER_NAV_PROFILE,
            title = "Tu perfil",
            message = "Revisá tus datos y cerrá sesión cuando termines el turno.",
            navigateToRoute = Routes.DRIVER_PROFILE
        )
    )

    private fun warehouseSteps() = listOf(
        CoachMarkStep(
            targetKey = CoachMarkKeys.WAREHOUSE_INTAKE,
            title = "Ingresar paquetes",
            message = "Registrá envíos nuevos con dirección, tamaño y código de barras.",
            navigateToRoute = Routes.WAREHOUSE_HOME
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.WAREHOUSE_LOAD_TRUCK,
            title = "Cargar camión",
            message = "Elegí el camión y escaneá los paquetes asignados a ese chofer.",
            navigateToRoute = Routes.WAREHOUSE_HOME
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.WAREHOUSE_NAV_HISTORY,
            title = "Historial",
            message = "Consultá todos los ingresos que registraste en el depósito.",
            navigateToRoute = Routes.WAREHOUSE_HISTORY
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.WAREHOUSE_NAV_PROFILE,
            title = "Tu perfil",
            message = "Datos de tu cuenta y cierre de sesión.",
            navigateToRoute = Routes.WAREHOUSE_PROFILE
        )
    )

    private fun adminSteps() = listOf(
        CoachMarkStep(
            targetKey = CoachMarkKeys.ADMIN_GENERATE_ROUTES,
            title = "Generar rutas del día",
            message = "Ejecutá la optimización para asignar paquetes a los choferes disponibles.",
            navigateToRoute = Routes.ADMIN_FLEET
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.ADMIN_FLEET_LIST,
            title = "Flota activa",
            message = "Tocá un camión para revisar o ajustar manualmente la ruta de un chofer.",
            navigateToRoute = Routes.ADMIN_FLEET
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.ADMIN_NAV_GLOBAL_MAP,
            title = "Mapa global",
            message = "Vista general de camiones y entregas del día.",
            navigateToRoute = Routes.ADMIN_GLOBAL_MAP
        ),
        CoachMarkStep(
            targetKey = CoachMarkKeys.ADMIN_NAV_PROFILE,
            title = "Administración",
            message = "Desde Perfil podés crear usuarios nuevos y cerrar sesión.",
            navigateToRoute = Routes.ADMIN_PROFILE
        )
    )
}

package com.trackit.core.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DRIVER = "driver"
    const val WAREHOUSE = "warehouse"
    const val ADMIN = "admin"

    const val DRIVER_ROUTE = "driver/route"
    const val DRIVER_MAP = "driver/map"
    const val DRIVER_PROFILE = "driver/profile"
    const val DRIVER_DETAIL = "driver/detail/{packageId}"

    const val WAREHOUSE_HOME = "warehouse/home"
    const val WAREHOUSE_LOAD_TRUCK = "warehouse/load_truck"
    const val WAREHOUSE_INTAKE = "warehouse/intake"
    const val WAREHOUSE_HISTORY = "warehouse/history"
    const val WAREHOUSE_PROFILE = "warehouse/profile"

    const val ADMIN_FLEET = "admin/fleet"
    const val ADMIN_GLOBAL_MAP = "admin/global_map"
    const val ADMIN_PROFILE = "admin/profile"
    const val ADMIN_ASSIGN_ROUTE = "admin/assign_route/{driverId}"

    fun driverDetail(packageId: String): String = "driver/detail/$packageId"
    fun adminAssignRoute(driverId: String): String = "admin/assign_route/$driverId"
}

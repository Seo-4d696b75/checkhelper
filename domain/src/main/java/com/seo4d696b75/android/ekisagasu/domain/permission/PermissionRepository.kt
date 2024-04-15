package com.seo4d696b75.android.ekisagasu.domain.permission

interface PermissionRepository {
    val isDeviceLocationEnabled: Boolean
    val isLocationGranted: Boolean
    suspend fun checkDeviceLocationSettings(minInterval: Int): Boolean
    val isNotificationGranted: Boolean
    val canDrawOverlay: Boolean
}

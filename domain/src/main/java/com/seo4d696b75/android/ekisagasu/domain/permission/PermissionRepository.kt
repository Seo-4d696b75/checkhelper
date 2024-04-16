package com.seo4d696b75.android.ekisagasu.domain.permission

interface PermissionRepository {
    suspend fun getLocationPermissionState(): PermissionState
    suspend fun setLocationPermissionDenied()
    suspend fun getNotificationPermissionState(): PermissionState
    suspend fun setNotificationPermissionDenied()

    val isDeviceLocationEnabled: Boolean
    suspend fun checkDeviceLocationSettings(minInterval: Int): Boolean
    val isNotificationChannelEnabled: Boolean
    val canDrawOverlay: Boolean

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "jp.seo.station.ekisagasu.notification_main_silent"
    }
}

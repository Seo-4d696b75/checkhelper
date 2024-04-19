package com.seo4d696b75.android.ekisagasu.ui.permission

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PermissionRationale {
    @SerialName("locationPermission")
    data class LocationPermission(val showSystemRequestDialog: Boolean) : PermissionRationale

    @SerialName("notificationPermission")
    data class NotificationPermission(val showSystemRequestDialog: Boolean) : PermissionRationale

    @SerialName("notificationChannel")
    data object NotificationChannel : PermissionRationale

    @SerialName("drawOverlay")
    data object DrawOverlay : PermissionRationale
}

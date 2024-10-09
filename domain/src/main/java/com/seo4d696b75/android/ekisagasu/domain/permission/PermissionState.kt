package com.seo4d696b75.android.ekisagasu.domain.permission

sealed interface PermissionState {
    data object Granted : PermissionState
    data class NotGranted(
        val permission: String,
        /**
         * 一度でも権限リクエストが拒否されているか
         */
        val hasDenied: Boolean,
    ) : PermissionState
}


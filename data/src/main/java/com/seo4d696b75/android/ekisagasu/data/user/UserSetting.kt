package com.seo4d696b75.android.ekisagasu.data.user

data class UserSetting(
    val locationUpdateInterval: Int = 5,
    val searchK: Int = 12,
    val isPushNotification: Boolean = false,
    val isPushNotificationForce: Boolean = false,
    val isKeepNotification: Boolean = false,
    val isShowPrefectureNotification: Boolean = false,
    val isVibrate: Boolean = false,
    val isVibrateWhenApproach: Boolean = false,
    val vibrateDistance: Int = 100,
    val nightModeTimeout: Int = 0,
    val nightModeBrightness: Float = 128f,
    val timerPosition: Int = 0,
)

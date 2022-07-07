package jp.seo.station.ekisagasu.model

import android.content.SharedPreferences

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
    val nightModeBrightness: Int = 128,
    val timerPosition: Int = 0,
) {
    companion object {
        private const val KEY_INTERVAL = "interval"
        private const val KEY_RADAR = "radar"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_NOTIFY = "notification"
        private const val KEY_FORCE_NOTIFY = "forceNotify"
        private const val KEY_BRIGHTNESS = "brightness"
        private const val KEY_KEEP_NOTIFICATION = "notification_stationary"
        private const val KEY_NOTIFY_PREFECTURE = "notify_prefecture"
        private const val KEY_VIBRATE_METER = "vibrate_meter"
        private const val KEY_VIBRATE_APPROACH = "vibrate_approach"
        private const val KEY_NIGHT_TIMEOUT = "night_mode_timeout"
        private const val KEY_TIMER_POSITION = "timer_position_y"

        fun load(reference: SharedPreferences) = UserSetting(
            reference.getInt(KEY_INTERVAL, 5),
            reference.getInt(KEY_RADAR, 12),
            reference.getBoolean(KEY_NOTIFY, false),
            reference.getBoolean(KEY_FORCE_NOTIFY, false),
            reference.getBoolean(KEY_KEEP_NOTIFICATION, false),
            reference.getBoolean(KEY_NOTIFY_PREFECTURE, false),
            reference.getBoolean(KEY_VIBRATE, false),
            reference.getBoolean(KEY_VIBRATE_APPROACH, false),
            reference.getInt(KEY_VIBRATE_METER, 100),
            reference.getInt(KEY_NIGHT_TIMEOUT, 0),
            reference.getInt(KEY_BRIGHTNESS, 128),
            reference.getInt(KEY_TIMER_POSITION, 0)
        )
    }

    fun save(reference: SharedPreferences) = reference.edit().also { editor ->
        editor.putInt(KEY_INTERVAL, locationUpdateInterval)
        editor.putInt(KEY_RADAR, searchK)
        editor.putBoolean(KEY_NOTIFY, isPushNotification)
        editor.putBoolean(KEY_FORCE_NOTIFY, isPushNotificationForce)
        editor.putBoolean(KEY_KEEP_NOTIFICATION, isKeepNotification)
        editor.putBoolean(KEY_NOTIFY_PREFECTURE, isShowPrefectureNotification)
        editor.putBoolean(KEY_VIBRATE, isVibrate)
        editor.putBoolean(KEY_VIBRATE_APPROACH, isVibrateWhenApproach)
        editor.putInt(KEY_VIBRATE_METER, vibrateDistance)
        editor.putInt(KEY_NIGHT_TIMEOUT, nightModeTimeout)
        editor.putInt(KEY_BRIGHTNESS, nightModeBrightness)
        editor.putInt(KEY_TIMER_POSITION, timerPosition)
        editor.apply()
    }
}
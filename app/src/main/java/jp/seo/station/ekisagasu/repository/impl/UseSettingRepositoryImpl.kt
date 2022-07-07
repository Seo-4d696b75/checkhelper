package jp.seo.station.ekisagasu.repository.impl

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.UserSetting
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class UseSettingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserSettingRepository {
    override val setting = MutableStateFlow(UserSetting())

    override suspend fun load() {

        val s = context.getSharedPreferences(
            context.getString(R.string.preference_setting_backup),
            Context.MODE_PRIVATE
        ).let {
            UserSetting(
                it.getInt(KEY_INTERVAL, 5),
                it.getInt(KEY_RADAR, 12),
                it.getBoolean(KEY_NOTIFY, false),
                it.getBoolean(KEY_FORCE_NOTIFY, false),
                it.getBoolean(KEY_KEEP_NOTIFICATION, false),
                it.getBoolean(KEY_NOTIFY_PREFECTURE, false),
                it.getBoolean(KEY_VIBRATE, false),
                it.getBoolean(KEY_VIBRATE_APPROACH, false),
                it.getInt(KEY_VIBRATE_METER, 100),
                it.getInt(KEY_NIGHT_TIMEOUT, 0),
                it.getInt(KEY_BRIGHTNESS, 128),
                it.getInt(KEY_TIMER_POSITION, 0)
            )
        }
        setting.emit(s)
    }

    override suspend fun save() {
        val setting = this.setting.value
        context.getSharedPreferences(
            context.getString(R.string.preference_setting_backup),
            Context.MODE_PRIVATE
        ).edit().also { editor ->
            editor.putInt(KEY_INTERVAL, setting.locationUpdateInterval)
            editor.putInt(KEY_RADAR, setting.searchK)
            editor.putBoolean(KEY_NOTIFY, setting.isPushNotification)
            editor.putBoolean(KEY_FORCE_NOTIFY, setting.isPushNotificationForce)
            editor.putBoolean(KEY_KEEP_NOTIFICATION, setting.isKeepNotification)
            editor.putBoolean(KEY_NOTIFY_PREFECTURE, setting.isShowPrefectureNotification)
            editor.putBoolean(KEY_VIBRATE, setting.isVibrate)
            editor.putBoolean(KEY_VIBRATE_APPROACH, setting.isVibrateWhenApproach)
            editor.putInt(KEY_VIBRATE_METER, setting.vibrateDistance)
            editor.putInt(KEY_NIGHT_TIMEOUT, setting.nightModeTimeout)
            editor.putInt(KEY_BRIGHTNESS, setting.nightModeBrightness)
            editor.putInt(KEY_TIMER_POSITION, setting.timerPosition)
            editor.apply()
        }
    }

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

    }
}
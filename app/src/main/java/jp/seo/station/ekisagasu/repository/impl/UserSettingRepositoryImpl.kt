package jp.seo.station.ekisagasu.repository.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.UserSetting
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserSettingRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : UserSettingRepository {
        private val keyInterval = intPreferencesKey(KEY_INTERVAL)
        private val keyRadar = intPreferencesKey(KEY_RADAR)
        private val keyNotify = booleanPreferencesKey(KEY_NOTIFY)
        private val keyForceNotify = booleanPreferencesKey(KEY_FORCE_NOTIFY)
        private val keyKeepNotification = booleanPreferencesKey(KEY_KEEP_NOTIFICATION)
        private val keyNotifyPrefecture = booleanPreferencesKey(KEY_NOTIFY_PREFECTURE)
        private val keyVibrate = booleanPreferencesKey(KEY_VIBRATE)
        private val keyVibrateApproach = booleanPreferencesKey(KEY_VIBRATE_APPROACH)
        private val keyVibrateMeter = intPreferencesKey(KEY_VIBRATE_METER)
        private val keyNightTimeout = intPreferencesKey(KEY_NIGHT_TIMEOUT)
        private val keyBrightness = floatPreferencesKey(KEY_BRIGHTNESS)
        private val keyTimerPosition = intPreferencesKey(KEY_TIMER_POSITION)

        private val Preferences.userSetting: UserSetting
            get() =
                UserSetting(
                    locationUpdateInterval = this[keyInterval] ?: 5,
                    searchK = this[keyRadar] ?: 12,
                    isPushNotification = this[keyNotify] ?: false,
                    isPushNotificationForce = this[keyForceNotify] ?: false,
                    isKeepNotification = this[keyKeepNotification] ?: false,
                    isShowPrefectureNotification = this[keyNotifyPrefecture] ?: false,
                    isVibrate = this[keyVibrate] ?: false,
                    isVibrateWhenApproach = this[keyVibrateApproach] ?: false,
                    vibrateDistance = this[keyVibrateMeter] ?: 100,
                    nightModeTimeout = this[keyNightTimeout] ?: 0,
                    nightModeBrightness = this[keyBrightness] ?: 128f,
                    timerPosition = this[keyTimerPosition] ?: 0,
                )

        private val _setting = MutableStateFlow(UserSetting())

        override val setting = _setting

        override suspend fun load(): Unit =
            withContext(Dispatchers.IO) {
                val preference = context.dataStore.data.first()
                _setting.update { preference.userSetting }
            }

        override suspend fun save(): Unit =
            withContext(Dispatchers.IO) {
                context.dataStore.edit {
                    val value = _setting.value
                    it[keyInterval] = value.locationUpdateInterval
                    it[keyRadar] = value.searchK
                    it[keyNotify] = value.isPushNotification
                    it[keyForceNotify] = value.isPushNotificationForce
                    it[keyKeepNotification] = value.isKeepNotification
                    it[keyNotifyPrefecture] = value.isShowPrefectureNotification
                    it[keyVibrate] = value.isVibrate
                    it[keyVibrateApproach] = value.isVibrateWhenApproach
                    it[keyVibrateMeter] = value.vibrateDistance
                    it[keyNightTimeout] = value.nightModeTimeout
                    it[keyBrightness] = value.nightModeBrightness
                    it[keyTimerPosition] = value.timerPosition
                }
            }

        override fun update(producer: (UserSetting) -> UserSetting) {
            _setting.update(producer)
        }

        companion object {
            private const val KEY_INTERVAL = "interval"
            private const val KEY_RADAR = "radar"
            private const val KEY_VIBRATE = "vibrate"
            private const val KEY_NOTIFY = "notification"
            private const val KEY_FORCE_NOTIFY = "forceNotify"
            private const val KEY_BRIGHTNESS = "brightness_float"
            private const val KEY_KEEP_NOTIFICATION = "notification_stationary"
            private const val KEY_NOTIFY_PREFECTURE = "notify_prefecture"
            private const val KEY_VIBRATE_METER = "vibrate_meter"
            private const val KEY_VIBRATE_APPROACH = "vibrate_approach"
            private const val KEY_NIGHT_TIMEOUT = "night_mode_timeout"
            private const val KEY_TIMER_POSITION = "timer_position_y"
        }
    }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "setting_datastore_backup",
    produceMigrations = {
        listOf(
            SharedPreferencesMigration(it, it.getString(R.string.preference_setting_backup)),
        )
    },
)

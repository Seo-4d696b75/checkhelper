package jp.seo.station.ekisagasu.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.RemoteDataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSetting
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingRepository: UserSettingRepository,
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
    private val remoteDataRepository: RemoteDataRepository,
    private val logger: LogCollector,
) : ViewModel(),
    LogCollector by logger {
    val dataVersion = dataRepository.dataVersion

    fun checkLatestData() = viewModelScope.launch(Dispatchers.IO) {
        val latest = try {
            remoteDataRepository.getLatestDataVersion(false)
        } catch (e: IOException) {
            Timber.w(e)
            appStateRepository.emitMessage(AppMessage.Data.CheckLatestVersionFailure(e))
            log(LogMessage.Data.CheckLatestVersionFailure(e))
            return@launch
        }
        val current = dataRepository.getDataVersion()
        if (current == null || latest.version > current.version) {
            appStateRepository.emitMessage(
                AppMessage.Data.ConfirmUpdate(DataUpdateType.Latest, latest),
            )
            log(LogMessage.Data.LatestVersionFound(latest))
        } else {
            appStateRepository.emitMessage(AppMessage.Data.VersionUpToDate)
        }
    }

    val state = combine(
        settingRepository.setting,
        appStateRepository.nightMode,
    ) { setting, night ->
        SettingState.fromUserSetting(setting, night)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SettingState.fromUserSetting(UserSetting(), false),
    )

    fun updateState(producer: (SettingState) -> SettingState) =
        settingRepository.update {
            val old = SettingState.fromUserSetting(it, appStateRepository.nightMode.value)
            val value = producer(old)
            appStateRepository.setNightMode(value.isNightMode)
            value.toUserSetting()
        }
}

data class SettingState(
    val locationUpdateInterval: Int,
    val searchK: Int,
    val isPushNotification: Boolean,
    val isPushNotificationForce: Boolean,
    val isKeepNotification: Boolean,
    val isShowPrefectureNotification: Boolean,
    val isVibrate: Boolean,
    val isVibrateWhenApproach: Boolean,
    val vibrateDistance: Int,
    val isNightMode: Boolean,
    val nightModeTimeout: Int,
    val nightModeBrightness: Float,
) {
    companion object {
        fun fromUserSetting(
            setting: UserSetting,
            isNightMode: Boolean,
        ) = SettingState(
            locationUpdateInterval = setting.locationUpdateInterval,
            searchK = setting.searchK,
            isPushNotification = setting.isPushNotification,
            isPushNotificationForce = setting.isPushNotificationForce,
            isKeepNotification = setting.isKeepNotification,
            isShowPrefectureNotification = setting.isShowPrefectureNotification,
            isVibrate = setting.isVibrate,
            isVibrateWhenApproach = setting.isVibrateWhenApproach,
            vibrateDistance = setting.vibrateDistance,
            isNightMode = isNightMode,
            nightModeTimeout = setting.nightModeTimeout,
            nightModeBrightness = setting.nightModeBrightness,
        )
    }

    fun toUserSetting() =
        UserSetting(
            locationUpdateInterval = locationUpdateInterval,
            searchK = searchK,
            isPushNotification = isPushNotification,
            isPushNotificationForce = isPushNotificationForce,
            isKeepNotification = isKeepNotification,
            isShowPrefectureNotification = isShowPrefectureNotification,
            isVibrate = isVibrate,
            isVibrateWhenApproach = isVibrateWhenApproach,
            vibrateDistance = vibrateDistance,
            nightModeTimeout = nightModeTimeout,
            nightModeBrightness = nightModeBrightness,
        )
}

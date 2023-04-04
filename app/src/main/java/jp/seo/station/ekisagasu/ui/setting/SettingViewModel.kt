package jp.seo.station.ekisagasu.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.model.UserSetting
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import jp.seo.station.ekisagasu.ui.dialog.DataUpdateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingRepository: UserSettingRepository,
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
) : ViewModel() {

    val dataVersion = dataRepository.dataVersion

    fun checkLatestData() = viewModelScope.launch(Dispatchers.IO) {
        val latest = try {
            dataRepository.getLatestDataVersion(true)
        } catch (e: IOException) {
            appStateRepository.emitMessage(AppMessage.CheckLatestVersionFailure(e))
            return@launch
        }
        val current = dataRepository.getDataVersion()
        if (current == null || latest.version > current.version) {
            appStateRepository.emitMessage(
                AppMessage.RequestDataUpdate(DataUpdateType.Latest, latest)
            )
        } else {
            appStateRepository.emitMessage(AppMessage.VersionUpToDate)
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
        SettingState.fromUserSetting(UserSetting(), false)
    )

    fun updateState(producer: (SettingState) -> SettingState) = settingRepository.update {
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
    val timerPosition: Int,
) {
    companion object {
        fun fromUserSetting(setting: UserSetting, isNightMode: Boolean) = SettingState(
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
            timerPosition = setting.timerPosition,
        )
    }

    fun toUserSetting() = UserSetting(
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
        timerPosition = timerPosition,
    )
}

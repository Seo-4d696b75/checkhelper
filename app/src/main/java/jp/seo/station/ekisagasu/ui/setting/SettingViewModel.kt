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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            // TODO フィードバックUI
            return@launch
        }
        val current = dataRepository.getDataVersion()
        if (current == null || latest.version > current.version) {
            appStateRepository.emitMessage(
                AppMessage.RequestDataUpdate(DataUpdateType.Latest, latest)
            )
        } else {
            // TODO フィードバックUI
        }
    }

    private val _setting = MutableStateFlow(
        SettingState.fromUserSetting(settingRepository.setting.value)
    )

    val setting: StateFlow<SettingState> = _setting

    var state: SettingState
        get() = _setting.value
        set(value) {
            _setting.value = value
            settingRepository.setting.value = value.toUserSetting()
            viewModelScope.launch {
                appStateRepository.setNightMode(value.isNightMode)
            }
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
        fun fromUserSetting(setting: UserSetting) = SettingState(
            locationUpdateInterval = setting.locationUpdateInterval,
            searchK = setting.searchK,
            isPushNotification = setting.isPushNotification,
            isPushNotificationForce = setting.isPushNotificationForce,
            isKeepNotification = setting.isKeepNotification,
            isShowPrefectureNotification = setting.isShowPrefectureNotification,
            isVibrate = setting.isVibrate,
            isVibrateWhenApproach = setting.isVibrateWhenApproach,
            vibrateDistance = setting.vibrateDistance,
            isNightMode = false,
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

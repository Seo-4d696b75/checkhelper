package jp.seo.station.ekisagasu.ui.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.model.UserSetting
import jp.seo.station.ekisagasu.repository.AppStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class SettingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appStateRepository: AppStateRepository,
    private val stationRepository: StationRepository,
) : ViewModel() {

    val dataVersion = stationRepository.dataVersion

    fun checkLatestData(context: Context) =
        viewModelScope.launch(Dispatchers.IO) {
            // TODO
            val latest = try {
                stationRepository.getLatestDataVersion(true)
            } catch (e: IOException) {
                //requestToast.postCall(messageNetworkError)
                return@launch
            }
            val current = stationRepository.getDataVersion()
            if (current == null || latest.version > current.version) {
                //targetInfo = latest
                //requestDialog(DataDialog.DIALOG_LATEST)
            } else {
                //requestToast.postCall(context.getString(R.string.data_already_latest))
            }
        }

    private val _setting = MutableStateFlow(
        SettingState.fromUserSetting(userRepository.setting.value)
    )

    val setting: StateFlow<SettingState> = _setting

    var state: SettingState
        get() = _setting.value
        set(value) {
            _setting.value = value
            userRepository.setting.value = value.toUserSetting()
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
    val nightModeBrightness: Int,
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

package com.seo4d696b75.android.ekisagasu.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.RemoteDataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSetting
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import com.seo4d696b75.android.ekisagasu.ui.update.DataUpdateNavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.update.NavigateDataUpdateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingComposeViewModel @Inject constructor(
    private val settingRepository: UserSettingRepository,
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
    private val remoteDataRepository: RemoteDataRepository,
    private val logger: LogCollector,
    private val navigateDataUpdateEvent: NavigateDataUpdateEvent,
) : ViewModel(),
    LogCollector by logger {

    private val isDataVersionChecking = MutableStateFlow(false)
    private val isLatestData = MutableStateFlow(false)

    private val dataUiState = combine(
        dataRepository.dataVersion,
        isDataVersionChecking,
        isLatestData,
    ) { version, isChecking, isLatest ->
        when {
            version == null -> DataVersionUiState.Undefined
            isChecking -> DataVersionUiState.Checking(version)
            isLatest -> DataVersionUiState.LatestChecked(version)
            else -> DataVersionUiState.Idle(version)
        }
    }

    val uiState: StateFlow<SettingUiState> = combine(
        settingRepository.setting,
        appStateRepository.nightMode,
        dataUiState,
    ) { setting, night, data ->
        SettingUiState.Loaded(
            locationUpdateInterval = SettingItemUiState(
                value = setting.locationUpdateInterval,
                enabled = true,
                onChange = update::locationUpdateInterval,
            ),
            searchSize = SettingItemUiState(
                value = setting.searchK,
                enabled = true,
                onChange = update::searchSize,
            ),
            isPopupEnabled = SettingItemUiState(
                value = setting.isPushNotification,
                enabled = true,
                onChange = update::isPopupEnabled,
            ),
            isPopupForced = SettingItemUiState(
                value = setting.isPushNotificationForce,
                enabled = setting.isPushNotification,
                onChange = update::isPopupForced,
            ),
            isPopupKept = SettingItemUiState(
                value = setting.isKeepNotification,
                enabled = setting.isPushNotification,
                onChange = update::isPopupKept,
            ),
            isPopupPrefectureShown = SettingItemUiState(
                value = setting.isShowPrefectureNotification,
                enabled = setting.isPushNotification,
                onChange = update::isPopupPrefectureShown,
            ),
            isVibrateEnabled = SettingItemUiState(
                value = setting.isVibrate,
                enabled = true,
                onChange = update::isVibrateEnabled,
            ),
            isVibrateOnApproachEnabled = SettingItemUiState(
                value = setting.isVibrateWhenApproach,
                enabled = setting.isVibrate,
                onChange = update::isVibrateOnApproachEnabled,
            ),
            vibrateDistanceOnApproach = SettingItemUiState(
                value = setting.vibrateDistance,
                enabled = setting.isVibrate,
                onChange = update::vibrateDistanceOnApproach,
            ),
            nightScrimTimeout = SettingItemUiState(
                value = NightScrimTimeout.Values.first { it.seconds == setting.nightModeTimeout },
                enabled = true,
                onChange = update::nightScrimTimeout,
            ),
            nightScrimBrightness = SettingItemUiState(
                value = setting.nightModeBrightness,
                enabled = true,
                onChange = update::nightScrimBrightness,
            ),
            isNightMode = night,
            data = data,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        SettingUiState.Initializing,
    )

    // 不要なrecompositionを回避するため毎回ラムダを生成する代わりにメソッド参照を渡す
    private val update = object {
        private operator fun invoke(
            producer: (UserSetting) -> UserSetting,
        ) = viewModelScope.launch {
            settingRepository.update(producer)
        }

        fun locationUpdateInterval(value: Int) = this { it.copy(locationUpdateInterval = value) }
        fun searchSize(value: Int) = this { it.copy(searchK = value) }
        fun isPopupEnabled(enabled: Boolean) = this { it.copy(isPushNotification = enabled) }
        fun isPopupForced(forced: Boolean) = this { it.copy(isPushNotificationForce = forced) }
        fun isPopupKept(keep: Boolean) = this{ it.copy(isKeepNotification = keep) }
        fun isPopupPrefectureShown(show: Boolean) = this{ it.copy(isShowPrefectureNotification = show) }
        fun isVibrateEnabled(enabled: Boolean) = this{ it.copy(isVibrate = enabled) }
        fun isVibrateOnApproachEnabled(enabled: Boolean) = this{ it.copy(isVibrateWhenApproach = enabled) }
        fun vibrateDistanceOnApproach(meter: Int) = this{ it.copy(vibrateDistance = meter) }
        fun nightScrimTimeout(timeout: NightScrimTimeout) = this{ it.copy(nightModeTimeout = timeout.seconds) }
        fun nightScrimBrightness(alpha: Float) = this{ it.copy(nightModeBrightness = alpha) }
    }

    fun updateNightMode(enabled: Boolean) = viewModelScope.launch {
        appStateRepository.setNightMode(enabled)
    }

    fun checkLatestData() = viewModelScope.launch(Dispatchers.IO) {
        val latest = try {
            remoteDataRepository.getLatestDataVersion(false)
        } catch (e: IOException) {
            Timber.w(e)
            // TODO エラー表示
            // appStateRepository.emitMessage(AppMessage.Data.CheckLatestVersionFailure(e))
            log(LogMessage.Data.CheckLatestVersionFailure(e))
            return@launch
        }
        val current = dataRepository.getDataVersion()
        if (current == null || latest.version > current.version) {
            val nav = DataUpdateNavigationEvent.ConfirmUpdate(DataUpdateType.Latest, latest)
            navigateDataUpdateEvent(nav)
            log(LogMessage.Data.LatestVersionFound(latest))
        } else {
            isLatestData.update { true }
        }
    }.apply {
        isDataVersionChecking.update { true }
        invokeOnCompletion {
            isDataVersionChecking.update { false }
        }
    }
}

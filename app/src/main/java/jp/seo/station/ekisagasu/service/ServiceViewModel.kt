package jp.seo.station.ekisagasu.service

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.repository.NavigationRepository
import jp.seo.station.ekisagasu.repository.SearchRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import jp.seo.station.ekisagasu.usecase.AppFinishUseCase
import jp.seo.station.ekisagasu.usecase.BootUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class ServiceViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val logger: AppLogger,
    private val userSettingRepository: UserSettingRepository,
    private val searchRepository: SearchRepository,
    private val navigator: NavigationRepository,
    private val appStateRepository: AppStateRepository,
    private val bootUseCase: BootUseCase,
    private val appFinishUseCase: AppFinishUseCase,
) : ViewModel(), AppLogger by logger {
    /**
     * 現在の探索・待機状態の変更を通知する
     */
    val isRunning = locationRepository.isRunning

    val currentLocation = locationRepository.currentLocation

    fun log(message: String) = viewModelScope.log(message)
    fun error(message: String) = viewModelScope.error(message)

    val message = appStateRepository.message

    val detectedStation = searchRepository.detectedStation
    val nearestStation = searchRepository.nearestStation

    val isNavigatorRunning = navigator.running
    val navigationPrediction =
        navigator.predictions.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val navigationLine
        get() = navigator.line

    val userSetting = userSettingRepository.setting
    fun saveTimerPosition(position: Int) {
        userSettingRepository.setting.value = userSettingRepository.setting.value.copy(
            timerPosition = position
        )
    }

    val selectedLine = searchRepository.selectedLine

    fun saveStationLog(station: Station) = viewModelScope.launch {
        appStateRepository.emitMessage(
            AppMessage.Station(station)
        )
    }

    fun updateLocation(location: Location) = viewModelScope.launch {
        appStateRepository.emitMessage(
            AppMessage.Location(location.latitude, location.longitude)
        )
        searchRepository.updateNearestStations(location)
        searchRepository.nearestStation.value?.let {
            navigator.updateLocation(location, it.station)
        }
    }

    fun stopStationSearch() {
        locationRepository.stopWatchCurrentLocation()
        searchRepository.onStopSearch()
        navigator.stop()
    }

    fun onServiceInit() = viewModelScope.launch {
        bootUseCase()
    }

    /**
     * 必要ならActivity側に通知して終了させる
     */
    fun requestAppFinish() = viewModelScope.launch {
        appStateRepository.emitMessage(
            AppMessage.FinishApp
        )
    }

    val appFinish = appStateRepository
        .message
        .filterIsInstance<AppMessage.FinishApp>()
        .onEach {
            stopStationSearch()
            log("service terminated")
            appFinishUseCase()
        }

    // accessor to app state
    val startTimer = appStateRepository.message.filterIsInstance<AppMessage.StartTimer>()
    val fixTimer = appStateRepository.fixTimer
    val nightMode = appStateRepository.nightMode

    fun setSearchK(k: Int) = viewModelScope.launch {
        searchRepository.setSearchK(k)
    }

    fun setSearchInterval(sec: Int) {
        if (locationRepository.isRunning.value) {
            locationRepository.startWatchCurrentLocation(sec)
        }
    }

    fun clearNavigationLine() {
        searchRepository.selectLine(null)
        navigator.stop()
    }
}

package jp.seo.station.ekisagasu.service

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.NavigationRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.repository.*
import jp.seo.station.ekisagasu.usecase.AppFinishUseCase
import jp.seo.station.ekisagasu.usecase.BootUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalCoroutinesApi
class ServiceViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val logRepository: LogRepository,
    private val logger: AppLogger,
    private val userSettingRepository: UserSettingRepository,
    private val stationRepository: StationRepository,
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

    val message = logRepository.message

    val detectedStation = stationRepository.detectedStation
    val nearestStation = stationRepository.nearestStation

    val isNavigatorRunning = navigator.running
    val navigationPrediction = navigator.predictions
    val navigationLine = navigator.line

    val userSetting = userSettingRepository.setting
    fun saveTimerPosition(position: Int) {
        userSettingRepository.setting.value = userSettingRepository.setting.value.copy(
            timerPosition = position
        )
    }

    val selectedLine = stationRepository.selectedLine

    fun saveStationLog(station: Station) = viewModelScope.launch {
        logRepository.logStation(station)
    }

    fun updateLocation(location: Location) = viewModelScope.launch {
        logRepository.logLocation(location.latitude, location.longitude)
        stationRepository.updateNearestStations(location)
        stationRepository.nearestStation.value?.let {
            navigator.updateLocation(location, it.station)
        }
    }

    fun stopStationSearch() {
        locationRepository.stopWatchCurrentLocation()
        stationRepository.onStopSearch()
        navigator.stop()
    }

    fun onServiceInit() = viewModelScope.launch {
        bootUseCase()
    }

    /**
     * 必要ならActivity側に通知して終了させる
     */
    fun requestAppFinish() = viewModelScope.launch {
        appStateRepository.finishApp()
    }

    val appFinish = appStateRepository.finishAppEvent

    fun onServiceFinish() = viewModelScope.launch {
        appFinishUseCase()
    }

    // accessor to app state
    val startTimer = appStateRepository.startTimerEvent
    val fixTimer = appStateRepository.fixTimer
    val nightMode = appStateRepository.nightMode

    fun setSearchK(k: Int) = viewModelScope.launch {
        stationRepository.setSearchK(k)
    }

    fun setSearchInterval(sec: Int) {
        if (locationRepository.isRunning.value) {
            locationRepository.startWatchCurrentLocation(sec)
        }
    }

    fun clearNavigationLine() {
        stationRepository.selectLine(null)
        navigator.stop()
    }
}
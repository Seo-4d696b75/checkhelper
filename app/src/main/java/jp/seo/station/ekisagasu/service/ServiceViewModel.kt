package jp.seo.station.ekisagasu.service

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.NavigationRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.usecase.AppFinishUseCase
import jp.seo.station.ekisagasu.usecase.BootUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject

class ServiceViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val logger: AppLogger,
    private val stationRepository: StationRepository,
    private val navigator: NavigationRepository,
    private val userRepository: UserRepository,
    private val appStateRepository: AppStateRepository,
    private val bootUseCase: BootUseCase,
    private val appFinishUseCase: AppFinishUseCase,
) : ViewModel() {
    /**
     * 現在の探索・待機状態の変更を通知する
     */
    val isRunning = locationRepository.isRunning

    fun message(text: String) = viewModelScope.launch { logger.log(text) }

    fun error(text: String, displayedText: String) =
        viewModelScope.launch { logger.error(text, displayedText) }

    val currentLocation = locationRepository.currentLocation

    val log = logger.message

    val detectedStation = stationRepository.detectedStation
    val nearestStation = stationRepository.nearestStation

    val isNavigatorRunning = navigator.running
    val navigationPrediction = navigator.predictions
    val navigationLine = navigator.line

    fun saveMessage(message: AppMessage) = viewModelScope.launch(Dispatchers.IO) {
        when (message) {
            is AppMessage.AppLog -> userRepository.logMessage(message.message)
            is AppMessage.AppError -> {
                val str = if (message.cause == null) {
                    message.message
                } else {
                    val sw = StringWriter()
                    val pw = PrintWriter(sw)
                    message.cause.printStackTrace(pw)
                    String.format("%s caused by;\n%s", message.message, sw.toString())
                }
                userRepository.logError(str)
            }
            is AppMessage.AppResolvableException -> userRepository.logError(message.exception.toString())
        }
    }

    val selectedLine = stationRepository.selectedLine

    fun saveStationLog(station: Station) = viewModelScope.launch {
        userRepository.logStation(String.format("%s(%d)", station.name, station.code))
    }

    fun updateLocation(location: Location) = viewModelScope.launch {
        userRepository.logLocation(location.latitude, location.longitude)
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
package jp.seo.station.ekisagasu.service

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.data.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.data.message.AppMessage
import com.seo4d696b75.android.ekisagasu.data.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.data.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.data.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.data.station.Station
import com.seo4d696b75.android.ekisagasu.data.user.UserSettingRepository
import jp.seo.station.ekisagasu.usecase.AppFinishUseCase
import jp.seo.station.ekisagasu.usecase.BootUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ServiceViewModel
    @Inject
    constructor(
        private val locationRepository: LocationRepository,
        private val userSettingRepository: UserSettingRepository,
        private val searchRepository: StationSearchRepository,
        private val navigator: NavigatorRepository,
        private val appStateRepository: AppStateRepository,
        private val bootUseCase: BootUseCase,
        private val appFinishUseCase: AppFinishUseCase,
    ) : ViewModel() {
        /**
         * 現在の探索・待機状態の変更を通知する
         */
        val isRunning = locationRepository.isRunning

        val currentLocation = locationRepository.currentLocation

        val detectedStation = searchRepository.detectedStation
        val nearestStation = searchRepository.nearestStation

        val isNavigatorRunning = navigator.running
        val navigationPrediction =
            navigator.predictions.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        val navigationLine
            get() = navigator.line

        val userSetting = userSettingRepository.setting

        fun saveTimerPosition(position: Int) =
            userSettingRepository.update {
                it.copy(timerPosition = position)
            }

        val selectedLine = searchRepository.selectedLine

        fun saveStationLog(station: Station) =
            viewModelScope.launch {
                appStateRepository.emitMessage(
                    AppMessage.Station(station),
                )
            }

        fun updateLocation(location: Location) =
            viewModelScope.launch {
                appStateRepository.emitMessage(
                    AppMessage.Location(location.latitude, location.longitude),
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

        fun onServiceInit() =
            viewModelScope.launch {
                bootUseCase()
            }

        /**
         * 必要ならActivity側に通知して終了させる
         */
        fun requestAppFinish() =
            viewModelScope.launch {
                appStateRepository.emitMessage(
                    AppMessage.FinishApp,
                )
            }

        val appFinish =
            appStateRepository
                .message
                .filterIsInstance<AppMessage.FinishApp>()
                .onEach {
                    stopStationSearch()
                    Timber.tag("Service").d("service terminated")
                    appFinishUseCase()
                }

        // accessor to app state
        val startTimer = appStateRepository.message.filterIsInstance<AppMessage.StartTimer>()
        val fixTimer = appStateRepository.fixTimer
        val nightMode = appStateRepository.nightMode

        fun setSearchK(k: Int) =
            viewModelScope.launch {
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

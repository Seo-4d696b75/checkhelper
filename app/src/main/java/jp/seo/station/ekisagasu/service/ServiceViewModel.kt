package jp.seo.station.ekisagasu.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.data.user.UserSettingRepository
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import jp.seo.station.ekisagasu.usecase.AppFinishUseCase
import jp.seo.station.ekisagasu.usecase.BootUseCase
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ServiceViewModel @Inject constructor(
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
    val currentLocation = locationRepository.currentLocation.filterNotNull()
    val detectedStation = searchRepository.detectedStation.filterNotNull()
    val nearestStation = searchRepository.nearestStation
    val isNavigatorRunning = navigator.running
    val navigationPrediction = navigator.predictions.filterNotNull()

    val navigationLine
        get() = navigator.line

    val userSetting = userSettingRepository.setting

    fun saveTimerPosition(position: Int) = userSettingRepository.update {
        it.copy(timerPosition = position)
    }

    val selectedLine = searchRepository.selectedLine

    fun updateLocation(location: Location) = viewModelScope.launch {
        val nearest = searchRepository.updateNearestStations(location)
        nearest?.let {
            navigator.updateLocation(location, it.station)
        }
    }

    private fun stopStationSearch() {
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
            AppMessage.FinishApp,
        )
    }

    val appFinish = appStateRepository
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

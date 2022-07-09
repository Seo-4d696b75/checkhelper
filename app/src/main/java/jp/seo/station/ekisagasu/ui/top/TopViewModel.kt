package jp.seo.station.ekisagasu.ui.top

import android.content.Context
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.NavigationRepository
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.utils.combineLiveData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val locationRepository: LocationRepository,
    private val stationRepository: StationRepository,
    private val settingRepository: UserSettingRepository,
    private val navigationRepository: NavigationRepository,
    private val prefectureRepository: PrefectureRepository,
    private val appStateRepository: AppStateRepository,
) : ViewModel() {

    val isRunning = locationRepository.isRunning

    val nearestStation = stationRepository.nearestStation
    val station = nearestStation.map { it?.station }
    val nearestStationPrefecture = nearestStation.map { n ->
        n?.let {
            prefectureRepository.getName(it.station.prefecture)
        } ?: ""
    }
    val selectedLine = stationRepository.selectedLine

    val state: LiveData<SearchState> = combineLiveData(
        SearchState.STOPPED,
        isRunning.asLiveData(),
        stationRepository.nearestStation
    ) { run, station ->
        if (run) {
            if (station == null) SearchState.STARTING else SearchState.RUNNING
        } else {
            SearchState.STOPPED
        }
    }

    fun onSearchStateChanged() {
        if (isRunning.value) {
            if (stationRepository.dataInitialized) {
                val interval = settingRepository.setting.value.locationUpdateInterval
                locationRepository.startWatchCurrentLocation(interval)
            }
        } else {
            // TODO このへんの処理がviewModelで重複してる？
            locationRepository.stopWatchCurrentLocation()
            stationRepository.onStopSearch()
            navigationRepository.stop()
        }
    }

    private val _event = MutableSharedFlow<TopFragmentEvent>()
    val event: SharedFlow<TopFragmentEvent> = _event

    fun openMenu() = viewModelScope.launch {
        _event.emit(TopFragmentEvent.ToggleMenu(true))
    }

    fun closeMenu() = viewModelScope.launch {
        _event.emit(TopFragmentEvent.ToggleMenu(false))
    }

    fun finishApp() = viewModelScope.launch {
        appStateRepository.finishApp()
    }

    fun selectCurrentLine() = viewModelScope.launch {
        _event.emit(TopFragmentEvent.SelectLine)
        closeMenu()
    }

    fun startNavigation() = viewModelScope.launch {
        _event.emit(TopFragmentEvent.StartNavigation)
        closeMenu()
    }

    fun showMap() = viewModelScope.launch {
        _event.emit(TopFragmentEvent.ShowMap)
    }

    fun startTimer() = viewModelScope.launch {
        appStateRepository.startTimer()
        closeMenu()
    }

    fun fixTimer() = viewModelScope.launch {
        val current = appStateRepository.fixTimer.value
        appStateRepository.setTimerFixed(!current)
        closeMenu()
    }
}

enum class SearchState {
    STOPPED,
    STARTING,
    RUNNING
}

sealed interface TopFragmentEvent {
    object ShowMap : TopFragmentEvent
    object SelectLine : TopFragmentEvent
    object StartNavigation : TopFragmentEvent
    data class ToggleMenu(val open: Boolean) : TopFragmentEvent
}
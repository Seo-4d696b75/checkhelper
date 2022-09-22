package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.repository.NavigationRepository
import jp.seo.station.ekisagasu.repository.PrefectureRepository
import jp.seo.station.ekisagasu.repository.SearchRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import jp.seo.station.ekisagasu.utils.mapState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val searchRepository: SearchRepository,
    private val dataRepository: DataRepository,
    settingRepository: UserSettingRepository,
    private val navigationRepository: NavigationRepository,
    private val prefectureRepository: PrefectureRepository,
    private val appStateRepository: AppStateRepository,
) : ViewModel() {

    private val interval = settingRepository.setting
        .map { it.locationUpdateInterval }
        .stateIn(viewModelScope, SharingStarted.Lazily, 5)

    val isRunning = locationRepository.isRunning

    val nearestStation = searchRepository.nearestStation
    val station = nearestStation.mapState(viewModelScope) { it?.station }
    val nearestStationPrefecture = nearestStation.mapState(viewModelScope) { n ->
        n?.let {
            prefectureRepository.getName(it.station.prefecture)
        } ?: ""
    }
    val selectedLine = searchRepository.selectedLine

    val state = combine(
        isRunning,
        searchRepository.nearestStation,
    ) { run, station ->
        if (run) {
            if (station == null) SearchState.STARTING else SearchState.RUNNING
        } else {
            SearchState.STOPPED
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchState.STOPPED)

    fun onSearchStateChanged() {
        if (isRunning.value) {
            locationRepository.stopWatchCurrentLocation()
            searchRepository.onStopSearch()
            navigationRepository.stop()
        } else {
            if (dataRepository.dataInitialized) {
                locationRepository.startWatchCurrentLocation(interval.value)
            }
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
        appStateRepository.emitMessage(AppMessage.FinishApp)
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
        appStateRepository.emitMessage(AppMessage.StartTimer)
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

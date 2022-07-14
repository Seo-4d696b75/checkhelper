package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.*
import jp.seo.station.ekisagasu.utils.mapState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val searchRepository: SearchRepository,
    private val dataRepository: DataRepository,
    private val settingRepository: UserSettingRepository,
    private val navigationRepository: NavigationRepository,
    private val prefectureRepository: PrefectureRepository,
    private val appStateRepository: AppStateRepository,
) : ViewModel() {

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
    }

    fun onSearchStateChanged() {
        if (isRunning.value) {
            if (dataRepository.dataInitialized) {
                val interval = settingRepository.setting.value.locationUpdateInterval
                locationRepository.startWatchCurrentLocation(interval)
            }
        } else {
            // TODO このへんの処理がviewModelで重複してる？
            locationRepository.stopWatchCurrentLocation()
            searchRepository.onStopSearch()
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

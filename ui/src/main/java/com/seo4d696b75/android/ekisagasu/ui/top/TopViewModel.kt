package com.seo4d696b75.android.ekisagasu.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.coroutine.mapStateIn
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationState
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    searchRepository: StationSearchRepository,
    private val dataRepository: DataRepository,
    private val appStateRepository: AppStateRepository,
) : ViewModel() {
    val isRunning = locationRepository
        .currentLocation
        .map { it is LocationState.Running }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    val nearestStation = searchRepository.state.mapStateIn(viewModelScope, null) {
        when (it) {
            is StationSearchState.Result -> it.nearest
            else -> null
        }
    }
    val station = nearestStation.mapStateIn(viewModelScope) { it?.station }
    val nearestStationPrefecture = nearestStation.mapStateIn(viewModelScope) { n ->
        n?.station?.prefecture?.name ?: ""
    }

    val selectedLine = searchRepository.selectedLine

    val state = searchRepository.state.map {
        when (it) {
            is StationSearchState.Idle -> SearchState.STOPPED
            is StationSearchState.Initializing -> SearchState.STARTING
            is StationSearchState.Result -> SearchState.RUNNING
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        SearchState.STOPPED,
    )

    fun onSearchStateChanged() = viewModelScope.launch {
        if (isRunning.first()) {
            locationRepository.stopWatchCurrentLocation()
        } else {
            if (dataRepository.dataInitialized) {
                locationRepository.startWatchCurrentLocation()
            }
        }
    }

    private val _event = MutableSharedFlow<TopFragmentEvent>()
    val event: SharedFlow<TopFragmentEvent> = _event

    fun openMenu() =
        viewModelScope.launch {
            _event.emit(TopFragmentEvent.ToggleMenu(true))
        }

    fun closeMenu() =
        viewModelScope.launch {
            _event.emit(TopFragmentEvent.ToggleMenu(false))
        }

    fun finishApp() =
        viewModelScope.launch {
            appStateRepository.requestAppFinish()
        }

    fun selectCurrentLine() =
        viewModelScope.launch {
            _event.emit(TopFragmentEvent.SelectLine)
            closeMenu()
        }

    fun startNavigation() =
        viewModelScope.launch {
            _event.emit(TopFragmentEvent.StartNavigation)
            closeMenu()
        }

    fun showMap() =
        viewModelScope.launch {
            _event.emit(TopFragmentEvent.ShowMap)
        }

    fun startTimer() =
        viewModelScope.launch {
            appStateRepository.emitMessage(AppMessage.StartTimer)
            closeMenu()
        }
}

enum class SearchState {
    STOPPED,
    STARTING,
    RUNNING,
}

sealed interface TopFragmentEvent {
    data object ShowMap : TopFragmentEvent

    data object SelectLine : TopFragmentEvent

    data object StartNavigation : TopFragmentEvent

    data class ToggleMenu(val open: Boolean) : TopFragmentEvent
}

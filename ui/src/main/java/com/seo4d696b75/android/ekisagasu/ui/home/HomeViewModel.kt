package com.seo4d696b75.android.ekisagasu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val dataRepository: DataRepository,
    private val appStateRepository: AppStateRepository,
    searchRepository: StationSearchRepository,
    handler: ErrorHandler,
) : ViewModel(),
    ErrorHandler by handler,
    NavigationEventHolder<HomeViewModel.Nav> by navigationEventHolder() {

    private val visible = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        visible,
        searchRepository.state,
        searchRepository.selectedLine,
    ) { isVisible, state, selectedLine ->
        if (isVisible) {
            when (state) {
                is StationSearchState.Idle -> HomeUiState.Idle
                is StationSearchState.Initializing -> HomeUiState.Initializing
                is StationSearchState.Result -> HomeUiState.Result(
                    station = state.nearest,
                    selectedLine = selectedLine,
                )
            }
        } else {
            HomeUiState.Invisible
        }
    }.stateInCatching(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        HomeUiState.Idle,
    )

    fun onVisibilityChanged(visible: Boolean) {
        this.visible.update { visible }
    }

    fun onSearchStateChanged() = viewModelScope.launchCatching {
        when (uiState.value) {
            HomeUiState.Invisible -> {}

            HomeUiState.Idle -> if (dataRepository.dataInitialized) {
                locationRepository.startWatchCurrentLocation()
            }

            is HomeUiState.Running -> locationRepository.stopWatchCurrentLocation()
        }
    }

    fun onFinishClicked() = viewModelScope.launchCatching {
        appStateRepository.requestAppFinish()
    }

    fun onSelectLineClicked() {

    }

    fun onLineNavigatorClicked() {

    }

    fun onTimerClicked() {

    }

    fun onMapClicked() {

    }

    fun onStationClicked(station: Station) {
        navigate(Nav.ShowStation(station))
    }

    fun onLineClicked(line: Line) {
        navigate(Nav.ShowLine(line))
    }

    sealed interface Nav : NavigationEvent {
        data class ShowStation(val station: Station) : Nav
        data class ShowLine(val line: Line) : Nav
    }
}

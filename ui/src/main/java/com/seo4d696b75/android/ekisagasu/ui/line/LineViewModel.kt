package com.seo4d696b75.android.ekisagasu.ui.line

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class LineViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    savedStateHandle: SavedStateHandle,
    handler: ErrorHandler,
) : ViewModel(),
    ErrorHandler by handler,
    NavigationEventHolder<LineViewModel.Nav> by navigationEventHolder() {

    private val args = savedStateHandle.toRoute<NavigationRoute.Home.Line>()

    val uiState = flow {
        emit(LineUiState.Initializing)
        val line = dataRepository.getLine(args.code)
        val stations = dataRepository.getStations(line.stationList.map { it.code })
        emit(
            LineUiState.Loaded(
                line = line,
                stations = line.stationList.map { r ->
                    val s = stations.find { it.code == r.code } ?: throw NoSuchElementException()
                    StationRegistrationUiState(r.code, s, r.getNumberingString())
                },
            )
        )
    }.stateInCatching(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        LineUiState.Initializing,
    )

    fun onClose() {
        navigate(Nav.Close)
    }

    fun onMapClicked() {
        val state = uiState.value as? LineUiState.Loaded ?: return
        navigate(Nav.ShowMap(state.line))
    }

    fun onStationClicked(station: Station) {
        navigate(Nav.ShowStation(station))
    }

    sealed interface Nav : NavigationEvent {
        data object Close : Nav
        data class ShowMap(val line: Line) : Nav
        data class ShowStation(val station: Station) : Nav
    }
}

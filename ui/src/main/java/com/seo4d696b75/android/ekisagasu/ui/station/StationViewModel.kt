package com.seo4d696b75.android.ekisagasu.ui.station

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
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class StationViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    savedStateHandle: SavedStateHandle,
    errorHandler: ErrorHandler,
) : ViewModel(),
    ErrorHandler by errorHandler,
    NavigationEventHolder<StationViewModel.Nav> by navigationEventHolder() {
    private val args = savedStateHandle.toRoute<NavigationRoute.Home.Station>(typeMap)

    val uiState = flow<StationUiState> {
        emit(StationUiState.Initializing)
        val station = dataRepository.getStation(args.code)
        emit(
            StationUiState.Loaded(
                station = station,
                lines = station.lines,
            )
        )
    }.stateInCatching(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        StationUiState.Initializing,
    )

    fun onClose() {
        navigate(Nav.Close)
    }

    fun onMapClicked() {
        val state = uiState.value as? StationUiState.Loaded ?: return
        navigate(Nav.ShowMap(state.station))
    }

    fun onLineClicked(line: Line) {
        navigate(Nav.ShowLine(line))
    }

    sealed interface Nav : NavigationEvent {
        data object Close : Nav
        data class ShowMap(val station: Station) : Nav
        data class ShowLine(val line: Line) : Nav
    }
}

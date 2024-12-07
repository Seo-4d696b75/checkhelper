package com.seo4d696b75.android.ekisagasu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    searchRepository: StationSearchRepository,
    private val dataRepository: DataRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        searchRepository.state,
        searchRepository.selectedLine,
    ) { state, selectedLine ->
        when (state) {
            is StationSearchState.Idle -> HomeUiState.Idle
            is StationSearchState.Initializing -> HomeUiState.Initializing
            is StationSearchState.Result -> HomeUiState.Result(
                station = state.nearest,
                selectedLine = selectedLine,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        HomeUiState.Idle,
    )

    fun onSearchStateChanged() = viewModelScope.launch {
        when (uiState.value) {
            HomeUiState.Idle -> if (dataRepository.dataInitialized) {
                locationRepository.startWatchCurrentLocation()
            }

            is HomeUiState.Running -> locationRepository.stopWatchCurrentLocation()
        }
    }
}

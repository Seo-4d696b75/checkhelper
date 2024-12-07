package com.seo4d696b75.android.ekisagasu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationState
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    locationRepository: LocationRepository,
    searchRepository: StationSearchRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        locationRepository.currentLocation,
        searchRepository.result,
        searchRepository.selectedLine,
    ) { location, result, selectedLine ->
        when {
            location is LocationState.Idle -> HomeUiState.Idle
            result == null -> HomeUiState.Initializing
            else -> HomeUiState.Result(
                station = result.nearest,
                selectedLine = selectedLine,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        HomeUiState.Idle,
    )
}

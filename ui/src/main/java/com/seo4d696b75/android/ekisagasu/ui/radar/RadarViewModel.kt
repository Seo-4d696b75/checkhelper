package com.seo4d696b75.android.ekisagasu.ui.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    searchRepository: StationSearchRepository,
) : ViewModel(),
    NavigationEventHolder<RadarViewModel.ShowStation> by navigationEventHolder() {

    val uiState: StateFlow<RadarUiState> = searchRepository
        .state
        .map {
            when (it) {
                is StationSearchState.Idle -> RadarUiState.None
                is StationSearchState.Initializing -> RadarUiState.Initializing(it.searchK)
                is StationSearchState.Result -> RadarUiState.Data(it.searchK, it.nears)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            RadarUiState.None,
        )

    fun onStationClicked(station: Station) {
        navigate(ShowStation(station))
    }

    data class ShowStation(val station: Station) : NavigationEvent
}

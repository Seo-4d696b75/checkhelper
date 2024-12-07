package com.seo4d696b75.android.ekisagasu.ui.top.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    searchRepository: StationSearchRepository,
    userSettingRepository: UserSettingRepository,
) : ViewModel() {
    val radarList = searchRepository.state.map {
        when (it) {
            is StationSearchState.Result -> it.nears
            else -> null
        }
    }

    val radarK = userSettingRepository
        .setting
        .map { it.searchK }
        .stateIn(viewModelScope, SharingStarted.Lazily, 12)
}

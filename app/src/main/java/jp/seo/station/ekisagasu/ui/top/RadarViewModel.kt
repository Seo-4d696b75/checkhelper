package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.repository.SearchRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RadarViewModel
    @Inject
    constructor(
        searchRepository: SearchRepository,
        userSettingRepository: UserSettingRepository,
    ) : ViewModel() {
        val radarList = searchRepository.nearestStations

        val radarK =
            userSettingRepository.setting
                .map { it.searchK }
                .stateIn(viewModelScope, SharingStarted.Lazily, 12)
    }

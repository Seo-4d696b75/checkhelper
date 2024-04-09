package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.data.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.data.user.UserSettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RadarViewModel
    @Inject
    constructor(
        searchRepository: StationSearchRepository,
        userSettingRepository: UserSettingRepository,
    ) : ViewModel() {
        val radarList = searchRepository.nearestStations

        val radarK =
            userSettingRepository.setting
                .map { it.searchK }
                .stateIn(viewModelScope, SharingStarted.Lazily, 12)
    }

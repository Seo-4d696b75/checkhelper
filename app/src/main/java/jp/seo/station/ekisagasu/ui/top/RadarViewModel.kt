package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import jp.seo.station.ekisagasu.utils.mapState
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    stationRepository: StationRepository,
    userSettingRepository: UserSettingRepository,
): ViewModel() {

    val radarList = stationRepository.nearestStations

    val radarK = userSettingRepository.setting.mapState(viewModelScope) { it.searchK }
}
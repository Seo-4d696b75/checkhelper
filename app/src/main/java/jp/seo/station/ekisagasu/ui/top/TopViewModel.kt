package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.utils.combineLiveData
import javax.inject.Inject

@HiltViewModel
class TopViewModel @Inject constructor(
    locationRepository: LocationRepository,
    stationRepository: StationRepository,
    appStateRepository: AppStateRepository,
) : ViewModel() {

    val isRunning = locationRepository.isRunning

    val nearestStation = stationRepository.nearestStation
    val selectedLine = stationRepository.selectedLine

    val fixTimer = appStateRepository.fixTimer

    val state: LiveData<SearchState> = combineLiveData(
        SearchState.STOPPED,
        isRunning.asLiveData(),
        stationRepository.nearestStation
    ) { run, station ->
        if (run) {
            if (station == null) SearchState.STARTING else SearchState.RUNNING
        } else {
            SearchState.STOPPED
        }
    }
}

enum class SearchState {
    STOPPED,
    STARTING,
    RUNNING
}
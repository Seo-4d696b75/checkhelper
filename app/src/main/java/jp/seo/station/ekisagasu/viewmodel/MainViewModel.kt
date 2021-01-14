package jp.seo.station.ekisagasu.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.utils.combineLiveData
import jp.seo.station.ekisagasu.utils.getViewModelFactory

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
class MainViewModel(
    private val service: StationService
) : ViewModel() {

    companion object {
        fun getFactory(service: StationService): ViewModelProvider.Factory =
            getViewModelFactory {
                MainViewModel(service)
            }
    }

    enum class SearchState {
        STOPPED,
        STARTING,
        RUNNING
    }

    val state: LiveData<SearchState> = combineLiveData(
        SearchState.STOPPED,
        service.isRunning,
        service.stationRepository.nearestStation
    ) { run, station ->
        if (run) {
            if (station == null) SearchState.STARTING else SearchState.RUNNING
        } else {
            SearchState.STOPPED
        }
    }

    val running = service.isRunning

    fun toggleStart() {
        running.value?.let { state ->
            if (state) {
                service.stop()
            } else {
                service.start()
            }
        }
    }

    val nearestStation = service.stationRepository.nearestStation
    val radarList = service.stationRepository.nearestStations
    var radarNum = service.userRepository.searchK
    val selectedLine = service.stationRepository.selectedLine

}

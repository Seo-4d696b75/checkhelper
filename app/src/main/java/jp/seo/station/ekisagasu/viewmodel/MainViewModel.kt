package jp.seo.station.ekisagasu.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.utils.combineLiveData
import jp.seo.station.ekisagasu.utils.getViewModelFactory
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
class MainViewModel(
    private val service: StationService
) : ViewModel() {

    companion object {

        /**
         * ViewModelインスタンスを取得する
         * @param owner 共通のインスタンスを取得する必要がある場合はDIなど利用して同一のstoreを渡す
         */
        fun getInstance(owner: ViewModelStoreOwner, service: StationService): MainViewModel {
            val factory = getViewModelFactory { MainViewModel(service) }
            return ViewModelProvider(owner, factory).get(MainViewModel::class.java)
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

    /**
     * 現在位置から最近傍の駅の情報
     */
    val nearestStation = service.stationRepository.nearestStation

    /**
     * 現在位置から近い順に[radarNum]個の駅
     *
     * `list[0]`は[nearestStation]と同じ
     */
    val radarList = service.stationRepository.nearestStations

    /**
     * 現在地から探索する駅の個数
     */
    var radarNum = service.userRepository.searchK

    /**
     * 現在選択している路線
     */
    val selectedLine = service.stationRepository.selectedLine

    private val _stationInDetail = MutableLiveData<Station?>(null)
    val stationInDetail: LiveData<Station?> = _stationInDetail
    fun showStationInDetail(station: Station) {
        _stationInDetail.value = station
    }

    val linesInDetail: LiveData<List<Line>> = stationInDetail.switchMap { s ->
        liveData {
            emit(
                s?.let { station ->
                    service.stationRepository.getLines(station.lines)
                } ?: Collections.emptyList<Line>()
            )
        }
    }

    private val _lineInDetail = MutableLiveData<Line?>(null)
    fun showLineInDetail(index: Int) {
        linesInDetail.value?.let { list ->
            _lineInDetail.value = list[index]
        }
    }

    val lineInDetail: LiveData<Line?> = _lineInDetail
}

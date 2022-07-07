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
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.utils.getViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.NoSuchElementException

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@ExperimentalCoroutinesApi
class MainViewModel(
    private val stationRepository: StationRepository,
    userRepository: UserRepository,
) : ViewModel() {

    companion object {

        /**
         * ViewModelインスタンスを取得する
         * @param owner 共通のインスタンスを取得する必要がある場合はDIなど利用して同一のstoreを渡す
         */
        fun getInstance(
            owner: ViewModelStoreOwner,
            stationRepository: StationRepository,
            userRepository: UserRepository
        ): MainViewModel {
            val factory = getViewModelFactory {
                MainViewModel(
                    stationRepository,
                    userRepository
                )
            }
            return ViewModelProvider(owner, factory).get(MainViewModel::class.java)
        }
    }

    /**
     * 現在位置から最近傍の駅の情報
     */
    val nearestStation = stationRepository.nearestStation

    /**
     * 現在位置から近い順に[radarNum]個の駅
     *
     * `list[0]`は[nearestStation]と同じ
     */
    val radarList = stationRepository.nearestStations

    /**
     * 現在地から探索する駅の個数
     */
    var radarNum = userRepository.setting.map { it.searchK }

    /**
     * 現在選択している路線
     */
    val selectedLine = stationRepository.selectedLine

    private val _stationInDetail = MutableLiveData<Station?>(null)
    val stationInDetail: LiveData<Station?> = _stationInDetail
    fun showStationInDetail(station: Station) {
        _stationInDetail.value = station
    }

    fun showStationInDetail(index: Int) {
        stationRegisterList.value?.let { list ->
            _stationInDetail.value = list[index].station
        }
    }

    val linesInDetail: LiveData<List<Line>> = stationInDetail.switchMap { s ->
        liveData {
            emit(
                s?.let { station ->
                    stationRepository.getLines(station.lines)
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

    fun showLineInDetail(line: Line) {
        _lineInDetail.value = line
    }

    val lineInDetail: LiveData<Line?> = _lineInDetail
    val stationRegisterList: LiveData<List<StationRegister>> = _lineInDetail.switchMap {
        liveData {
            emit(it?.let { line ->
                val indices = line.stationList.map { r -> r.code }
                val stations = stationRepository.getStations(indices)
                line.stationList.map { r ->
                    val station =
                        stations.find { it.code == r.code } ?: throw NoSuchElementException()
                    StationRegister(r.code, station, r.getNumberingString())
                }
            } ?: Collections.emptyList<StationRegister>())
        }
    }

    data class StationRegister(
        val code: Int,
        val station: Station,
        val numbering: String
    )
}

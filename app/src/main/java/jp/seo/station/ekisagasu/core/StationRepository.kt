package jp.seo.station.ekisagasu.core

import android.location.Location
import android.os.Looper
import androidx.annotation.MainThread
import androidx.core.os.HandlerCompat
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.model.NearStation
import jp.seo.station.ekisagasu.search.KdTree
import jp.seo.station.ekisagasu.search.measureDistance
import jp.seo.station.ekisagasu.usecase.DataUpdateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.floor

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
class StationRepository(
    private val dao: StationDao,
    private val api: APIClient,
    private val tree: KdTree,
    private val updateUseCase: DataUpdateUseCase,
) {

    suspend fun getLines(codes: Array<Int>) = withContext(Dispatchers.IO) {
        dao.getLines(codes)
    }

    suspend fun getStations(codes: List<Int>) = withContext(Dispatchers.IO) {
        dao.getStations(codes)
    }

    private val _currentVersion = MutableStateFlow<DataVersion?>(null)
    private var _dataInitialized: Boolean = false
    private var _lastCheckedVersion: DataLatestInfo? = null
    private var _lastCheckedLocation: Location? = null
    private var _lastSearchK: Int? = null
    private val _currentStation = MutableStateFlow<NearStation?>(null)
    private val _nearestStation = MutableStateFlow<NearStation?>(null)
    private val _selectedLine = MutableStateFlow<Line?>(null)
    private val _nearestStations = MutableStateFlow<List<NearStation>>(emptyList())

    private suspend fun searchNearestStations(
        lat: Double,
        lng: Double,
        k: Int,
        r: Double
    ): KdTree.SearchResult = withContext(Dispatchers.IO) {
        if (!_dataInitialized) {
            throw IllegalStateException("data not initialized yet")
        }
        tree.search(lat, lng, k, r, false)
    }

    private fun checkNeedUpdate(location: Location): Boolean {
        val last = _lastCheckedLocation
        if (last != null && last.longitude == location.longitude && last.latitude == location.latitude) return false
        _lastCheckedLocation = location
        return true
    }

    private val updateMutex = Mutex()

    private var searchK: Int = 12
    suspend fun setSearchK(value: Int) {
        if (value != searchK) {
            searchK = value
            _lastCheckedLocation?.let {
                updateNearestStations(it)
            }

        }
    }

    @MainThread
    suspend fun updateNearestStations(location: Location) = updateMutex.withLock {
        if (searchK < 1) return
        if (!checkNeedUpdate(location)) return
        val result = searchNearestStations(location.latitude, location.longitude, searchK, 0.0)
        if (result.stations.isEmpty()) return

        val nearest = result.stations[0]
        val current = _currentStation.value
        val time = Date(location.time)
        val list = result.stations.map { s ->
            val lines = getLines(s.lines)
            NearStation(s, measureDistance(s, location), time, lines)
        }
        _nearestStations.value = list
        _nearestStation.value = list[0]
        if (current == null || current.station != nearest) {
            _currentStation.value = list[0]
        }
        _lastCheckedLocation = location
    }

    @MainThread
    fun selectLine(line: Line?) {
        _selectedLine.value = line
    }

    @MainThread
    fun onStopSearch() {
        _currentStation.value = null
        _selectedLine.value = null
        _nearestStation.value = null
        _nearestStations.value = emptyList()
        _lastCheckedLocation = null
        _lastSearchK = null
    }

    /**
     * 現在位置から最近傍の駅と距離情報
     * 探索を開始し位置情報を更新された状態でのみ `not-Null`
     */
    val nearestStation: StateFlow<NearStation?> = _nearestStation

    val selectedLine: StateFlow<Line?> = _selectedLine

    /**
     * 現在位置からの近傍駅を近い順にソートしたリスト
     */
    val nearestStations: StateFlow<List<NearStation>> = _nearestStations

    /**
     * 現在位置からの最近傍の駅
     * [nearestStation]とは異なり現在位置が変化しても更新されず、駅が変化したタイミングでのみ更新される
     * [NearStation]の距離・タイムスタンプは更新されたときの値のまま保持される
     */
    val detectedStation: StateFlow<NearStation?> = _currentStation

    val dataInitialized: Boolean
        get() = _dataInitialized

    val dataVersion: StateFlow<DataVersion?> = _currentVersion

    val lastCheckedVersion: DataLatestInfo?
        get() = _lastCheckedVersion


    suspend fun getDataVersion(): DataVersion? = withContext(Dispatchers.IO) {
        val version = dao.getCurrentDataVersion()
        _dataInitialized = version != null
        _currentVersion.value = version
        version
    }

    suspend fun getLatestDataVersion(forceRefresh: Boolean = true): DataLatestInfo =
        withContext(Dispatchers.IO) {
            val last = _lastCheckedVersion
            if (last != null && !forceRefresh) {
                last
            } else {
                val info = api.getLatestInfo()
                _lastCheckedVersion = info
                info
            }
        }

    suspend fun getDataVersionHistory() = dao.getDataVersionHistory()

    val dataUpdateProgress = updateUseCase.progress

    suspend fun updateData(info: DataLatestInfo) {
        updateUseCase(info).also {
            when (it) {
                is DataUpdateUseCase.Result.Success -> {
                    _dataInitialized = true
                    _currentVersion.value = it.version
                }
                is DataUpdateUseCase.Result.Failure -> {

                }
            }
        }
    }

}

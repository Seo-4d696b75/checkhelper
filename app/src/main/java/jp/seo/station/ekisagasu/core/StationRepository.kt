package jp.seo.station.ekisagasu.core

import android.location.Location
import android.os.Handler
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.search.KdTree
import jp.seo.station.ekisagasu.search.measureDistance
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_SIMPLE
import jp.seo.station.ekisagasu.utils.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
class StationRepository(
    private val dao: StationDao,
    private val api: APIClient,
    private val tree: KdTree,
    private val main: Handler
) {

    fun getStation(code: Int) = dao.getStation(code)

    fun getLine(code: Int) = dao.getLine(code)

    suspend fun getLines(codes: Array<Int>) = withContext(Dispatchers.IO) {
        dao.getLines(codes)
    }

    suspend fun getStations(codes: List<Int>) = withContext(Dispatchers.IO) {
        dao.getStations(codes)
    }

    private val _currentVersion = MutableLiveData<DataVersion?>(null)
    private var _dataInitialized: Boolean = false
    private var _lastCheckedVersion: DataLatestInfo? = null
    private var _lastCheckedLocation: Location? = null
    private var _lastSearchK: Int? = null
    private val _currentStation = MutableLiveData<NearStation?>(null)
    private val _nearestStation = MutableLiveData<NearStation?>(null)
    private val _selectedLine = MutableLiveData<Line?>(null)
    private val _nearestStations = MutableLiveData<List<NearStation>>(ArrayList())

    suspend fun searchNearestStations(
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

    private fun checkNeedUpdate(location: Location, k: Int): Boolean {
        val lastK = _lastSearchK
        _lastSearchK = k
        if (lastK != null && k != lastK) {
            return true
        }
        val last = _lastCheckedLocation
        if (last != null && last.longitude == location.longitude && last.latitude == location.latitude) return false
        _lastCheckedLocation = location
        return true
    }

    @MainThread
    suspend fun updateNearestStations(location: Location, k: Int) {
        if (k < 1) return
        if (!checkNeedUpdate(location, k)) return
        val result = searchNearestStations(location.latitude, location.longitude, k, 0.0)
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
        _nearestStations.value = ArrayList()
        _lastCheckedLocation = null
        _lastSearchK = null
    }

    /**
     * 現在位置から最近傍の駅と距離情報
     * 探索を開始し位置情報を更新された状態でのみ `not-Null`
     */
    val nearestStation: LiveData<NearStation?> = _nearestStation

    val selectedLine: LiveData<Line?> = _selectedLine

    /**
     * 現在位置からの近傍駅を近い順にソートしたリスト
     */
    val nearestStations: LiveData<List<NearStation>> = _nearestStations

    /**
     * 現在位置からの最近傍の駅
     * [nearestStation]とは異なり現在位置が変化しても更新されず、駅が変化したタイミングでのみ更新される
     * [NearStation]の距離・タイムスタンプは更新されたときの値のまま保持される
     */
    val detectedStation: LiveData<NearStation?> = _currentStation

    val dataInitialized: Boolean
        get() = _dataInitialized

    val dataVersion: LiveData<DataVersion?> = _currentVersion

    val lastCheckedVersion: DataLatestInfo?
        get() = _lastCheckedVersion


    suspend fun getDataVersion(): DataVersion? = withContext(Dispatchers.IO) {
        val version = dao.getCurrentDataVersion()
        _dataInitialized = version != null
        _currentVersion.postValue(version)
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

    interface UpdateProgressListener {
        companion object {
            const val STATE_DOWNLOAD = "download"
            const val STATE_PARSE = "parse"
            const val STATE_CLEAN = "clean"
            const val STATE_ADD = "add"
        }

        fun onStateChanged(state: String)
        fun onProgress(progress: Int)
        fun onComplete(success: Boolean)
    }

    suspend fun updateData(info: DataLatestInfo, listener: UpdateProgressListener) {
        main.post {
            listener.onStateChanged(UpdateProgressListener.STATE_DOWNLOAD)
            listener.onProgress(0)
        }
        var percent = 0
        val download = getDownloadClient { length: Long ->
            val p = floor(length.toFloat() / info.length * 100.0f).toInt()
            if (p in 1..100 && p > percent) {
                main.post { listener.onProgress(p) }
                percent = p
                if (percent == 100) main.post { listener.onStateChanged(UpdateProgressListener.STATE_PARSE) }
            }
        }
        val data = download.getData(info.url)
        var result = false
        if (data.version == info.version) {
            dao.updateData(data, listener, main)
            val current = getDataVersion()
            if (info.version == current?.version) {
                _dataInitialized = true
                _currentVersion.postValue(current)
                result = true
            }
        }
        main.post { listener.onComplete(result) }
    }

}

data class NearStation(
    val station: Station,
    /**
     * distance from the current position to this station
     */
    val distance: Double,
    /**
     * Time when this near station detected
     */
    val time: Date,

    val lines: List<Line>
) {

    fun getDetectedTime(): String {
        return formatTime(TIME_PATTERN_SIMPLE, time)
    }

    fun getLinesName(): String {
        return lines.joinToString(separator = " ", transform = { line -> line.name })
    }
}

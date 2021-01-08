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

    suspend fun getLines(codes: Array<Int>) = dao.getLines(codes)

    private var _dataInitialized: Boolean = false
    private var _lastCheckedVersion: DataLatestInfo? = null
    private var _lastCheckedLocation: Location? = null
    private val _nearestStation = MutableLiveData<NearStation?>(null)
    private val _selectedLine = MutableLiveData<Line?>(null)
    private val _nearestStations = MutableLiveData<List<NearStation>>(ArrayList())

    suspend fun searchNearestStations(
        lat: Double,
        lng: Double,
        k: Int,
        r: Double
    ): KdTree.SearchResult {
        if (!_dataInitialized) {
            throw IllegalStateException("data not initialized yet")
        }
        return tree.search(lat, lng, k, r, false)
    }

    suspend fun updateNearestStations(location: Location, k: Int): Station? {
        if (k < 1) return null
        val last = _lastCheckedLocation
        if (last != null && last.longitude == location.longitude && last.latitude == location.latitude) return null
        var detected: Station? = null
        withContext(Dispatchers.IO) {
            val result = searchNearestStations(location.latitude, location.longitude, k, 0.0)
            withContext(Dispatchers.Main) {
                val nearest = result.stations[0]
                val current = _nearestStation.value
                val time = Date(location.time)
                _nearestStations.value = result.stations.map { s ->
                    NearStation(s, measureDistance(s, location), time)
                }
                if (current == null || current.station != nearest) {
                    _nearestStation.value =
                        NearStation(nearest, measureDistance(nearest, location), time)
                    detected = nearest
                }
            }
            _lastCheckedLocation = location
        }
        return detected
    }

    @MainThread
    fun selectLine(line: Line) {
        _selectedLine.value = line
    }

    @MainThread
    fun onStopSearch() {
        _selectedLine.value = null
        _nearestStation.value = null
        _nearestStations.value = ArrayList()
    }

    val nearestStation: LiveData<NearStation?>
        get() = _nearestStation

    val selectedLine: LiveData<Line?>
        get() = _selectedLine

    val nearestStations: LiveData<List<NearStation>>
        get() = _nearestStations

    val dataInitialized: Boolean
        get() = _dataInitialized

    val lastCheckedVersion: DataLatestInfo?
        get() = _lastCheckedVersion

    suspend fun getDataVersion(): DataVersion? {
        val version = dao.getCurrentDataVersion()
        _dataInitialized = version != null
        return version
    }

    suspend fun getLatestDataVersion(forceRefresh: Boolean = true): DataLatestInfo {
        val last = _lastCheckedVersion
        if (last != null && !forceRefresh) return last
        val info = api.getLatestInfo()
        _lastCheckedVersion = info
        return info
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
    val time: Date
)

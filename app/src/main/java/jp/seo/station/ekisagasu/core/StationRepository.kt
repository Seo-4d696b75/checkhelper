package jp.seo.station.ekisagasu.core

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.search.KdTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
class StationRepository(
    private val dao: StationDao,
    private val api: APIClient,
    private val tree: KdTree,
) {

    fun getStation(code: Int) = dao.getStation(code)

    fun getLine(code: Int) = dao.getLine(code)

    fun getLines(codes: Array<Int>) = dao.getLines(codes)

    private var _dataInitialized: Boolean = false
    private var _lastCheckedVersion: DataLatestInfo? = null
    private val _nearestStation = MutableLiveData<Station?>(null)
    private val _selectedLine = MutableLiveData<Line?>(null)
    private val _nearestStations = MutableLiveData<List<Station>>(ArrayList())

    suspend fun searchNearestStations(lat: Double, lng:Double, k:Int, r: Double): KdTree.SearchResult{
        if ( !_dataInitialized ){
            throw IllegalStateException("data not initialized yet")
        }
        return tree.search(lat, lng, k, r, false)
    }

    suspend fun updateNearestStations(lat: Double, lng: Double, k: Int): Station?{
        if ( k < 1 ) return null
        var detected: Station? = null
        withContext(Dispatchers.IO){
            val result = searchNearestStations(lat, lng, k, 0.0)
            withContext(Dispatchers.Main){
                val nearest = result.stations[0]
                val current = _nearestStation.value
                _nearestStation.value = nearest
                _nearestStations.value = result.stations
                if ( current == null || current != nearest ) detected = nearest
            }
        }
        return detected
    }

    @MainThread
    fun selectLine(line: Line){
        _selectedLine.value = line
    }

    @MainThread
    fun onStopSearch(){
        _selectedLine.value = null
        _nearestStation.value = null
        _nearestStations.value = ArrayList()
    }

    val nearestStation: LiveData<Station?>
        get() = _nearestStation

    val selectedLine: LiveData<Line?>
        get() = _selectedLine

    val nearestStations: LiveData<List<Station>>
        get() = _nearestStations

    val dataInitialized: Boolean
        get() = _dataInitialized

    val lastCheckedVersion: DataLatestInfo?
        get() = _lastCheckedVersion

    suspend fun getDataVersion(): DataVersion?{
        val version = dao.getCurrentDataVersion()
        _dataInitialized = version != null
        return version
    }

    suspend fun getLatestDataVersion(): DataLatestInfo{
        val info = api.getLatestInfo()
        _lastCheckedVersion = info
        return info
    }

    suspend fun getDataVersionHistory() = dao.getDataVersionHistory()

    suspend fun updateData(version: Long?, url: String): Boolean{
        val data = api.getData(url)
        if ( version == null || data.version == version ){
            dao.updateData(data)
            val current = getDataVersion()
            if ( version == null || version == current?.version ){
                _dataInitialized = true
                return true
            }
        }
        return false
    }

}

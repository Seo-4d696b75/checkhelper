package jp.seo.station.ekisagasu.repository.impl

import android.location.Location
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.NearStation
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.repository.SearchRepository
import jp.seo.station.ekisagasu.search.NearestSearch
import jp.seo.station.ekisagasu.search.SearchResult
import jp.seo.station.ekisagasu.search.measureDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val search: NearestSearch,
) : SearchRepository {

    private val updateMutex = Mutex()
    private var searchK: Int = 12

    private var _lastCheckedLocation: Location? = null
    private var _lastSearchK: Int? = null
    private val _currentStation = MutableStateFlow<NearStation?>(null)
    private val _nearestStation = MutableStateFlow<NearStation?>(null)
    private val _selectedLine = MutableStateFlow<Line?>(null)
    private val _nearestStations = MutableStateFlow<List<NearStation>>(emptyList())

    override suspend fun setSearchK(value: Int) = updateMutex.withLock {
        if (value != searchK) {
            searchK = value
            _lastCheckedLocation?.let {
                updateLocation(it)
            }
        }
    }

    private suspend fun searchNearestStations(
        lat: Double,
        lng: Double,
        k: Int,
        r: Double
    ): SearchResult = withContext(Dispatchers.IO) {
        if (!dataRepository.dataInitialized) {
            throw IllegalStateException("data not initialized yet")
        }
        search.search(lat, lng, k, r, false)
    }

    override suspend fun updateNearestStations(location: Location) = updateMutex.withLock {
        if (searchK < 1) return
        val last = _lastCheckedLocation
        if (last != null && last.longitude == location.longitude && last.latitude == location.latitude) return
        _lastCheckedLocation = location
        updateLocation(location)
    }

    private suspend fun updateLocation(location: Location) {
        val result = searchNearestStations(location.latitude, location.longitude, searchK, 0.0)
        if (result.stations.isEmpty()) return

        val nearest = result.stations[0]
        val current = _currentStation.value
        val time = Date(location.time)
        val list = result.stations.map { s ->
            val lines = dataRepository.getLines(s.lines)
            NearStation(s, s.measureDistance(location), time, lines)
        }
        _nearestStations.value = list
        _nearestStation.value = list[0]
        if (current == null || current.station != nearest) {
            _currentStation.value = list[0]
        }
        _lastCheckedLocation = location
    }

    override fun selectLine(line: Line?) {
        _selectedLine.value = line
    }

    override fun onStopSearch() {
        _currentStation.value = null
        _selectedLine.value = null
        _nearestStation.value = null
        _nearestStations.value = emptyList()
        _lastCheckedLocation = null
        _lastSearchK = null
    }

    override val nearestStation: StateFlow<NearStation?> = _nearestStation

    override val selectedLine: StateFlow<Line?> = _selectedLine

    override val nearestStations: StateFlow<List<NearStation>> = _nearestStations

    override val detectedStation: StateFlow<NearStation?> = _currentStation
}

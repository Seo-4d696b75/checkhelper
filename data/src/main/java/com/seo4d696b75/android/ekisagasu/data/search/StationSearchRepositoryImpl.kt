package com.seo4d696b75.android.ekisagasu.data.search

import android.location.Location
import com.seo4d696b75.android.ekisagasu.data.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.data.kdtree.SearchResult
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import com.seo4d696b75.android.ekisagasu.data.log.LogCollector
import com.seo4d696b75.android.ekisagasu.data.log.LogMessage
import com.seo4d696b75.android.ekisagasu.data.station.DataRepository
import com.seo4d696b75.android.ekisagasu.data.station.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class StationSearchRepositoryImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val search: NearestSearch,
    private val logger: LogCollector,
) : StationSearchRepository,
    LogCollector by logger {
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
        r: Double,
    ): SearchResult = withContext(Dispatchers.IO) {
        if (!dataRepository.dataInitialized) {
            throw IllegalStateException("data not initialized yet")
        }
        search.search(lat, lng, k, r, false)
    }

    override suspend fun updateNearestStations(location: Location) = updateMutex.withLock {
        require(searchK > 0)
        val last = _lastCheckedLocation
        if (last != null && last.longitude == location.longitude && last.latitude == location.latitude) {
            return _nearestStation.value
        }
        _lastCheckedLocation = location
        updateLocation(location)
    }

    private suspend fun updateLocation(location: Location): NearStation? {
        val result = searchNearestStations(location.latitude, location.longitude, searchK, 0.0)
        if (result.stations.isEmpty()) return null

        val nearest = result.stations[0]
        val current = _currentStation.value
        val time = Date(location.time)
        val list = result.stations.map { s ->
            val lines = dataRepository.getLines(s.lines)
            NearStation(s, s.measureDistance(location), time, lines)
        }
        _nearestStations.update { list }
        _nearestStation.update { list[0] }
        if (current == null || current.station != nearest) {
            Timber.d("${nearest.name} (${nearest.code})")
            log(LogMessage.Station(nearest))
            _currentStation.update { list[0] }
        }
        _lastCheckedLocation = location
        return list[0]
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

    override val nearestStation = _nearestStation.asStateFlow()

    override val selectedLine = _selectedLine.asStateFlow()

    override val nearestStations = _nearestStations.asStateFlow()

    override val detectedStation = _currentStation.asStateFlow()
}

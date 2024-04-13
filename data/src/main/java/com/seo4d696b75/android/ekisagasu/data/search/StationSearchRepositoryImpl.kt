package com.seo4d696b75.android.ekisagasu.data.search

import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import com.seo4d696b75.android.ekisagasu.domain.coroutine.mapLatestBySkip
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class StationSearchRepositoryImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val locationRepository: LocationRepository,
    private val search: NearestSearch,
    private val logger: LogCollector,
    @ExternalScope private val scope: CoroutineScope,
) : StationSearchRepository,
    LogCollector by logger {

    private var searchK = MutableStateFlow(12)
    private val _selectedLine = MutableStateFlow<Line?>(null)

    override val selectedLine = _selectedLine.asStateFlow()
    override fun setSearchK(value: Int) {
        searchK.update { value }
    }

    override fun selectLine(line: Line?) {
        _selectedLine.value = line
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val result: Flow<StationSearchResult?> = locationRepository
        .isRunning
        .flatMapLatest { running ->
            if (running) {
                combine(
                    locationRepository.currentLocation.filterNotNull(),
                    searchK,
                ) { location, k ->
                    SearchParam(location, k)
                }
                    .distinctUntilChanged(::isSearchSkippable)
                    .mapLatestBySkip(null, ::updateLocation)
            } else {
                _selectedLine.update { null }
                flowOf(null)
            }
        }.stateIn(
            // convert into hot flow so that same result should be shared in application
            scope,
            SharingStarted.WhileSubscribed(),
            null,
        )

    private data class SearchParam(
        val location: Location,
        val k: Int,
    )

    private fun isSearchSkippable(old: SearchParam, new: SearchParam): Boolean {
        return old.location.lat == new.location.lat
            && old.location.lng == old.location.lng
            && old.k == new.k
    }

    private suspend fun updateLocation(
        param: SearchParam,
        previous: StationSearchResult?,
    ): StationSearchResult? {
        // Must be pure function
        // do not access to class member!!
        require(param.k > 0)
        require(dataRepository.dataInitialized)
        val result = search.search(param.location.lat, param.location.lng, param.k, 0.0, false)
        if (result.stations.isEmpty()) {
            return null
        }
        val nearest = result.stations[0]
        val time = Date(param.location.timestamp)
        val list = result.stations.map { s ->
            val lines = dataRepository.getLines(s.lines)
            NearStation(
                station = s,
                distance = s.measureDistance(param.location.lat, param.location.lng),
                time = time,
                lines = lines,
            )
        }
        return if (previous == null || previous.detected.station != nearest) {
            Timber.d("${nearest.name} (${nearest.code})")
            log(LogMessage.Station(nearest))
            StationSearchResult(
                location = param.location,
                searchK = param.k,
                detected = list[0],
                nears = list,
            )
        } else {
            previous.copy(
                location = param.location,
                searchK = param.k,
                nears = list,
            )
        }
    }
}

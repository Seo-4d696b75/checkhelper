package com.seo4d696b75.android.ekisagasu.data.search

import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import com.seo4d696b75.android.ekisagasu.domain.coroutine.mapLatestBySkip
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationState
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchResult
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

class StationSearchRepositoryImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val locationRepository: LocationRepository,
    settingRepository: UserSettingRepository,
    private val search: NearestSearch,
    private val logger: LogCollector,
    @ExternalScope private val scope: CoroutineScope,
) : StationSearchRepository,
    LogCollector by logger {

    private val _selectedLine = MutableStateFlow<Line?>(null)

    override val selectedLine = _selectedLine.asStateFlow()

    override suspend fun selectLine(line: Line) {
        if (locationRepository.currentLocation.first() is LocationState.Running) {
            _selectedLine.update { line }
        }
    }

    override fun clearLine() {
        _selectedLine.update { null }
    }

    override val result = combine(
        locationRepository.currentLocation,
        settingRepository.setting.map { it.searchK },
    ) { state, k ->
        when (state) {
            LocationState.Idle -> {
                _selectedLine.update { null }
                null
            }

            is LocationState.Running -> state.location?.let {
                SearchParam(it, k)
            }
        }
    }
        .distinctUntilChanged(::isSearchSkippable)
        .mapLatestBySkip(null, ::updateLocation)
        .stateIn(
            // convert into hot flow so that same result should be shared in application
            scope,
            SharingStarted.WhileSubscribed(),
            null,
        )

    private data class SearchParam(
        val location: Location,
        val k: Int,
    )

    private fun isSearchSkippable(old: SearchParam?, new: SearchParam?): Boolean {
        if (old == null && new == null) {
            return true
        }
        if (old == null || new == null) {
            return false
        }
        return old.location.lat == new.location.lat
            && old.location.lng == old.location.lng
            && old.k == new.k
    }

    private suspend fun updateLocation(
        param: SearchParam?,
        previous: StationSearchResult?,
    ): StationSearchResult? {
        if (param == null) {
            return null
        }
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

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface StationSearchRepositoryModule {
    @Singleton
    @Binds
    fun bind(impl: StationSearchRepositoryImpl): StationSearchRepository
}

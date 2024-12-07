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
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
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

    private val location = locationRepository
        .currentLocation
        .distinctUntilChanged { old, new ->
            old is LocationState.Result &&
                new is LocationState.Result &&
                old.location.lat == new.location.lat &&
                old.location.lng == new.location.lng
        }

    private val k = settingRepository
        .setting
        .map { it.searchK }
        .distinctUntilChanged()

    override val state = combine(
        location,
        k,
    ) { state, k ->
        if (state !is LocationState.Result) {
            _selectedLine.update { null }
        }
        state to k
    }
        .mapLatestBySkip<Pair<LocationState, Int>, StationSearchState>(
            StationSearchState.Idle(12),
        ) { param, previous ->
            val (state, k) = param
            when (state) {
                LocationState.Idle -> StationSearchState.Idle(k)
                is LocationState.Initializing -> StationSearchState.Initializing(k)
                is LocationState.Result -> updateLocation(state.location, k, previous)
            }
        }
        .stateIn(
            // convert into hot flow so that same result should be shared in application
            scope,
            SharingStarted.WhileSubscribed(),
            StationSearchState.Idle(12),
        )

    private suspend fun updateLocation(
        location: Location,
        k: Int,
        previous: StationSearchState,
    ): StationSearchState {
        // Must be pure function
        // do not access to class member!!
        require(k > 0)
        require(dataRepository.dataInitialized)
        val result = search.search(location.lat, location.lng, k, 0.0, false)
        if (result.stations.isEmpty()) {
            return StationSearchState.Initializing(k)
        }
        val nearest = result.stations[0]
        val time = Date(location.timestamp)
        val list = result.stations.map { s ->
            NearStation(
                station = s,
                distance = s.measureDistance(location.lat, location.lng),
                time = time,
            )
        }
        return if (previous !is StationSearchState.Result || previous.detected.station != nearest) {
            Timber.d("${nearest.name} (${nearest.code})")
            log(LogMessage.Station(nearest))
            StationSearchState.Result(
                location = location,
                searchK = k,
                detected = list[0],
                nears = list,
            )
        } else {
            previous.copy(
                location = location,
                searchK = k,
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

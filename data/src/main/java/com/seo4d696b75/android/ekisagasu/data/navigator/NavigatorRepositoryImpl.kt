package com.seo4d696b75.android.ekisagasu.data.navigator

import com.seo4d696b75.android.ekisagasu.data.polyline.PolylineNavigator
import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import com.seo4d696b75.android.ekisagasu.domain.coroutine.mapLatestBySkip
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorPrediction
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

class NavigatorRepositoryImpl @Inject constructor(
    private val search: NearestSearch,
    private val searchRepository: StationSearchRepository,
    @ExternalScope private val scope: CoroutineScope,
) : NavigatorRepository {

    private val navigator = MutableStateFlow<PolylineNavigator?>(null)

    override val line = navigator.map { it?.line }

    override val isRunning = navigator.map { it != null }
    override val currentLine: Line?
        get() = navigator.value?.line

    override fun setLine(line: Line?) {
        navigator.update {
            it?.release()
            if (line == null) {
                null
            } else {
                PolylineNavigator(search, line)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state = searchRepository
        .result
        .map { it != null }
        .distinctUntilChanged()
        .flatMapLatest { running ->
            if (running) {
                _state
            } else {
                navigator.update {
                    it?.release()
                    null
                }
                flowOf(null)
            }
        }.stateIn(
            // convert to hot flow so that same result should be shared in application
            scope,
            SharingStarted.WhileSubscribed(),
            null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _state: Flow<NavigatorState?> =
        navigator.flatMapLatest { navigator ->
            if (navigator == null) {
                flowOf(null)
            } else {
                searchRepository
                    .result
                    .mapLatestBySkip {
                        val result = if (it == null) {
                            null
                        } else {
                            navigator.onLocationUpdate(it.location, it.detected.station)
                            navigator.result
                        }
                        if (result == null) {
                            NavigatorState.Initializing(
                                line = navigator.line,
                            )
                        } else {
                            NavigatorState.Result(
                                line = navigator.line,
                                current = result.current,
                                predictions = (0 until result.size).map { idx ->
                                    NavigatorPrediction(
                                        station = result.getStation(idx),
                                        distance = result.getDistance(idx),
                                    )
                                },
                            )
                        }
                    }
            }
        }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface NavigationRepositoryModule {
    @Singleton
    @Binds
    fun bindNavigation(impl: NavigatorRepositoryImpl): NavigatorRepository
}

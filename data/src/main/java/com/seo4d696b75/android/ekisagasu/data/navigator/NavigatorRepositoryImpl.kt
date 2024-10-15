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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
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
    override val currentLine: Line?
        get() = navigator.value?.line

    override fun start(line: Line) {
        navigator.update {
            it?.release()
            PolylineNavigator(search, line)
        }
    }

    override fun stop() {
        navigator.update {
            it?.release()
            null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state = navigator.flatMapLatest { navigator ->
        if (navigator == null) {
            flowOf(NavigatorState.Idle)
        } else {
            searchRepository
                .result
                .onEach {
                    if (it == null) {
                        // 探索が終了したらnavigatorも終了する
                        stop()
                    }
                }
                .filterNotNull()
                .mapLatestBySkip {
                    val result = navigator.run {
                        onLocationUpdate(it.location, it.detected.station)
                        result
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
    }.stateIn(
        // convert to hot flow so that same result should be shared in application
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = NavigatorState.Idle,
    )
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface NavigationRepositoryModule {
    @Singleton
    @Binds
    fun bindNavigation(impl: NavigatorRepositoryImpl): NavigatorRepository
}

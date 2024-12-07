package com.seo4d696b75.android.ekisagasu.data.navigator

import com.seo4d696b75.android.ekisagasu.data.polyline.PolylineNavigator
import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import com.seo4d696b75.android.ekisagasu.domain.coroutine.mapLatestBySkip
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
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
                .state
                .onEach {
                    if (it is StationSearchState.Idle) {
                        // 探索が終了したらnavigatorも終了する
                        stop()
                    }
                }
                .filterIsInstance<StationSearchState.Result>()
                .mapLatestBySkip {
                    navigator.onLocationUpdate(it.location, it.detected.station)
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

package com.seo4d696b75.android.ekisagasu.data.navigator

import android.location.Location
import com.seo4d696b75.android.ekisagasu.data.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.data.polyline.PolylineNavigator
import com.seo4d696b75.android.ekisagasu.data.station.Line
import com.seo4d696b75.android.ekisagasu.data.station.Station
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class NavigatorRepositoryImpl @Inject constructor(
    private val search: NearestSearch,
) : NavigatorRepository {
    private val _running = MutableStateFlow(false)
    private var navigator: PolylineNavigator? = null
    private val _navigator = MutableStateFlow<PolylineNavigator?>(null)

    override val running: StateFlow<Boolean> = _running

    @OptIn(ExperimentalCoroutinesApi::class)
    override val predictions = _navigator.flatMapLatest { n ->
        n?.results ?: flowOf(null)
    }

    override val line: Line?
        get() = navigator?.line

    override fun start(line: Line) {
        navigator?.release()
        navigator = PolylineNavigator(search, line)
        _navigator.value = navigator
        _running.value = true
    }

    override fun stop() {
        navigator?.release()
        navigator = null
        _navigator.value = null
        _running.value = false
    }

    override suspend fun updateLocation(
        location: Location,
        station: Station,
    ) {
        navigator?.onLocationUpdate(location, station)
    }
}
